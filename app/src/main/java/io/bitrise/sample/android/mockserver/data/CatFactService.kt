package io.bitrise.sample.android.mockserver.data

import retrofit2.http.GET

interface CatFactService {

    @GET("fact")
    suspend fun getCatFact(): CatFact
}