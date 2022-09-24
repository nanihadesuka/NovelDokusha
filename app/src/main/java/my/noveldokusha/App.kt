package my.noveldokusha

import android.app.Application
import android.content.Context
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.Excludes
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpLibraryGlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.network.ScraperNetworkClient
import java.io.InputStream
import javax.inject.Inject


@HiltAndroidApp
class App @Inject constructor() : Application(), ImageLoaderFactory {

    @Inject
    lateinit var networkClient: NetworkClient

    override fun onCreate() {
        super.onCreate()
    }

    override fun newImageLoader(): ImageLoader = when (val networkClient = networkClient) {
        is ScraperNetworkClient -> ImageLoader
            .Builder(this)
            .okHttpClient(networkClient.client)
            .build()

        else -> ImageLoader(this)
    }
}

@Excludes(OkHttpLibraryGlideModule::class)
@GlideModule
private class CustomGlideModule : AppGlideModule() {

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        val appContext = context.applicationContext
        val networkClient = EntryPointAccessors.fromApplication<NetworkClient>(appContext)
        if (networkClient !is ScraperNetworkClient) {
            return
        }
        registry.replace(
            GlideUrl::class.java,
            InputStream::class.java,
            OkHttpUrlLoader.Factory(networkClient.client)
        )
    }
}
