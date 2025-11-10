package com.example.spenttracker.di

import com.example.spenttracker.data.remote.api.AuthApiService
import com.example.spenttracker.data.remote.api.ExpenseApiService
import com.example.spenttracker.data.remote.api.CategoryApiService
import com.example.spenttracker.data.remote.auth.AuthTokenProvider
import com.example.spenttracker.data.remote.auth.AuthTokenProviderImpl
import com.example.spenttracker.data.remote.interceptor.AuthInterceptor
import com.example.spenttracker.data.remote.interceptor.UserAgentInterceptor
import com.example.spenttracker.data.auth.UserContextProvider
import com.example.spenttracker.data.auth.UserContextProviderImpl
import dagger.Module
import dagger.Provides
import dagger.Binds
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {
    
    @Binds
    abstract fun bindAuthTokenProvider(
        authTokenProviderImpl: AuthTokenProviderImpl
    ): AuthTokenProvider
    
    @Binds
    abstract fun bindUserContextProvider(
        userContextProviderImpl: UserContextProviderImpl
    ): UserContextProvider
    
    companion object {
    
    private const val BASE_URL = "https://spentracker.live/api/"
    
    @Provides
    @Singleton
    fun provideBaseOkHttpClient(
        userAgentInterceptor: UserAgentInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(userAgentInterceptor)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }
    
    @Provides
    @Singleton
    @Named("auth_intercepted")
    fun provideAuthInterceptedOkHttpClient(
        baseClient: OkHttpClient,
        authInterceptor: AuthInterceptor
    ): OkHttpClient {
        return baseClient.newBuilder()
            .addInterceptor(authInterceptor)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideRetrofit(
        @Named("auth_intercepted") 
        okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    @Provides
    @Singleton
    @Named("refresh")
    fun provideRefreshRetrofit(baseClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(baseClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    @Provides
    @Singleton
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }
    
    @Provides
    @Singleton
    @Named("refresh")
    fun provideRefreshApiService(
        @Named("refresh") 
        retrofit: Retrofit
    ): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }
    
    @Provides
    @Singleton
    fun provideExpenseApiService(retrofit: Retrofit): ExpenseApiService {
        return retrofit.create(ExpenseApiService::class.java)
    }
    
    @Provides
    @Singleton
    fun provideCategoryApiService(retrofit: Retrofit): CategoryApiService {
        return retrofit.create(CategoryApiService::class.java)
    }
    
    } // End companion object
}