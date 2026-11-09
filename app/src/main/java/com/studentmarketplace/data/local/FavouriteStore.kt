package com.studentmarketplace.data.local

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.studentmarketplace.data.model.ApiListing

object FavouriteStore {

    private const val PREFS_NAME = "student_marketplace_favourites"
    private const val KEY_FAVOURITES = "saved_listings"

    private val gson = Gson()

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun accountKey(): String {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: throw IllegalStateException("Please sign in first.")

        return "${KEY_FAVOURITES}_$uid"
    }

    // Gets all listings saved by the current student.
    fun getAll(context: Context): List<ApiListing> {
        val json = preferences(context).getString(accountKey(), null)
            ?: return emptyList()

        return try {
            val type = object : TypeToken<List<ApiListing>>() {}.type
            gson.fromJson<List<ApiListing>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Checks whether a listing is already a favourite.
    fun contains(context: Context, listingId: Int): Boolean {
        return getAll(context).any { it.listingId == listingId }
    }

    // Saves a listing to favourites.
    fun save(context: Context, listing: ApiListing) {
        val updated = getAll(context)
            .filterNot { it.listingId == listing.listingId } + listing

        preferences(context).edit()
            .putString(accountKey(), gson.toJson(updated))
            .apply()
    }

    // Removes a listing from favourites.
    fun remove(context: Context, listingId: Int) {
        val updated = getAll(context)
            .filterNot { it.listingId == listingId }

        preferences(context).edit()
            .putString(accountKey(), gson.toJson(updated))
            .apply()
    }
}