package com.studentmarketplace.ui.marketplace

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.studentmarketplace.R
import com.studentmarketplace.data.local.FavouriteStore

class FavouritesFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_favourites,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        displayFavourites(view)
    }

    // Refreshes the screen when the student returns to it.
    override fun onResume() {
        super.onResume()
        view?.let { displayFavourites(it) }
    }

    private fun displayFavourites(root: View) {

        val container = root.findViewById<LinearLayout>(
            R.id.favouritesContainer
        )

        val emptyMessage = root.findViewById<TextView>(
            R.id.tvFavouritesEmpty
        )

        container.removeAllViews()

        val savedListings = try {
            FavouriteStore.getAll(requireContext())
        } catch (e: Exception) {
            emptyMessage.visibility = View.VISIBLE
            emptyMessage.text = "Please sign in to view your favourites."
            return
        }

        if (savedListings.isEmpty()) {
            emptyMessage.visibility = View.VISIBLE
            emptyMessage.text = "You have not saved any listings yet."
            return
        }

        emptyMessage.visibility = View.GONE

        savedListings.forEach { listing ->

            val card = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(32, 24, 32, 24)
                setBackgroundColor(
                    resources.getColor(R.color.brand_white, null)
                )
            }

            val title = TextView(requireContext()).apply {
                text = listing.title
                textSize = 18f
                setTextColor(
                    resources.getColor(R.color.brand_navy, null)
                )
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            val details = TextView(requireContext()).apply {
                text = "R${listing.price}\n" +
                        "${listing.itemCondition} • ${listing.category}"
                textSize = 14f
                setTextColor(
                    resources.getColor(R.color.text_secondary, null)
                )
                setPadding(0, 12, 0, 12)
            }

            val viewButton = Button(requireContext()).apply {
                text = "View Listing"

                setOnClickListener {
                    val arguments = Bundle().apply {
                        putInt("listingId", listing.listingId)
                    }

                    findNavController().navigate(
                        R.id.listingDetailsFragment,
                        arguments
                    )
                }
            }

            val removeButton = Button(requireContext()).apply {
                text = "Remove from Favourites"

                setOnClickListener {
                    FavouriteStore.remove(
                        requireContext(),
                        listing.listingId
                    )

                    Toast.makeText(
                        requireContext(),
                        "Listing removed from favourites.",
                        Toast.LENGTH_SHORT
                    ).show()

                    displayFavourites(root)
                }
            }

            card.addView(title)
            card.addView(details)
            card.addView(viewButton)
            card.addView(removeButton)

            val spacing = (12 * resources.displayMetrics.density).toInt()

            container.addView(
                card,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = spacing
                }
            )
        }
    }
}