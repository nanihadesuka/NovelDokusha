package my.noveldokusha.ui.reader

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

//class ReaderItemAdapter(
//    val ctx: Context,
//    val list: ArrayList<ReaderItem>,
//    val onChapterStartVisible: (chapterUrl: String) -> Unit,
//    val onChapterEndVisible: (chapterUrl: String) -> Unit,
//) :
//    ArrayAdapter<ReaderItem>(ctx, 0, list)
//{
//    override fun getCount() = super.getCount() + 2
//    override fun getItem(position: Int): ReaderItem = when (position)
//    {
//        0 -> topPadding
//        this.count - 1 -> bottomPadding
//        else -> super.getItem(position - 1)!!
//    }
//
//    val topPadding = ReaderItem.PADDING("")
//    val bottomPadding = ReaderItem.PADDING("")
//
//    private fun viewBody(item: ReaderItem.BODY, convertView: View?, parent: ViewGroup): View
//    {
//        val bind = when (convertView)
//        {
//            null -> ActivityReaderListItemBodyBinding.inflate(parent.inflater, parent, false).also { it.root.tag = it }
//            else -> ActivityReaderListItemBodyBinding.bind(convertView)
//        }
//
//        when (item.location)
//        {
//            ReaderItem.LOCATION.FIRST -> onChapterStartVisible(item.chapterUrl)
//            ReaderItem.LOCATION.LAST -> onChapterEndVisible(item.chapterUrl)
//            else -> run {}
//        }
//        return bind.root
//    }
//
//    private fun viewImage(item: ReaderItem.BODY, convertView: View?, parent: ViewGroup): View
//    {
//        val bind = when (convertView)
//        {
//            null -> ActivityReaderListItemImageBinding.inflate(parent.inflater, parent, false).also { it.root.tag = it }
//            else -> ActivityReaderListItemImageBinding.bind(convertView)
//        }
//
//        when (item.location)
//        {
//            ReaderItem.LOCATION.FIRST -> onChapterStartVisible(item.chapterUrl)
//            ReaderItem.LOCATION.LAST -> onChapterEndVisible(item.chapterUrl)
//            else -> run {}
//        }
//
//        return bind.root
//    }
//}