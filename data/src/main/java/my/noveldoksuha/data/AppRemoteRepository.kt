package my.noveldoksuha.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.noveldokusha.core.AppInternalState
import my.noveldokusha.core.Response
import my.noveldokusha.core.domain.AppVersion
import my.noveldokusha.core.domain.RemoteAppVersion
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.network.toJson
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRemoteRepository @Inject constructor(
    private val networkClient: NetworkClient,
    private val appInternalState: AppInternalState,
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

    fun getCurrentAppVersion() = AppVersion.fromString(appInternalState.versionName)
}