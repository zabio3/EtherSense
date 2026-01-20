package com.ethersense.di

import android.content.Context
import android.net.ConnectivityManager
import android.net.nsd.NsdManager
import android.net.wifi.WifiManager
import com.ethersense.data.source.NetworkInfoDataSource
import com.ethersense.data.source.NsdServiceDiscovery
import com.ethersense.data.source.SsdpDiscovery
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ToolsModule {

    @Provides
    @Singleton
    fun provideConnectivityManager(
        @ApplicationContext context: Context
    ): ConnectivityManager {
        return context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    @Provides
    @Singleton
    fun provideNsdManager(
        @ApplicationContext context: Context
    ): NsdManager {
        return context.getSystemService(Context.NSD_SERVICE) as NsdManager
    }

    @Provides
    @Singleton
    fun provideNetworkInfoDataSource(
        @ApplicationContext context: Context,
        wifiManager: WifiManager
    ): NetworkInfoDataSource {
        return NetworkInfoDataSource(context, wifiManager)
    }

    @Provides
    @Singleton
    fun provideNsdServiceDiscovery(
        @ApplicationContext context: Context
    ): NsdServiceDiscovery {
        return NsdServiceDiscovery(context)
    }

    @Provides
    @Singleton
    fun provideSsdpDiscovery(): SsdpDiscovery {
        return SsdpDiscovery()
    }
}
