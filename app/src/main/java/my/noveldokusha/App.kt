package my.noveldokusha

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import dagger.hilt.android.HiltAndroidApp
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.network.ScrapperNetworkClient
import javax.inject.Inject

@HiltAndroidApp
class App @Inject constructor(

) : Application(), ImageLoaderFactory {

    @Inject
    lateinit var networkClient: NetworkClient

    override fun onCreate() {
        _instance = this
        super.onCreate()
    }

    companion object {
        private var _instance: App? = null
        val instance get() = _instance!!
    }

    override fun newImageLoader(): ImageLoader = when (val networkClient = networkClient) {
        is ScrapperNetworkClient -> ImageLoader
            .Builder(this)
            .okHttpClient(networkClient.client)
            .build()
        else -> ImageLoader(this)
    }
}
