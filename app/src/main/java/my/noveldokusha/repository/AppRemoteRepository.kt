package my.noveldokusha.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.noveldokusha.BuildConfig
import my.noveldokusha.data.Response
import my.noveldokusha.domain.AppVersion
import my.noveldokusha.domain.RemoteAppVersion
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.network.tryConnect
import my.noveldokusha.utils.toJson
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRemoteRepository @Inject constructor(
    private val networkClient: NetworkClient,
) {

    private val lastReleaseUrl =
        "https://api.github.com/repos/nanihadesuka/NovelDokusha/releases/latest"

    suspend fun getLastAppVersion(
    ): Response<RemoteAppVersion> = withContext(Dispatchers.Default) {
        return@withContext tryConnect {
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