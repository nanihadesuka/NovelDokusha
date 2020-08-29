package my.noveldokusha.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import my.noveldokusha.MainActivityModel
import my.noveldokusha.R

class DashboardFragment : Fragment()
{
	private val viewModel: MainActivityModel by lazy {
		ViewModelProvider(this.requireActivity()).get(MainActivityModel::class.java)
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
	{
		val view = inflater.inflate(R.layout.fragment_dashboard, container, false)
		val textView: TextView = view.findViewById(R.id.text_dashboard)
		viewModel.dashboardText.observe(viewLifecycleOwner, Observer { textView.text = it })
		return view
	}
}