package my.noveldokusha.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.asLiveData
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import my.noveldokusha.AppPreferences
import my.noveldokusha.R
import my.noveldokusha.databinding.ActivityMainFragmentLibraryBottomsheetOptionsBinding
import my.noveldokusha.uiViews.Checkbox3StatesView

class LibraryFragmentBottomSheetDialog : BottomSheetDialogFragment()
{
    companion object
    {
        val tag = this::class.simpleName
    }

    private lateinit var preferences: AppPreferences

    private lateinit var viewBind: ActivityMainFragmentLibraryBottomsheetOptionsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        viewBind = ActivityMainFragmentLibraryBottomsheetOptionsBinding.inflate(layoutInflater, container, false)
        return viewBind.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)
        preferences = AppPreferences(requireContext().applicationContext)
        initFilterRead()
        initSortRead()
    }

    private fun initFilterRead()
    {
        viewBind.filterRead.state = when (preferences.LIBRARY_FILTER_READ)
        {
            AppPreferences.TERNARY_STATE.active -> Checkbox3StatesView.STATE.POSITIVE
            AppPreferences.TERNARY_STATE.inverse -> Checkbox3StatesView.STATE.NEGATIVE
            AppPreferences.TERNARY_STATE.inactive -> Checkbox3StatesView.STATE.NONE
        }

        viewBind.filterRead.onStateChangeListener = {
            preferences.LIBRARY_FILTER_READ = when (it)
            {
                Checkbox3StatesView.STATE.POSITIVE -> AppPreferences.TERNARY_STATE.active
                Checkbox3StatesView.STATE.NEGATIVE -> AppPreferences.TERNARY_STATE.inverse
                Checkbox3StatesView.STATE.NONE -> AppPreferences.TERNARY_STATE.inactive
            }
        }
    }

    private fun initSortRead()
    {
        preferences.LIBRARY_SORT_READ_flow().asLiveData().observe(viewLifecycleOwner) {
            if (it == null) return@observe
            val drawableId = when (it)
            {
                AppPreferences.TERNARY_STATE.active -> R.drawable.ic_baseline_arrow_upward_24
                AppPreferences.TERNARY_STATE.inverse -> R.drawable.ic_baseline_arrow_downward_24
                AppPreferences.TERNARY_STATE.inactive -> R.drawable.ic_baseline_empty_24
            }
            viewBind.sortRead.setCompoundDrawablesWithIntrinsicBounds(drawableId, 0, 0, 0)
        }

        viewBind.sortRead.setOnClickListener {
            preferences.LIBRARY_SORT_READ = when (preferences.LIBRARY_SORT_READ)
            {
                AppPreferences.TERNARY_STATE.active -> AppPreferences.TERNARY_STATE.inverse
                AppPreferences.TERNARY_STATE.inverse -> AppPreferences.TERNARY_STATE.inactive
                AppPreferences.TERNARY_STATE.inactive -> AppPreferences.TERNARY_STATE.active
            }
        }
    }
}