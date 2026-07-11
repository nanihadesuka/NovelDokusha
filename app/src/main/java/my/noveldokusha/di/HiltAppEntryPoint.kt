package my.noveldokusha.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import my.noveldokusha.tooling.application_workers.setup.AppWorkerFactory

@EntryPoint
@InstallIn(SingletonComponent::class)
interface HiltAppEntryPoint {
    fun workerFactory(): AppWorkerFactory
}