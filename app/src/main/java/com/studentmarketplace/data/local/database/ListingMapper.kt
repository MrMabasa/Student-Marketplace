package com.studentmarketplace.data.local.database

import com.studentmarketplace.data.model.ApiListing

fun ApiListing.toEntity(): ListingEntity {
    return ListingEntity(
        listingId = listingId,
        sellerId = sellerId,
        title = title,
        description = description,
        price = price,
        itemCondition = itemCondition,
        category = category,
        moduleCode = moduleCode,
        status = status,
        dateCreated = dateCreated,
        hasImage = hasImage
    )
}

fun ListingEntity.toApiListing(): ApiListing {
    return ApiListing(
        listingId = listingId,
        sellerId = sellerId,
        title = title,
        description = description,
        price = price,
        itemCondition = itemCondition,
        category = category,
        moduleCode = moduleCode,
        status = status,
        dateCreated = dateCreated,
        hasImage = hasImage
    )
}