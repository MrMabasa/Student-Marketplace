
package com.studentmarketplace.data.remote

import com.google.gson.annotations.SerializedName
import com.studentmarketplace.data.model.ApiListing
import okhttp3.ResponseBody
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Body

data class RegisterRequest(
    @SerializedName("full_name")
    val fullName: String,

    @SerializedName("student_number")
    val studentNumber: String,

    val institution: String
)

data class RegisterResponse(
    val message: String,

    @SerializedName("user_id")
    val userId: Int
)

interface ListingApiService {
    @GET("listings")
    suspend fun getListings(): List<ApiListing>

    @GET("listings/{id}")
    suspend fun getListingById(@Path("id") id: Int): ApiListing

    @GET("listings/{id}/image")
    suspend fun getListingImage(@Path("id") id: Int): ResponseBody

    @Multipart
    @POST("listings")
    suspend fun createListing(
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody,
        @Part("price") price: RequestBody,
        @Part("condition") condition: RequestBody,
        @Part("category") category: RequestBody,
        @Part("module_code") moduleCode: RequestBody,
        @Part image: MultipartBody.Part?
    ): ApiListing

    @POST("users/register")
    suspend fun registerUser(
        @Body request: RegisterRequest
    ): RegisterResponse
}

object ListingApiClient {
    internal val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(FirebaseAuthInterceptor())
                    .build()
            )
            .baseUrl(com.studentmarketplace.BuildConfig.MARKETPLACE_API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val service: ListingApiService by lazy {
        retrofit.create(ListingApiService::class.java)
    }
}
