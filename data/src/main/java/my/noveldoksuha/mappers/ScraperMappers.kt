package my.noveldoksuha.mappers

import my.noveldokusha.core.PagedList
import my.noveldokusha.core.Response
import my.noveldokusha.core.syncMap
import my.noveldokusha.scraper.domain.BookResult
import my.noveldokusha.scraper.domain.ChapterResult
import my.noveldokusha.tooling.local_database.BookMetadata
import my.noveldokusha.tooling.local_database.ChapterMetadata

fun ChapterResult.mapToChapterMetadata() = ChapterMetadata(
    title = this.title,
    url = this.url,
)

fun List<ChapterResult>.mapToChapterMetadata() = map { it.mapToChapterMetadata() }

fun Response<List<ChapterResult>>.mapToChapterMetadata() = syncMap { it.mapToChapterMetadata() }

fun BookResult.mapToBookMetadata() = BookMetadata(
    title = this.title,
    url = this.url,
    coverImageUrl = this.coverImageUrl,
    description = this.description,
)

fun List<BookResult>.mapToBookMetadata() = map { it.mapToBookMetadata() }

fun PagedList<BookResult>.mapToBookMetadata() = PagedList(
    list = this.list.mapToBookMetadata(),
    index = this.index,
    isLastPage = this.isLastPage
)

fun Response<PagedList<BookResult>>.mapToBookMetadata() = syncMap { it.mapToBookMetadata() }

