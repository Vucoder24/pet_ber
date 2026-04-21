package com.nvv.petber.di

import android.content.Context
import com.nvv.petber.BuildConfig
import com.nvv.petber.data.repo.remote.AuthRepository
import com.nvv.petber.data.repo.remote.HomeRepository
import com.nvv.petber.utils.NetworkErrorPlugin
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.ktor.client.plugins.HttpTimeout
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @OptIn(SupabaseInternal::class)
    @Provides
    @Singleton
    fun provideSupabaseClient(@ApplicationContext context: Context): SupabaseClient {
        return createSupabaseClient(
            supabaseKey = BuildConfig.SUPABASE_KEY,
            supabaseUrl = BuildConfig.SUPABASE_URL
        ) {
            install(Auth)
            install(Postgrest)
            install(Storage)
            install(Realtime)
            httpConfig {
                install(HttpTimeout) {
                    requestTimeoutMillis = 30_000
                    connectTimeoutMillis = 30_000
                    socketTimeoutMillis = 30_000
                }
                install(NetworkErrorPlugin) {
                    this.context = context
                }
            }
        }
    }

    @Provides
    @Singleton
    fun provideAuthRepository(supabaseClient: SupabaseClient, context: Context): AuthRepository {
        return AuthRepository(supabaseClient, context)
    }

    @Provides
    @Singleton
    fun provideHomeRepository(supabaseClient: SupabaseClient): HomeRepository {
        return HomeRepository(supabaseClient)
    }

    @Provides
    fun provideContext(
        @ApplicationContext context: Context
    ): Context {
        return context
    }
}