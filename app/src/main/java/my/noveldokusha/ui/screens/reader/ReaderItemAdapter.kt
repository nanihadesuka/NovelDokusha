package my.noveldokusha.ui.screens.reader

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.doOnNextLayout
import androidx.core.view.updateLayoutParams
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import my.noveldokusha.AppPreferences
import my.noveldokusha.R
import my.noveldokusha.databinding.*
import my.noveldokusha.uiUtils.inflater
import java.io.File
import java.util.ArrayList

class ReaderItemAdapter(
    val ctx: Context,
    val list: ArrayList<ReaderItem>,
    val localBookBaseFolder: File,
    val fontsLoader: FontsLoader,
    val appPreferences: AppPreferences,
    val onChapterStartVisible: (chapterUrl: String) -> Unit,
    val onChapterEndVisible: (chapterUrl: String) -> Unit,
) :
    ArrayAdapter<ReaderItem>(ctx, 0, list)
{
    override fun getCount() = super.getCount() + 2
    override fun getItem(position: Int): ReaderItem = when (position)
    {
        0 -> topPadding
        this.count - 1 -> bottomPadding
        else -> super.getItem(position - 1)!!
    }

    val topPadding = ReaderItem.PADDING("")
    val bottomPadding = ReaderItem.PADDING("")

    override fun getViewTypeCount(): Int = 9
    override fun getItemViewType(position: Int) = when (val item = getItem(position))
    {
        is ReaderItem.BODY -> if (item.image != null) 0 else 1
        is ReaderItem.BOOK_END -> 2
        is ReaderItem.BOOK_START -> 2
        is ReaderItem.DIVIDER -> 3
        is ReaderItem.ERROR -> 4
        is ReaderItem.PADDING -> 5
        is ReaderItem.PROGRESSBAR -> 6
        is ReaderItem.TITLE -> 7
    }

    private fun viewBody(item: ReaderItem.BODY, convertView: View?, parent: ViewGroup): View
    {
        val bind = when (convertView)
        {
            null -> ActivityReaderListItemBodyBinding.inflate(parent.inflater, parent, false).also { it.root.tag = it }
            else -> ActivityReaderListItemBodyBinding.bind(convertView)
        }

        val paragraph = item.text + "\n"
        bind.body.text = paragraph
        bind.body.textSize = appPreferences.READER_FONT_SIZE.value
        bind.body.typeface = fontsLoader.getTypeFaceNORMAL(appPreferences.READER_FONT_FAMILY.value)

        when (item.location)
        {
            ReaderItem.LOCATION.FIRST -> onChapterStartVisible(item.chapterUrl)
            ReaderItem.LOCATION.LAST -> onChapterEndVisible(item.chapterUrl)
            else -> run {}
        }
        return bind.root
    }

    private fun viewImage(item: ReaderItem.BODY, convertView: View?, parent: ViewGroup): View
    {
        val bind = when (convertView)
        {
            null -> ActivityReaderListItemImageBinding.inflate(parent.inflater, parent, false).also { it.root.tag = it }
            else -> ActivityReaderListItemImageBinding.bind(convertView)
        }

        val imgEntry = item.image ?: return bind.root

        bind.image.updateLayoutParams<ConstraintLayout.LayoutParams> {
            dimensionRatio = "1:${imgEntry.yrel}"
        }

        val imageModel = when {
            imgEntry.path.startsWith("http://", ignoreCase = true) -> imgEntry.path
            imgEntry.path.startsWith("https://", ignoreCase = true) -> imgEntry.path
            else -> File(localBookBaseFolder, imgEntry.path)
        }
            
        // Glide uses current imageView size to load the bitmap best optimized for it, but current
        // size corresponds to the last image (different size) and the view layout only updates to
        // the new values on next redraw. Execute Glide loading call in the next (parent) layout
        // update to let it get the correct values.
        // (Avoids getting "blurry" images)
        bind.imageContainer.doOnNextLayout {
            val s = Glide.with(ctx)
                .load(imageModel)
                .fitCenter()
                .error(R.drawable.ic_baseline_error_outline_24)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(bind.image)
        }

        when (item.location)
        {
            ReaderItem.LOCATION.FIRST -> onChapterStartVisible(item.chapterUrl)
            ReaderItem.LOCATION.LAST -> onChapterEndVisible(item.chapterUrl)
            else -> run {}
        }

        return bind.root
    }

    private fun viewBookEnd(item: ReaderItem.BOOK_END, convertView: View?, parent: ViewGroup): View
    {
        val bind = when (convertView)
        {
            null -> ActivityReaderListItemSpecialTitleBinding.inflate(parent.inflater, parent, false).also { it.root.tag = it }
            else -> ActivityReaderListItemSpecialTitleBinding.bind(convertView)
        }
        bind.specialTitle.text = ctx.getString(R.string.reader_no_more_chapters)
        bind.specialTitle.typeface = fontsLoader.getTypeFaceBOLD(appPreferences.READER_FONT_FAMILY.value)
        return bind.root
    }

    private fun viewBookStart(item: ReaderItem.BOOK_START, convertView: View?, parent: ViewGroup): View
    {
        val bind = when (convertView)
        {
            null -> ActivityReaderListItemSpecialTitleBinding.inflate(parent.inflater, parent, false).also { it.root.tag = it }
            else -> ActivityReaderListItemSpecialTitleBinding.bind(convertView)
        }
        bind.specialTitle.text = ctx.getString(R.string.reader_first_chapter)
        bind.specialTitle.typeface = fontsLoader.getTypeFaceBOLD(appPreferences.READER_FONT_FAMILY.value)
        return bind.root
    }

    private fun viewProgressbar(item: ReaderItem.PROGRESSBAR, convertView: View?, parent: ViewGroup): View
    {
        val bind = when (convertView)
        {
            null -> ActivityReaderListItemProgressBarBinding.inflate(parent.inflater, parent, false).also { it.root.tag = it }
            else -> ActivityReaderListItemProgressBarBinding.bind(convertView)
        }
        return bind.root
    }

    private fun viewDivider(item: ReaderItem.DIVIDER, convertView: View?, parent: ViewGroup): View
    {
        val bind = when (convertView)
        {
            null -> ActivityReaderListItemDividerBinding.inflate(parent.inflater, parent, false).also { it.root.tag = it }
            else -> ActivityReaderListItemDividerBinding.bind(convertView)
        }
        return bind.root
    }

    private fun viewError(item: ReaderItem.ERROR, convertView: View?, parent: ViewGroup): View
    {
        val bind = when (convertView)
        {
            null -> ActivityReaderListItemErrorBinding.inflate(parent.inflater, parent, false).also { it.root.tag = it }
            else -> ActivityReaderListItemErrorBinding.bind(convertView)
        }
        bind.error.text = item.text
        return bind.root
    }

    private fun viewPadding(item: ReaderItem.PADDING, convertView: View?, parent: ViewGroup): View
    {
        val bind = when (convertView)
        {
            null -> ActivityReaderListItemPaddingBinding.inflate(parent.inflater, parent, false).also { it.root.tag = it }
            else -> ActivityReaderListItemPaddingBinding.bind(convertView)
        }
        return bind.root
    }

    private fun viewTitle(item: ReaderItem.TITLE, convertView: View?, parent: ViewGroup): View
    {
        val bind = when (convertView)
        {
            null -> ActivityReaderListItemTitleBinding.inflate(parent.inflater, parent, false).also { it.root.tag = it }
            else -> ActivityReaderListItemTitleBinding.bind(convertView)
        }
        bind.title.text = item.text
        bind.title.typeface = fontsLoader.getTypeFaceBOLD(appPreferences.READER_FONT_FAMILY.value)
        return bind.root
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View = when (val item = getItem(position))
    {
        is ReaderItem.BODY -> when (item.isImage)
        {
            true -> viewImage(item, convertView, parent)
            false -> viewBody(item, convertView, parent)
        }
        is ReaderItem.BOOK_END -> viewBookEnd(item, convertView, parent)
        is ReaderItem.BOOK_START -> viewBookStart(item, convertView, parent)
        is ReaderItem.DIVIDER -> viewDivider(item, convertView, parent)
        is ReaderItem.ERROR -> viewError(item, convertView, parent)
        is ReaderItem.PADDING -> viewPadding(item, convertView, parent)
        is ReaderItem.PROGRESSBAR -> viewProgressbar(item, convertView, parent)
        is ReaderItem.TITLE -> viewTitle(item, convertView, parent)
    }
}