package com.studentmarketplace.ui.marketplace

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.studentmarketplace.R

// Displays the main Student Marketplace dashboard.
class HomeFragment : Fragment() {

    // Loads the Home screen XML layout.
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_home,
            container,
            false
        )
    }

    // Connects the Home screen buttons to their destinations.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Opens the screen containing available marketplace listings.
        view.findViewById<View>(R.id.btnBrowseListings).setOnClickListener {
            findNavController().navigate(R.id.action_home_to_browse)
        }

        // Opens the form for creating a new listing.
        view.findViewById<View>(R.id.btnCreateListing).setOnClickListener {
            findNavController().navigate(R.id.action_home_to_createListing)
        }
    }
}