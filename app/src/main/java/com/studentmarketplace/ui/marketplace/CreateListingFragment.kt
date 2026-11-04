package com.studentmarketplace.ui.marketplace

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.studentmarketplace.R

// Displays the form used to create a new marketplace listing.
class CreateListingFragment : Fragment() {

    // Loads the Create Listing XML layout.
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_create_listing,
            container,
            false
        )
    }

    // Connects the submission button to the browsing screen.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Temporary navigation for testing.
        // Actual listing validation and submission will be implemented later.
        view.findViewById<View>(R.id.btnSubmitListing).setOnClickListener {
            findNavController().navigate(R.id.action_createListing_to_browse)
        }
    }
}