package my.noveldokusha.features.reader

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.doOnNextLayout
import androidx.core.view.updateLayoutParams
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import my.noveldokusha.R
import my.noveldokusha.databinding.ActivityReaderListItemBodyBinding
import my.noveldokusha.databinding.ActivityReaderListItemDividerBinding
import my.noveldokusha.databinding.ActivityReaderListItemErrorBinding
import my.noveldokusha.databinding.ActivityReaderListItemGoogleTranslateAttributionBinding
import my.noveldokusha.databinding.ActivityReaderListItemImageBinding
import my.noveldokusha.databinding.ActivityReaderListItemPaddingBinding
import my.noveldokusha.databinding.ActivityReaderListItemProgressBarBinding
import my.noveldokusha.databinding.ActivityReaderListItemSpecialTitleBinding
import my.noveldokusha.databinding.ActivityReaderListItemTitleBinding
import my.noveldokusha.databinding.ActivityReaderListItemTranslatingBinding
import my.noveldokusha.repository.AppFileResolver
import my.noveldokusha.tools.Utterance
import my.noveldokusha.features.reader.features.TextSynthesis
import my.noveldokusha.utils.inflater

class ReaderItemAdapter(
    private val ctx: Context,
    list: List<ReaderItem>,
    private val bookUrl: String,
    private val currentSpeakerActiveItem: () -> TextSynthesis,
    private val currentTextSelectability: () -> Boolean,
    private val currentFontSize: () -> Float,
    private val currentTypeface: () -> Typeface,
    private val currentTypefaceBold: () -> Typeface,
    private val onChapterStartVisible: (chapterUrl: String) -> Unit,
    private val onChapterEndVisible: (chapterUrl: String) -> Unit,
    private val onReloadReader: () -> Unit,
    private val onClick: () -> Unit,
) : ArrayAdapter<ReaderItem>(ctx, 0, list) {
    val appFileResolver = AppFileResolver(ctx)
    override fun getCount() = super.getCount() + 2
    override fun getItem(position: Int): ReaderItem = when (position) {
        0 -> topPadding
        count - 1 -> bottomPadding
        else -> super.getItem(position - 1)!!
    }

    // Ignores paddings as items that are visible
    fun getFirstVisibleItemIndexGivenPosition(firstVisiblePosition: Int): Int =
        when (firstVisiblePosition) {
            in 1 until (count - 1) -> firstVisiblePosition - 1
            0 -> 0
            count - 1 -> count - 1
            else -> -1
        }

    // Get list index from current position
    fun fromPositionToIndex(position: Int): Int = when (position) {
        in 1 until (count - 1) -> position - 1
        else -> -1
    }

    fun fromIndexToPosition(index: ItemIndex): Int = when (index) {
        in 0 until super.getCount() -> index + 1
        else -> -1
    }

    private val topPadding = ReaderItem.Padding(chapterIndex = Int.MIN_VALUE)
    private val bottomPadding = ReaderItem.Padding(chapterIndex = Int.MAX_VALUE)

    override fun getViewTypeCount(): Int = 11
    override fun getItemViewType(position: Int) = when (getItem(position)) {
        is ReaderItem.Body -> 0
        is ReaderItem.Image -> 1
        is ReaderItem.BookEnd -> 2
        is ReaderItem.BookStart -> 3
        is ReaderItem.Divider -> 4
        is ReaderItem.Error -> 5
        is ReaderItem.Padding -> 6
        is ReaderItem.Progressbar -> 7
        is ReaderItem.Title -> 8
        is ReaderItem.Translating -> 9
        is ReaderItem.GoogleTranslateAttribution -> 10
    }

    private fun viewTranslateAttribution(
        convertView: View?,
        parent: ViewGroup
    ): View {
        val bind = when (convertView) {
            null -> ActivityReaderListItemGoogleTranslateAttributionBinding.inflate(
                parent.inflater,
                parent,
                false
            ).also { it.root.tag = it }
            else -> ActivityReaderListItemGoogleTranslateAttributionBinding.bind(convertView)
        }
        return bind.root
    }

    private fun viewBody(item: ReaderItem.Body, convertView: View?, parent: ViewGroup): View {
        val bind = when (convertView) {
            null -> ActivityReaderListItemBodyBinding.inflate(parent.inflater, parent, false)
                .also { it.root.tag = it }
            else -> ActivityReaderListItemBodyBinding.bind(convertView)
        }

        bind.body.updateTextSelectability()
        bind.root.background = getItemReadingStateBackground(item)
        val paragraph = item.textToDisplay + "\n"
        bind.body.text = paragraph
        bind.body.textSize = currentFontSize()
        bind.body.typeface = currentTypeface()

        when (item.location) {
            ReaderItem.Location.FIRST -> onChapterStartVisible(item.chapterUrl)
            ReaderItem.Location.LAST -> onChapterEndVisible(item.chapterUrl)
            else -> run {}
        }
        return bind.root
    }

    private fun viewImage(
        item: ReaderItem.Image,
        convertView: View?,
        parent: ViewGroup
    ): View {
        val bind = when (convertView) {
            null -> ActivityReaderListItemImageBinding.inflate(parent.inflater, parent, false)
                .also { it.root.tag = it }
            else -> ActivityReaderListItemImageBinding.bind(convertView)
        }

        bind.image.updateLayoutParams<ConstraintLayout.LayoutParams> {
            dimensionRatio = "1:${item.image.yrel}"
        }

        val imageModel = appFileResolver.resolvedBookImagePath(
            bookUrl = bookUrl,
            imagePath = item.image.path
        )

        // Glide uses current imageView size to load the bitmap best optimized for it, but current
        // size corresponds to the last image (different size) and the view layout only updates to
        // the new values on next redraw. Execute Glide loading call in the next (parent) layout
        // update to let it get the correct values.
        // (Avoids getting "blurry" images)
        bind.imageContainer.doOnNextLayout {
            Glide.with(ctx)
                .load(imageModel)
                .fitCenter()
                .error(R.drawable.ic_baseline_error_outline_24)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(bind.image)
        }

        when (item.location) {
            ReaderItem.Location.FIRST -> onChapterStartVisible(item.chapterUrl)
            ReaderItem.Location.LAST -> onChapterEndVisible(item.chapterUrl)
            else -> run {}
        }

        return bind.root
    }

    private fun viewBookEnd(
        convertView: View?,
        parent: ViewGroup
    ): View {
        val bind = when (convertView) {
            null -> ActivityReaderListItemSpecialTitleBinding.inflate(
                parent.inflater,
                parent,
                false
            ).also { it.root.tag = it }
            else -> ActivityReaderListItemSpecialTitleBinding.bind(convertView)
        }

        bind.specialTitle.updateTextSelectability()
        bind.specialTitle.text = ctx.getString(R.string.reader_no_more_chapters)
        bind.specialTitle.typeface = currentTypefaceBold()
        return bind.root
    }

    private fun viewBookStart(
        convertView: View?,
        parent: ViewGroup
    ): View {
        val bind = when (convertView) {

            null -> ActivityReaderListItemSpecialTitleBinding.inflate(
                parent.inflater,
                parent,
                false
            ).also { it.root.tag = it }
            else -> ActivityReaderListItemSpecialTitleBinding.bind(convertView)
        }

        bind.specialTitle.updateTextSelectability()
        bind.specialTitle.text = ctx.getString(R.string.reader_first_chapter)
        bind.specialTitle.typeface = currentTypefaceBold()
        return bind.root
    }

    private fun viewProgressbar(
        convertView: View?,
        parent: ViewGroup
    ): View {
        val bind = when (convertView) {
            null -> ActivityReaderListItemProgressBarBinding.inflate(parent.inflater, parent, false)
                .also { it.root.tag = it }
            else -> ActivityReaderListItemProgressBarBinding.bind(convertView)
        }
        return bind.root
    }

    private fun viewTranslating(
        item: ReaderItem.Translating,
        convertView: View?,
        parent: ViewGroup
    ): View {
        val bind = when (convertView) {
            null -> ActivityReaderListItemTranslatingBinding.inflate(parent.inflater, parent, false)
                .also { it.root.tag = it }
            else -> ActivityReaderListItemTranslatingBinding.bind(convertView)
        }
        bind.text.text = context.getString(
            R.string.translating_from_lang_a_to_lang_b,
            item.sourceLang,
            item.targetLang
        )
        return bind.root
    }

    private fun viewDivider(convertView: View?, parent: ViewGroup): View {
        val bind = when (convertView) {
            null -> ActivityReaderListItemDividerBinding.inflate(parent.inflater, parent, false)
                .also { it.root.tag = it }
            else -> ActivityReaderListItemDividerBinding.bind(convertView)
        }
        return bind.root
    }

    private fun viewError(item: ReaderItem.Error, convertView: View?, parent: ViewGroup): View {
        val bind = when (convertView) {
            null -> ActivityReaderListItemErrorBinding.inflate(parent.inflater, parent, false)
                .also { it.root.tag = it }
            else -> ActivityReaderListItemErrorBinding.bind(convertView)
        }

        bind.error.updateTextSelectability()
        bind.reloadButton.setOnClickListener { onReloadReader() }
        bind.error.text = item.text
        return bind.root
    }

    private fun viewPadding(convertView: View?, parent: ViewGroup): View {
        val bind = when (convertView) {
            null -> ActivityReaderListItemPaddingBinding.inflate(parent.inflater, parent, false)
                .also { it.root.tag = it }
            else -> ActivityReaderListItemPaddingBinding.bind(convertView)
        }
        return bind.root
    }

    private fun viewTitle(item: ReaderItem.Title, convertView: View?, parent: ViewGroup): View {
        val bind = when (convertView) {
            null -> ActivityReaderListItemTitleBinding.inflate(parent.inflater, parent, false)
                .also { it.root.tag = it }
            else -> ActivityReaderListItemTitleBinding.bind(convertView)
        }

        bind.title.updateTextSelectability()
        bind.root.background = getItemReadingStateBackground(item)
        bind.title.text = item.textToDisplay
        bind.title.typeface = currentTypefaceBold()
        return bind.root
    }

    private val currentReadingAloudDrawable by lazy {
        AppCompatResources.getDrawable(
            context,
            R.drawable.translucent_current_reading_text_background
        )
    }

    private val currentReadingAloudLoadingDrawable by lazy {
        AppCompatResources.getDrawable(
            context,
            R.drawable.translucent_current_reading_loading_text_background
        )
    }

    private fun TextView.updateTextSelectability() {
        val selectableText = currentTextSelectability()
        setTextIsSelectable(selectableText)
        if (selectableText) {
            setTextSelectionAwareClick { onClick() }
        }
    }

    private fun getItemReadingStateBackground(item: ReaderItem): Drawable? {
        val textSynthesis = currentSpeakerActiveItem()
        val isReadingItem = item is ReaderItem.Position &&
                textSynthesis.itemPos.chapterIndex == item.chapterIndex &&
                textSynthesis.itemPos.chapterItemPosition == item.chapterItemPosition

        if (!isReadingItem) return null

        return when (textSynthesis.playState) {
            Utterance.PlayState.PLAYING -> currentReadingAloudDrawable
            Utterance.PlayState.LOADING -> currentReadingAloudLoadingDrawable
            Utterance.PlayState.FINISHED -> null
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
        when (val item = getItem(position)) {
            is ReaderItem.GoogleTranslateAttribution -> viewTranslateAttribution(
                convertView,
                parent
            )
            is ReaderItem.Body -> viewBody(item, convertView, parent)
            is ReaderItem.Image -> viewImage(item, convertView, parent)
            is ReaderItem.BookEnd -> viewBookEnd(convertView, parent)
            is ReaderItem.BookStart -> viewBookStart(convertView, parent)
            is ReaderItem.Divider -> viewDivider(convertView, parent)
            is ReaderItem.Error -> viewError(item, convertView, parent)
            is ReaderItem.Padding -> viewPadding(convertView, parent)
            is ReaderItem.Progressbar -> viewProgressbar(convertView, parent)
            is ReaderItem.Translating -> viewTranslating(item, convertView, parent)
            is ReaderItem.Title -> viewTitle(item, convertView, parent)
        }
}

private fun View.setTextSelectionAwareClick(action: () -> Unit) {
    setOnClickListener { action() }
    setOnTouchListener { _, event ->
        if (event.action == MotionEvent.ACTION_UP && !this.isFocused) {
            performClick()
        }
        false
    }
}