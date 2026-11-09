package com.studentmarketplace.data.remote

import com.studentmarketplace.data.model.ApiListing
import okhttp3.ResponseBody
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

interface ListingApiService {
    @GET("listings")
    suspend fun getListings(): List<ApiListing>

    @GET("listings/{id}")
    suspend fun getListingById(@Path("id") id: Int): ApiListing

    @GET("listings/{id}/image")
    suspend fun getListingImage(@Path("id") id: Int): ResponseBody
}

object ListingApiClient {
    internal val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .client(OkHttpClient.Builder().addInterceptor(FirebaseAuthInterceptor()).build())
            .baseUrl(com.studentmarketplace.BuildConfig.MARKETPLACE_API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val service: ListingApiService by lazy {
        retrofit.create(ListingApiService::class.java)
    }
}