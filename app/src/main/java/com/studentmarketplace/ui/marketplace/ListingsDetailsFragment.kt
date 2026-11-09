package com.studentmarketplace.ui.marketplace

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.studentmarketplace.R
import com.studentmarketplace.data.repository.ListingRepository
import com.studentmarketplace.data.local.FavouriteStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class ListingDetailsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_listing_details, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val listingId = arguments?.getInt("listingId", -1) ?: -1
        if (listingId <= 0) {
            Toast.makeText(requireContext(), "Listing details are unavailable.", Toast.LENGTH_LONG).show()
            return
        }

        val image = view.findViewById<ImageView>(R.id.ivListingImage)
        image.visibility = View.GONE
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val listing = withContext(Dispatchers.IO) {
                    ListingRepository.getListingById(listingId)
                }
                view.findViewById<TextView>(R.id.tvListingTitle).text = listing.title
                val price = String.format(Locale.getDefault(), "%.2f", listing.price)
                view.findViewById<TextView>(R.id.tvListingPrice).text = "Price: R$price"
                view.findViewById<TextView>(R.id.tvListingCondition).text =
                    "Condition: ${listing.itemCondition} • ${listing.category}"
                view.findViewById<TextView>(R.id.tvListingDescription).text = listing.description
                view.findViewById<TextView>(R.id.tvSellerInformation).text =
                    "Seller ID: ${listing.sellerId}"

                // Gets the Favourites button.
                val favouriteButton = view.findViewById<android.widget.Button>(R.id.btnFavourite)

                // Updates the button depending on whether the listing is saved.
                fun updateFavouriteButton() {
                    favouriteButton.text = if (
                        FavouriteStore.contains(requireContext(), listing.listingId)
                    ) {
                        "Remove from Favourites"
                    } else {
                        "Save to Favourites"
                    }
                }

// Shows the correct button text when the screen opens.
                updateFavouriteButton()

// Saves or removes the listing when the student taps the button.
                favouriteButton.setOnClickListener {
                    if (FavouriteStore.contains(requireContext(), listing.listingId)) {
                        FavouriteStore.remove(requireContext(), listing.listingId)

                        Toast.makeText(
                            requireContext(),
                            "Removed from Favourites",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        FavouriteStore.save(requireContext(), listing)

                        Toast.makeText(
                            requireContext(),
                            "Added to Favourites",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    updateFavouriteButton()
                }

                if (listing.hasImage != 0) {
                    try {
                        val bytes = withContext(Dispatchers.IO) {
                            ListingRepository.getListingImage(listingId)
                        }
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        if (bitmap != null) {
                            image.setImageBitmap(bitmap)
                            image.visibility = View.VISIBLE
                        }
                    } catch (_: Exception) {
                        image.visibility = View.GONE
                    }
                }
            } catch (_: Exception) {
                if (isAdded) {
                    Toast.makeText(
                        requireContext(),
                        "Listing details could not be loaded. Please try again.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}