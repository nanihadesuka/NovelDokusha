package my.noveldokusha

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainActivityModel : ViewModel()
{
	val homeText = MutableLiveData<String>("home fragment")
	val dashboardText = MutableLiveData<String>("dashboard fragment")
}