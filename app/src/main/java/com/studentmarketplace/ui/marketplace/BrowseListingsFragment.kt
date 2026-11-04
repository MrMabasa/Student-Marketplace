package com.studentmarketplace.ui.marketplace

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.studentmarketplace.R

// Displays the marketplace listings available to students.
class BrowseListingsFragment : Fragment() {

    // Loads the Browse Listings XML layout.
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_browse_listings,
            container,
            false
        )
    }

    // Connects listing selection to the listing details screen.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Opens listing details when the sample listing is selected.
        view.findViewById<View>(R.id.btnViewListingDetails).setOnClickListener {
            findNavController().navigate(R.id.action_browse_to_listingDetails)
        }
    }
}