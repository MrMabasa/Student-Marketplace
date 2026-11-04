package com.studentmarketplace.ui.marketplace

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.studentmarketplace.R

// Displays detailed information about a selected marketplace item.
class ListingDetailsFragment : Fragment() {

    // Loads the listing details XML layout.
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_listing_details,
            container,
            false
        )
    }
}