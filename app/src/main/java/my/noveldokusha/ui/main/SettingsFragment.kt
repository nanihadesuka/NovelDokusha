package my.noveldokusha.ui.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.google.android.material.radiobutton.MaterialRadioButton
import my.noveldokusha.databinding.ActivityMainFragmentSettingsBinding
import my.noveldokusha.ui.BaseFragment

class SettingsFragment : BaseFragment()
{
	class Extras
	{
		fun intent(ctx: Context) = Intent(ctx, SettingsFragment::class.java)
	}
	
	private val viewModel by viewModels<SettingsModel>()
	private lateinit var viewHolder: ActivityMainFragmentSettingsBinding
	private lateinit var viewAdapter: Adapter
	
	private inner class Adapter
	{
	
	}
	
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View
	{
		viewHolder = ActivityMainFragmentSettingsBinding.inflate(inflater, container, false)
		viewAdapter = Adapter()
		
		val currentThemeId = preferencesGetThemeId()
		globalThemeList.forEach { (name, id) ->
			viewHolder.settingsTheme.addView(MaterialRadioButton(requireActivity()).also {
				it.text = name
				it.setOnCheckedChangeListener { _, _ -> preferencesSetThemeId(id) }
				it.isChecked = currentThemeId == id
			})
		}
		
		return viewHolder.root
	}
}
