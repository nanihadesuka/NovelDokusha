package my.noveldokusha.scraper.sources

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
import my.noveldokusha.R
import my.noveldokusha.composableActions.onDoAddLocalSourceDirectory
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.ChapterMetadata
import my.noveldokusha.data.Response
import my.noveldokusha.data.getOrNull
import my.noveldokusha.network.PagedList
import my.noveldokusha.network.tryConnect
import my.noveldokusha.repository.AppFileResolver
import my.noveldokusha.scraper.LocalSourcesDirectories
import my.noveldokusha.scraper.SourceInterface
import my.noveldokusha.tools.epub.epubCoverParser
import my.noveldokusha.tools.epub.epubImageImporter
import my.noveldokusha.ui.theme.Grey25
import my.noveldokusha.utils.asSequence
import my.noveldokusha.utils.textPadding

class LocalSource(
    @ApplicationContext private val appContext: Context,
    private val localSourcesDirectories: LocalSourcesDirectories,
    private val appFileResolver: AppFileResolver,
) : SourceInterface.Catalog, SourceInterface.Configurable {
    override val id = "local_source"
    override val nameStrId = R.string.source_name_local
    override val baseUrl = "local://"
    override val catalogUrl = "local://"
    override val language = null
    override val iconUrl = Icons.Filled.Folder

    override suspend fun getChapterList(bookUrl: String): Response<List<ChapterMetadata>> {
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

    private fun Uri.cursorRecursiveGetAllFiles(): Sequence<BookMetadata> {
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
                        BookMetadata(
                            title = fileName,
                            url = fileURI.toString(),
                        )
                    )
                }
            }
        }
    }

    private fun Uri.cursorRecursiveSearchAllFilesWithName(text: String): Sequence<BookMetadata> {
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
                            BookMetadata(
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
    ): Response<PagedList<BookMetadata>> = withContext(Dispatchers.IO) {
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
        bookMetadata: BookMetadata
    ): BookMetadata = withContext(Dispatchers.IO) {
        val coverFile = appFileResolver.getStorageBookCoverImageFile(bookMetadata.title)
        if (!coverFile.exists()) {
            val inputStream = appContext.contentResolver.openInputStream(bookMetadata.url.toUri())
                ?: return@withContext bookMetadata
            val coverImage = inputStream.use { epubCoverParser(inputStream = inputStream) }
                ?: return@withContext bookMetadata
            epubImageImporter(
                targetFile = coverFile,
                imageData = coverImage.image,
            )
        }
        bookMetadata.copy(
            coverImageUrl = coverFile.canonicalFile.absolutePath
        )
    }

    override suspend fun getCatalogSearch(
        index: Int,
        input: String
    ): Response<PagedList<BookMetadata>> = withContext(Dispatchers.IO) {
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
            val list = localSourcesDirectories.listState.value
            if (list.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_directories_added_please_add_them_to_see_them_in_the_source_catalog_list),
                    Modifier.textPadding()
                )
            }
            list.forEach {
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
