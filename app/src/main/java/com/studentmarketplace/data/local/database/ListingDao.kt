package com.studentmarketplace.data.local.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ListingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveListings(listings: List<ListingEntity>)

    @Query("SELECT * FROM listings ORDER BY dateCreated DESC")
    fun getAllListings(): Flow<List<ListingEntity>>

    @Query("SELECT * FROM listings WHERE listingId = :id")
    suspend fun getListingById(id: Int): ListingEntity?

    @Query("DELETE FROM listings")
    suspend fun clearListings()
}