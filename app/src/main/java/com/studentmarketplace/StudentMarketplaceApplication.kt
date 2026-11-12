package com.studentmarketplace

import android.app.Application
import com.studentmarketplace.data.repository.ListingRepository

class StudentMarketplaceApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        ListingRepository.initialize(this)
    }
}