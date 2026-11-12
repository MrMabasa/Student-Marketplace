package com.studentmarketplace.data.local.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "listings")
data class ListingEntity(

    @PrimaryKey
    val listingId: Int,

    val sellerId: Int,
    val title: String,
    val description: String,
    val price: Double,
    val itemCondition: String,
    val category: String,
    val moduleCode: String?,
    val status: String,
    val dateCreated: String,
    val hasImage: Int
)