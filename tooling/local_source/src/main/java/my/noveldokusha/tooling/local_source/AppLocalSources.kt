package my.noveldokusha.tooling.local_source

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import my.noveldoksuha.coreui.theme.Grey25
import my.noveldoksuha.coreui.theme.textPadding
import my.noveldokusha.core.AppFileResolver
import my.noveldokusha.core.PagedList
import my.noveldokusha.core.Response
import my.noveldokusha.core.asSequence
import my.noveldokusha.core.fileImporter
import my.noveldokusha.core.getOrNull
import my.noveldokusha.epub_tooling.epubCoverParser
import my.noveldokusha.network.tryConnect
import my.noveldokusha.scraper.R
import my.noveldokusha.scraper.domain.BookResult
import my.noveldokusha.scraper.domain.ChapterResult
import my.noveldokusha.scraper.sources.LocalSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLocalSources @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val localSourcesDirectories: LocalSourcesDirectories,
    private val appFileResolver: AppFileResolver,
) : LocalSource {

    override val id = "local_source"
    override val nameStrId = R.string.source_name_local
    override val baseUrl = "local://"
    override val catalogUrl = "local://"
    override val language = null
    override val iconUrl = Icons.Filled.Folder

    override suspend fun getChapterList(bookUrl: String): Response<List<ChapterResult>> {
        // This should always fail as is local
        return Response.Error(
            "LocalSource doesn't have remote API",
            UnsupportedOperationException()
        )
    }

    private val validMIMES = setOf(
        "application/epub+zip",
        DocumentsContract.Document.MIME_TYPE_DIR
    )

    private fun Uri.cursorRecursiveGetAllFiles(): Sequence<BookResult> {
        val rootURI = this
        return appContext.contentResolver.query(
            rootURI,
            arrayOf(
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
            ),
            null,
            null,
            null,
        ).asSequence().flatMap {
            val mime = it.getString(2)
            // Query selector doesn't work for mime_type
            if (mime !in validMIMES) return@flatMap emptySequence()

            val id = it.getString(1)
            when (mime) {
                DocumentsContract.Document.MIME_TYPE_DIR -> {
                    val fileURI = DocumentsContract.buildChildDocumentsUriUsingTree(rootURI, id)
                    fileURI.cursorRecursiveGetAllFiles()
                }
                else -> {
                    val fileName = it.getString(0)
                    val fileURI = DocumentsContract.buildDocumentUriUsingTree(rootURI, id)
                    sequenceOf(
                        BookResult(
                            title = fileName,
                            url = fileURI.toString(),
                        )
                    )
                }
            }
        }
    }

    private fun Uri.cursorRecursiveSearchAllFilesWithName(text: String): Sequence<BookResult> {
        val rootURI = this
        return appContext.contentResolver.query(
            rootURI,
            arrayOf(
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
            ),
            null,
            null,
            null,
        ).asSequence().flatMap {
            val mime = it.getString(2)
            // Query selector doesn't work for mime_type
            if (mime !in validMIMES) return@flatMap emptySequence()

            val id = it.getString(1)
            when (mime) {
                DocumentsContract.Document.MIME_TYPE_DIR -> {
                    val fileURI = DocumentsContract.buildChildDocumentsUriUsingTree(rootURI, id)
                    fileURI.cursorRecursiveSearchAllFilesWithName(text)
                }
                else -> {
                    val fileName = it.getString(0)
                    if (fileName.contains(text, ignoreCase = true)) {
                        val fileURI = DocumentsContract.buildDocumentUriUsingTree(rootURI, id)
                        sequenceOf(
                            BookResult(
                                title = fileName,
                                url = fileURI.toString(),
                            )
                        )
                    } else {
                        sequenceOf()
                    }
                }
            }
        }
    }

    override suspend fun getCatalogList(
        index: Int
    ): Response<PagedList<BookResult>> = withContext(Dispatchers.IO) {
        tryConnect {
            val files = localSourcesDirectories
                .list
                .asSequence()
                .flatMap {
                    DocumentsContract.buildChildDocumentsUriUsingTree(
                        it, DocumentsContract.getTreeDocumentId(it)
                    ).cursorRecursiveGetAllFiles()
                }
                .map { async { tryConnect { addCover(it) }.getOrNull() } }
                .toList()
                .awaitAll()
                .filterNotNull()

            PagedList(
                list = files,
                index = 0,
                isLastPage = true
            )
        }
    }

    private suspend fun addCover(
        bookResult: BookResult
    ): BookResult = withContext(Dispatchers.IO) {
        val coverFile = appFileResolver.getStorageBookCoverImageFile(bookResult.title)
        if (!coverFile.exists()) {
            val inputStream = appContext.contentResolver.openInputStream(bookResult.url.toUri())
                ?: return@withContext bookResult
            val coverImage = inputStream.use {
                epubCoverParser(
                    inputStream = inputStream
                )
            }
                ?: return@withContext bookResult
            fileImporter(
                targetFile = coverFile,
                imageData = coverImage.image,
            )
        }
        bookResult.copy(
            coverImageUrl = coverFile.canonicalFile.absolutePath
        )
    }

    override suspend fun getCatalogSearch(
        index: Int,
        input: String
    ): Response<PagedList<BookResult>> = withContext(Dispatchers.IO) {
        if (index > 0) {
            return@withContext Response.Success(PagedList.createEmpty(index))
        }
        tryConnect {
            val files = localSourcesDirectories
                .list
                .asSequence()
                .flatMap {
                    DocumentsContract.buildChildDocumentsUriUsingTree(
                        it, DocumentsContract.getTreeDocumentId(it)
                    ).cursorRecursiveSearchAllFilesWithName(input)
                }
                .map { async { tryConnect { addCover(it) }.getOrNull() } }
                .toList()
                .awaitAll()
                .filterNotNull()

            PagedList(
                list = files,
                index = 0,
                isLastPage = true
            )
        }
    }

    @Composable
    override fun ScreenConfig() {
        val context by rememberUpdatedState(LocalContext.current)
        Column(Modifier.fillMaxWidth()) {
            FilledTonalButton(
                onClick = onDoAddLocalSourceDirectory(
                    onResult = { localSourcesDirectories.add(it) }
                ),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.CreateNewFolder, null, tint = Grey25)
                    Text(text = stringResource(R.string.add_local_directory))
                }
            }
            val list by localSourcesDirectories.listState.collectAsState()
            if (list.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_directories_added_please_add_them_to_see_them_in_the_source_catalog_list),
                    Modifier.textPadding()
                )
            }
            for (it in list) {
                val item = remember(it.toString()) { DocumentFile.fromTreeUri(context, it) }
                ListItem(
                    headlineContent = {
                        Text(text = item?.name ?: "** Access denied **")
                    },
                    leadingContent = {
                        Icon(Icons.Filled.Folder, null)
                    },
                    trailingContent = {
                        IconButton(onClick = { localSourcesDirectories.remove(it) }) {
                            Icon(Icons.Filled.Delete, stringResource(id = R.string.delete))
                        }
                    }
                )
            }
        }
    }
}


