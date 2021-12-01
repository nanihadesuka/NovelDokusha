package my.noveldokusha

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import my.noveldokusha.uiUtils.manager
import my.noveldokusha.uiUtils.setLargeIcon
import java.io.File
import javax.inject.Inject
import kotlin.reflect.KProperty

@HiltAndroidApp
class App @Inject constructor() : Application()
{
    override fun onCreate()
    {
        _instance = this
        super.onCreate()
    }

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main + CoroutineName("App"))

    companion object
    {
        private lateinit var _instance: App
        val instance get() = _instance
        val cacheDir: File get() = _instance.cacheDir
        val scope get() = instance.scope
        fun getDatabasePath(databaseName: String): File = instance.applicationContext.getDatabasePath(databaseName)
    }
}
