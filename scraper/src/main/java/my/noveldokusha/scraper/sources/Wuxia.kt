package my.noveldokusha.scraper.sources

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.noveldokusha.core.LanguageCode
import my.noveldokusha.core.PagedList
import my.noveldokusha.core.Response
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.network.toDocument
import my.noveldokusha.network.tryConnect
import my.noveldokusha.scraper.R
import my.noveldokusha.scraper.SourceInterface
import my.noveldokusha.scraper.domain.BookResult
import my.noveldokusha.scraper.domain.ChapterResult
import org.jsoup.nodes.Document
import java.net.URI

/**
 * Wuxia — https://wuxia.click
 *
 * A Next.js-based Wuxia/light novel reader. The site renders pages via
 * server-side rendering with React Query data dehydration. All novel and
 * chapter data is available in a <script id="__NEXT_DATA__"> JSON blob in the
 * initial HTML — no JavaScript execution needed by the scraper.
 *
 * NOTE: This replaces the old wuxia.blog source (dead since 2025). The source
 * ID "wuxia" is reused so the source slot is preserved in the app UI.
 *
 * Site structure (verified 2026-06-30 by fetching live HTML):
 *
 *  Catalog (Browse):
 *    https://wuxia.click/                  (home page — lists featured/popular)
 *    Novel links are <a href="/novel/<slug>"> with title text and cover img.
 *    No pagination — single page of featured novels.
 *
 *  Search:
 *    https://wuxia.click/search?q=<query>
 *    The search page also contains __NEXT_DATA__ with results.
 *
 *  Novel info page:
 *    https://wuxia.click/novel/<slug>
 *    - __NEXT_DATA__ queryKey "/api/novels/<slug>/" contains:
 *      name, image (cover URL), author.name, description, categories (genres),
 *      tags, chapters (count), slug, status, first_chapter.
 *
 *  Chapter reader:
 *    https://wuxia.click/chapter/<novel-slug>-<N>
 *    - __NEXT_DATA__ queryKey "/api/getchapter/<chapter-slug>/" contains:
 *      title (e.g. "CH 1"), text (full chapter text), nextChap (slug or null),
 *      prevChap (slug or null).
 *    - The chapter text is plain text with line breaks (not HTML <p> tags).
 *
 *  Chapter list generation:
 *    The info page API returns `chapters` (count) but NOT the chapter list.
 *    Chapter URLs follow the pattern: /chapter/<novel-slug>-<N>
 *    (e.g. /chapter/alchemy-emperor-of-the-divine-dao-1).
 *    We generate the list from 1 to chapters count.
 */
