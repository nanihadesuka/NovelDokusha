package my.noveldokusha

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import javax.inject.Inject

@HiltAndroidApp
class App @Inject constructor() : Application()
{
    override fun onCreate()
    {
        _instance = this
        super.onCreate()
    }

    companion object
    {
        private lateinit var _instance: App
        val instance get() = _instance
        val cacheDir: File get() = _instance.cacheDir
    }
}
