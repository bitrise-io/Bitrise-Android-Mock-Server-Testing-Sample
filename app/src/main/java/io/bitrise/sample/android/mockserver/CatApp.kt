package io.bitrise.sample.android.mockserver

import android.app.Application
import io.bitrise.sample.android.mockserver.data.CatFactService
import logcat.AndroidLogcatLogger
import logcat.LogPriority
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory


class CatApp : Application() {

    lateinit var catFactService: CatFactService

    override fun onCreate() {
        super.onCreate()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://catfact.ninja/") // TODO
            .addConverterFactory(MoshiConverterFactory.create())
            .build()

        catFactService = retrofit.create(CatFactService::class.java)

        AndroidLogcatLogger.installOnDebuggableApp(this, minPriority = LogPriority.VERBOSE)
    }
}