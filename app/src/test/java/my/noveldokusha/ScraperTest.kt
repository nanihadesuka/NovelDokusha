package my.noveldokusha

import my.noveldokusha.core.appPreferences.AppPreferences
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.scraper.Scraper
import my.noveldokusha.tooling.local_source.AppLocalSources
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class ScraperTest {

    private val networkClient: NetworkClient = mock()

    /**
     * AppPreferences is mocked. The Scraper constructor reads
     * `appPreferences.GEMINI_API_KEY.value` and `appPreferences.GEMINI_MODEL.value`
     * to build the TimoTxtGemini source, so we stub those two properties to
     * return empty strings — otherwise the mock returns null and the `.value`
     * access throws an NPE during Scraper construction.
     *
     * `Preference` is an abstract inner class of AppPreferences; we mock it
     * with Mockito's inline mock maker (default since Mockito 5) which can
     * produce a concrete proxy of the abstract inner class without needing a
     * real outer AppPreferences instance.
     */
    private val stringPref = mock<AppPreferences.Preference<String>>().also {
        whenever(it.value).thenReturn("")
    }

    private val appPreferences: AppPreferences = mock<AppPreferences>().also {
        whenever(it.GEMINI_API_KEY).thenReturn(stringPref)
        whenever(it.GEMINI_MODEL).thenReturn(stringPref)
    }

    private val sut = Scraper(
        networkClient = networkClient,
        localSource = AppLocalSources(mock(), mock(), mock()),
        appPreferences = appPreferences,
    )

    @Before
    fun setup() {
    }

    // DATABASES TEST

    @Test
    fun `databaseList items are compatible`() {
        for (database in sut.databasesList)
            assertNotNull(sut.getCompatibleDatabase(database.baseUrl))
    }

    @Test
    fun `databaseList items baseUrl ends with slash`() {
        for (database in sut.databasesList)
            assertTrue(
                "${database::class.simpleName} baseUrl missing ending slash",
                database.baseUrl.endsWith("/")
            )
    }

    @Test
    fun `databaseList items have unique id`() {
        val groups = sut.databasesList.groupBy { it.id }
        for (list in groups)
            assertEquals(
                "${list.value.joinToString { it::class.simpleName.toString() }}: id can't be the same value for multiple databases",
                1,
                list.value.size
            )
    }

    // SOURCES TEST

    @Test
    fun `sourceList items are compatible`() {
        for (source in sut.sourcesList)
            assertNotNull(sut.getCompatibleSource(source.baseUrl))
    }

    @Test
    fun `sourceList items baseUrl ends with slash`() {
        for (source in sut.sourcesList)
            assertTrue(
                "${source::class.simpleName} baseUrl missing ending slash",
                source.baseUrl.endsWith("/")
            )
    }

    @Test
    fun `sourceList items have unique id`() {
        val groups = sut.sourcesList.groupBy { it.id }
        for (list in groups)
            assertEquals(
                "${list.value.joinToString { it::class.simpleName.toString() }}: name can't be the same value for multiple sources",
                1,
                list.value.size
            )
    }

}
