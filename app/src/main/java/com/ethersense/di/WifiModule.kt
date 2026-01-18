package com.ethersense.di

import com.ethersense.data.repository.WifiRepository
import com.ethersense.data.repository.WifiRepositoryImpl
import com.ethersense.data.source.WifiScannerDataSource
import com.ethersense.data.source.WifiScannerDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WifiModule {

    @Binds
    @Singleton
    abstract fun bindWifiScannerDataSource(
        impl: WifiScannerDataSourceImpl
    ): WifiScannerDataSource

    @Binds
    @Singleton
    abstract fun bindWifiRepository(
        impl: WifiRepositoryImpl
    ): WifiRepository
}
