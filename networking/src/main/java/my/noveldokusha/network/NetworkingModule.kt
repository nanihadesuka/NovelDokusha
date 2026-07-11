package my.noveldokusha.network

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
abstract class NetworkingModule {

    @Singleton
    @Binds
    internal abstract fun bindNetworkClient(client: ScraperNetworkClient): NetworkClient
}