package my.noveldokusha.di

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.Excludes
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpLibraryGlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import dagger.hilt.android.EntryPointAccessors
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.network.ScraperNetworkClient
import java.io.InputStream

@Excludes(OkHttpLibraryGlideModule::class)
@GlideModule
private class GlideModule : AppGlideModule() {

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