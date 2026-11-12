
package com.studentmarketplace.data.repository

import android.content.Context
import com.studentmarketplace.data.local.database.AppDatabase
import com.studentmarketplace.data.local.database.toApiListing
import com.studentmarketplace.data.local.database.toEntity
import com.studentmarketplace.data.model.ApiListing
import com.studentmarketplace.data.remote.ListingApiClient
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ListingRepository {

    private lateinit var database: AppDatabase

    private lateinit var applicationContext: Context

    private fun getCachedImageFile(id: Int): File {
        val imageDirectory = File(
            applicationContext.filesDir,
            "listing_images"
        )

        if (!imageDirectory.exists()) {
            imageDirectory.mkdirs()
        }

        return File(imageDirectory, "listing_$id.jpg")
    }

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
        database = AppDatabase.getDatabase(applicationContext)
    }

    suspend fun getListings(): List<ApiListing> {
        return try {
            val onlineListings = ListingApiClient.service.getListings()

            database.listingDao().saveListings(
                onlineListings.map { it.toEntity() }
            )

            onlineListings
        } catch (e: Exception) {
            database.listingDao()
                .getAllListings()
                .first()
                .map { it.toApiListing() }
        }
    }

    suspend fun getListingById(id: Int): ApiListing {
        return try {
            val onlineListing = ListingApiClient.service.getListingById(id)

            database.listingDao().saveListings(
                listOf(onlineListing.toEntity())
            )

            onlineListing
        } catch (e: Exception) {
            database.listingDao()
                .getListingById(id)
                ?.toApiListing()
                ?: throw IllegalStateException(
                    "Listing unavailable online and offline."
                )
        }
    }

    suspend fun getListingImage(id: Int): ByteArray =
        withContext(Dispatchers.IO) {

            val cachedImage = getCachedImageFile(id)

            // Return the saved image when available.
            if (cachedImage.exists() && cachedImage.length() > 0) {
                return@withContext cachedImage.readBytes()
            }

            // Otherwise download and cache the image.
            val imageBytes = ListingApiClient.service
                .getListingImage(id)
                .use { it.bytes() }

            cachedImage.writeBytes(imageBytes)

            imageBytes
        }

    suspend fun createListing(
        title: String,
        description: String,
        price: Double,
        condition: String,
        category: String,
        moduleCode: String?,
        imageBytes: ByteArray?,
        imageMimeType: String?
    ): ApiListing {

        val textMediaType = "text/plain".toMediaType()

        val imagePart = if (imageBytes != null && imageMimeType != null) {
            MultipartBody.Part.createFormData(
                "image",
                "listing_image",
                imageBytes.toRequestBody(imageMimeType.toMediaType())
            )
        } else {
            null
        }

        val createdListing = ListingApiClient.service.createListing(
            title.toRequestBody(textMediaType),
            description.toRequestBody(textMediaType),
            price.toString().toRequestBody(textMediaType),
            condition.toRequestBody(textMediaType),
            category.toRequestBody(textMediaType),
            (moduleCode ?: "").toRequestBody(textMediaType),
            imagePart
        )

        database.listingDao().saveListings(
            listOf(createdListing.toEntity())
        )

        return createdListing
    }
}