class Wuxia(
    private val networkClient: NetworkClient
) : SourceInterface.Catalog {
    override val id = "wuxia"
    override val nameStrId = R.string.source_name_wuxia
    override val baseUrl = "https://wuxia.click/"
    override val catalogUrl = "https://wuxia.click/"
    override val language = LanguageCode.ENGLISH
    override val iconUrl = "https://wuxia.click/favicon.ico"

    /**
     * Extract the __NEXT_DATA__ JSON from a page and return it as a
     * kotlinx JSONObject-like map. Uses org.json.JSONObject for simplicity.
     */
    private fun extractNextData(doc: Document): org.json.JSONObject? {
        val script = doc.selectFirst("script#__NEXT_DATA__") ?: return null
        val jsonText = script.data().ifBlank { return null }
        return try {
            org.json.JSONObject(jsonText)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Find a React Query data object by its queryKey prefix.
     * dehydratedState.queries is an array of {queryKey, state: {data}}.
     */
    private fun findQueryData(nextData: org.json.JSONObject, keyPrefix: String): org.json.JSONObject? {
        val queries = nextData
            .optJSONObject("props")
            ?.optJSONObject("pageProps")
            ?.optJSONObject("dehydratedState")
            ?.optJSONArray("queries")
            ?: return null

        for (i in 0 until queries.length()) {
            val query = queries.optJSONObject(i) ?: continue
            val queryKey = query.optJSONArray("queryKey") ?: continue
            if (queryKey.length() > 0 && queryKey.optString(0).startsWith(keyPrefix)) {
                return query
                    .optJSONObject("state")
                    ?.optJSONObject("data")
            }
        }
        return null
    }

    override suspend fun getChapterTitle(doc: Document): String? =
        withContext(Dispatchers.Default) {
            val nextData = extractNextData(doc) ?: return@withContext null
            val chapterData = findQueryData(nextData, "/api/getchapter/")
                ?: return@withContext null
            chapterData.optString("title").ifBlank { null }
        }

    override suspend fun getChapterText(doc: Document): String? =
        withContext(Dispatchers.Default) {
            val nextData = extractNextData(doc) ?: return@withContext null
            val chapterData = findQueryData(nextData, "/api/getchapter/")
                ?: return@withContext null
            // The "text" field contains the full chapter text with line breaks
            chapterData.optString("text").ifBlank { null }
        }

    override suspend fun getBookCoverImageUrl(
        bookUrl: String
    ): Response<String?> = withContext(Dispatchers.Default) {
        tryConnect {
            val doc = networkClient.get(bookUrl).toDocument()
            val nextData = extractNextData(doc) ?: return@tryConnect null
            val novelData = findQueryData(nextData, "/api/novels/")
                ?: return@tryConnect null
            novelData.optString("image").ifBlank { null }
        }
    }

    override suspend fun getBookDescription(
        bookUrl: String
    ): Response<String?> = withContext(Dispatchers.Default) {
        tryConnect {
            val doc = networkClient.get(bookUrl).toDocument()
            val nextData = extractNextData(doc) ?: return@tryConnect null
            val novelData = findQueryData(nextData, "/api/novels/")
                ?: return@tryConnect null
            novelData.optString("description").ifBlank { null }
        }
    }

    /**
     * Generate the chapter list from the novel's chapter count.
     * Chapter URLs follow the pattern: /chapter/<novel-slug>-<N>
     */
    override suspend fun getChapterList(
        bookUrl: String
    ): Response<List<ChapterResult>> = withContext(Dispatchers.Default) {
        tryConnect {
            val doc = networkClient.get(bookUrl).toDocument()
            val nextData = extractNextData(doc) ?: return@tryConnect emptyList()
            val novelData = findQueryData(nextData, "/api/novels/")
                ?: return@tryConnect emptyList()

            val slug = novelData.optString("slug").ifBlank {
                // Fallback: extract slug from the book URL
                bookUrl.trimEnd('/').substringAfterLast('/')
            }
            val chapterCount = novelData.optInt("chapters", 0)
            if (chapterCount <= 0) return@tryConnect emptyList()

            // Generate chapter list: CH 1, CH 2, ... CH N
            (1..chapterCount).map { n ->
                ChapterResult(
                    title = "CH $n",
                    url = "${baseUrl.trimEnd('/')}/chapter/$slug-$n"
                )
            }
        }
    }

    override suspend fun getCatalogList(
        index: Int
    ): Response<PagedList<BookResult>> = withContext(Dispatchers.Default) {
        tryConnect("index=$index") {
            if (index > 0) {
                return@tryConnect PagedList.createEmpty(index = index)
            }

            val doc = networkClient.get(catalogUrl).toDocument()
            // Novel links on the home page: a[href*="/novel/"]
            val books = doc.select("a[href*='/novel/']")
                .mapNotNull { a ->
                    val href = a.attr("href").ifBlank { return@mapNotNull null }
                    val title = a.text().trim().ifBlank {
                        a.selectFirst("h2, h3, h4, h5, .title")?.text()
                    } ?: return@mapNotNull null
                    if (title.length < 2) return@mapNotNull null

                    val cover = a.selectFirst("img[src]")?.attr("src")?.let { c ->
                        when {
                            c.startsWith("http") -> c
                            c.startsWith("/") -> URI(baseUrl).resolve(c).toString()
                            else -> ""
                        }
                    } ?: ""

                    BookResult(
                        title = title,
                        url = URI(baseUrl).resolve(href).toString(),
                        coverImageUrl = cover
                    )
                }
                .distinctBy { it.url }

            PagedList(
                list = books,
                index = index,
                isLastPage = true
            )
        }
    }

    override suspend fun getCatalogSearch(
        index: Int,
        input: String
    ): Response<PagedList<BookResult>> = withContext(Dispatchers.Default) {
        tryConnect {
            if (input.isBlank() || index > 0) {
                return@tryConnect PagedList.createEmpty(index = index)
            }

            // Try the search page — it should also have __NEXT_DATA__ with results
            val searchUrl = "${baseUrl.trimEnd('/')}/search?q=${java.net.URLEncoder.encode(input, "UTF-8")}"
            val doc = networkClient.get(searchUrl).toDocument()

            // Try parsing __NEXT_DATA__ first (for API-driven results)
            val nextData = extractNextData(doc)
            if (nextData != null) {
                val queries = nextData
                    .optJSONObject("props")
                    ?.optJSONObject("pageProps")
                    ?.optJSONObject("dehydratedState")
                    ?.optJSONArray("queries")

                if (queries != null) {
                    val books = mutableListOf<BookResult>()
                    for (i in 0 until queries.length()) {
                        val query = queries.optJSONObject(i) ?: continue
                        val queryKey = query.optJSONArray("queryKey") ?: continue
                        val keyStr = queryKey.optString(0, "")
                        if (keyStr.contains("search", ignoreCase = true)) {
                            val data = query.optJSONObject("state")?.optJSONObject("data")
                            // Search results might be a list or paginated object
                            val results = data?.optJSONArray("results") ?: data?.optJSONArray("data")
                            if (results != null) {
                                for (j in 0 until results.length()) {
                                    val novel = results.optJSONObject(j) ?: continue
                                    val slug = novel.optString("slug")
                                    if (slug.isBlank()) continue
                                    val name = novel.optString("name")
                                    if (name.isBlank()) continue
                                    val image = novel.optString("image")
                                    books.add(BookResult(
                                        title = name,
                                        url = "${baseUrl.trimEnd('/')}/novel/$slug",
                                        coverImageUrl = image
                                    ))
                                }
                            }
                        }
                    }
                    if (books.isNotEmpty()) {
                        return@tryConnect PagedList(
                            list = books.distinctBy { it.url },
                            index = index,
                            isLastPage = true
                        )
                    }
                }
            }

            // Fallback: parse HTML links
            val books = doc.select("a[href*='/novel/']")
                .mapNotNull { a ->
                    val href = a.attr("href").ifBlank { return@mapNotNull null }
                    val title = a.text().trim().ifBlank { return@mapNotNull null }
                    if (title.length < 2) return@mapNotNull null

                    val cover = a.selectFirst("img[src]")?.attr("src")?.let { c ->
                        when {
                            c.startsWith("http") -> c
                            c.startsWith("/") -> URI(baseUrl).resolve(c).toString()
                            else -> ""
                        }
                    } ?: ""

                    BookResult(
                        title = title,
                        url = URI(baseUrl).resolve(href).toString(),
                        coverImageUrl = cover
                    )
                }
                .distinctBy { it.url }

            PagedList(
                list = books,
                index = index,
                isLastPage = true
            )
        }
    }
}
