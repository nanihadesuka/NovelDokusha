package my.noveldokusha.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import my.noveldokusha.MainActivityModel
import my.noveldokusha.R
import my.noveldokusha.myUtils
import my.noveldokusha.scrubber
import org.jetbrains.anko.support.v4.runOnUiThread
import org.jetbrains.anko.support.v4.toast
import java.util.concurrent.TimeoutException

class HomeFragment : Fragment()
{
	private lateinit var job: Job
	private lateinit var textView: TextView

	private val viewModel: MainActivityModel by lazy {
		ViewModelProvider(this.requireActivity()).get(MainActivityModel::class.java)
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
	{
		val view = inflater.inflate(R.layout.fragment_home, container, false)
		textView = view.findViewById(R.id.text_home)
		viewModel.homeText.observe(viewLifecycleOwner, Observer { textView.text = it })

		if (savedInstanceState is Bundle)
			return view

		loadChapterList()
		return view
	}

	fun loadChapterList()
	{
		if (this::job.isInitialized)
			job.cancel()

		job = GlobalScope.launch {
			try
			{
				runOnUiThread { toast("Loading chaper list") }
				if (!myUtils.internet.isConnected(context))
				{
					runOnUiThread { toast("No internet connection") }
					return@launch
				}

				val chapterArray = scrubber.source_WebNovelOnline.getChaptersList("the_beginning_after_the_end")
				viewModel.homeText.postValue(chaptersToString(chapterArray))

			} catch (ex: TimeoutException)
			{
				runOnUiThread { toast("Timeout when loading chapters list") }
			} catch (ex: Exception)
			{
				runOnUiThread { toast("Error: $ex") }
			}
		}
	}

	fun chaptersToString(chapters: Array<scrubber.Chapter>): String
	{
		return chapters.map { "${it.index}: ${it.title}" }.joinToString("\n")
	}

	override fun onDestroy()
	{
//		if (this::job.isInitialized)
//			job.cancel()
		super.onDestroy()
	}
}