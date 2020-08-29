package my.noveldokusha

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

object myUtils {

	object internet {

		// https://stackoverflow.com/questions/51141970/check-internet-connectivity-android-in-kotlin
		fun isConnected(context: Context?): Boolean {
			if (context == null) return false
			val connectivityManager =
				context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
				val capabilities =
					connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
				if (capabilities != null) {
					when {
						capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> return true
						capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> return true
						capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> return true
					}
				}
			} else {
				val activeNetworkInfo = connectivityManager.activeNetworkInfo
				if (activeNetworkInfo != null && activeNetworkInfo.isConnected) return true
			}
			return false
		}
	}
}