package com.example.spenttracker.data.remote.interceptor

import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserAgentInterceptor @Inject constructor(
    @ApplicationContext private val context: Context
) : Interceptor {
    
    companion object {
        private const val HEADER_USER_AGENT = "User-Agent"
    }
    
    private val userAgent: String by lazy {
        buildUserAgent()
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        val requestWithUserAgent = originalRequest.newBuilder()
            .header(HEADER_USER_AGENT, userAgent)
            .build()
        
        return chain.proceed(requestWithUserAgent)
    }
    
    private fun buildUserAgent(): String {
        val appName = "SpentTracker"
        val appVersion = getAppVersion()
        val androidVersion = Build.VERSION.RELEASE
        val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
        val androidSdk = Build.VERSION.SDK_INT
        
        return "$appName/$appVersion (Android $androidVersion; API $androidSdk; $deviceModel) Mobile App"
    }
    
    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }
}