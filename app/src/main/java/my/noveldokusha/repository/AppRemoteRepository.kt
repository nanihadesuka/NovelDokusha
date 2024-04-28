package my.noveldokusha.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.noveldokusha.BuildConfig
import my.noveldokusha.core.Response
import my.noveldokusha.domain.AppVersion
import my.noveldokusha.domain.RemoteAppVersion
import my.noveldokusha.network.toJson
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRemoteRepository @Inject constructor(
    private val networkClient: my.noveldokusha.network.NetworkClient,
) {

    private val lastReleaseUrl =
        "https://api.github.com/repos/nanihadesuka/NovelDokusha/releases/latest"

    suspend fun getLastAppVersion(
    ): Response<RemoteAppVersion> = withContext(Dispatchers.Default) {
        return@withContext my.noveldokusha.network.tryConnect {
            val json = networkClient
                .get(lastReleaseUrl)
                .toJson()
                .asJsonObject

            RemoteAppVersion(
                version = AppVersion.fromString(json["tag_name"].asString),
                sourceUrl = json["html_url"].asString
            )
        }
    }

    fun getCurrentAppVersion() = AppVersion.fromString(BuildConfig.VERSION_NAME)
}