package my.noveldokusha.ui

import android.content.Context.MODE_PRIVATE
import androidx.fragment.app.Fragment
import my.noveldokusha.R

open class BaseFragment : Fragment()
{
	fun preferencesGetTheme() = requireActivity().getSharedPreferences("GLOBAL_THEME", MODE_PRIVATE)
	fun preferencesGetThemeId() = preferencesGetTheme().getInt("id", R.style.AppTheme_Light)
	fun preferencesSetThemeId(id: Int) = preferencesGetTheme().edit().putInt("id", id).apply()
	
	companion object
	{
		val globalThemeList = BaseActivity.globalThemeList
	}
}