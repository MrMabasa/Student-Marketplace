package com.studentmarketplace.ui.marketplace

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.studentmarketplace.R
import com.studentmarketplace.data.model.ApiListing
import com.studentmarketplace.data.repository.ListingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class BrowseListingsFragment : Fragment() {
    private lateinit var firebaseAuth: FirebaseAuth
    private var isRedirecting = false
    private var allListings: List<ApiListing> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_browse_listings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firebaseAuth = FirebaseAuth.getInstance()
        view.visibility = View.INVISIBLE

        val user = firebaseAuth.currentUser
        if (user == null) {
            redirectToSignIn()
            return
        }

        user.reload().addOnCompleteListener { task ->

            if (!isAdded) return@addOnCompleteListener

            val refreshedUser = firebaseAuth.currentUser

            if (refreshedUser == null) {
                redirectToSignIn()
                return@addOnCompleteListener
            }

            if (task.isSuccessful) {

                if (refreshedUser.isEmailVerified) {
                    view.visibility = View.VISIBLE
                    setupSearchAndFilters(view)
                    loadListings(view)
                } else {
                    redirectToVerification()
                }

            } else {

                // Internet may be unavailable.
                // Use the locally cached Firebase verification status.
                if (refreshedUser.isEmailVerified) {

                    Toast.makeText(
                        requireContext(),
                        "Offline mode: showing previously downloaded listings.",
                        Toast.LENGTH_LONG
                    ).show()

                    view.visibility = View.VISIBLE
                    setupSearchAndFilters(view)
                    loadListings(view)

                } else {

                    Toast.makeText(
                        requireContext(),
                        "Connect to the internet to verify your account.",
                        Toast.LENGTH_LONG
                    ).show()

                    redirectToVerification()
                }
            }
        }
    }

    private fun setupSearchAndFilters(view: View) {
        val searchField = view.findViewById<EditText>(R.id.etSearchListings)
        val categoryField = view.findViewById<AutoCompleteTextView>(R.id.etFilterCategory)
        val conditionField = view.findViewById<AutoCompleteTextView>(R.id.etFilterCondition)

        val categories = listOf(
            "All Categories",
            "Textbooks and Study Material",
            "Electronics and Accessories",
            "Stationery and Course Equipment",
            "Dorm and Daily Essentials",
            "Other Student Items"
        )
        val conditions = listOf(
            "All Conditions",
            "Brand New",
            "Like New",
            "Good",
            "Fair",
            "For Parts / Repair"
        )

        categoryField.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        )
        conditionField.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, conditions)
        )
        categoryField.setText(categories[0], false)
        conditionField.setText(conditions[0], false)

        searchField.doAfterTextChanged { applyFilters(view) }
        categoryField.setOnItemClickListener { _, _, _, _ -> applyFilters(view) }
        conditionField.setOnItemClickListener { _, _, _, _ -> applyFilters(view) }
        view.findViewById<EditText>(R.id.etMinPrice).doAfterTextChanged { applyFilters(view) }
        view.findViewById<EditText>(R.id.etMaxPrice).doAfterTextChanged { applyFilters(view) }

        view.findViewById<View>(R.id.btnClearFilters).setOnClickListener {
            searchField.text.clear()
            view.findViewById<EditText>(R.id.etMinPrice).text.clear()
            view.findViewById<EditText>(R.id.etMaxPrice).text.clear()
            categoryField.setText(categories[0], false)
            conditionField.setText(conditions[0], false)
            applyFilters(view)
        }
        view.findViewById<View>(R.id.btnRetryListings).setOnClickListener { loadListings(view) }
    }

    private fun loadListings(view: View) {
        val status = view.findViewById<TextView>(R.id.tvListingStatus)
        val retry = view.findViewById<View>(R.id.btnRetryListings)

        status.text = "Loading listings…"
        status.visibility = View.VISIBLE
        retry.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    ListingRepository.getListings()
                }

                if (!isAdded) return@launch

                allListings = result
                applyFilters(view)

                val connectivityMessage = if (result.isNotEmpty()) {
                    "Listings loaded. Previously downloaded listings are available offline."
                } else {
                    "No listings are available yet."
                }

                status.text = connectivityMessage
                status.visibility = if (result.isEmpty()) View.VISIBLE else View.GONE

            } catch (e: Exception) {
                if (!isAdded) return@launch

                allListings = emptyList()
                status.text = "Listings could not be loaded. Check your connection and try again."
                status.visibility = View.VISIBLE
                retry.visibility = View.VISIBLE
            }
        }
    }

    private fun applyFilters(view: View) {
        val query = view.findViewById<EditText>(R.id.etSearchListings).text.toString().trim()
        val category = view.findViewById<AutoCompleteTextView>(R.id.etFilterCategory).text.toString()
        val condition = view.findViewById<AutoCompleteTextView>(R.id.etFilterCondition).text.toString()
        val minPrice = view.findViewById<EditText>(R.id.etMinPrice).text.toString().toDoubleOrNull()
        val maxPrice = view.findViewById<EditText>(R.id.etMaxPrice).text.toString().toDoubleOrNull()

        val filtered = allListings.filter { listing ->
            listing.status.equals("Active", ignoreCase = true) &&
                (query.isBlank() ||
                    listing.title.contains(query, ignoreCase = true) ||
                    listing.description.contains(query, ignoreCase = true) ||
                    (listing.moduleCode?.contains(query, ignoreCase = true) == true)) &&
                (category == "All Categories" || listing.category == category) &&
                (condition == "All Conditions" || listing.itemCondition == condition) &&
                (minPrice == null || listing.price >= minPrice) &&
                (maxPrice == null || listing.price <= maxPrice)
        }

        view.findViewById<TextView>(R.id.tvListingResults).text =
            "Available Items (${filtered.size})"
        val status = view.findViewById<TextView>(R.id.tvListingStatus)
        val retry = view.findViewById<View>(R.id.btnRetryListings)
        val listingContainer = view.findViewById<LinearLayout>(R.id.listingContainer)
        listingContainer.removeAllViews()

        if (filtered.isEmpty()) {
            status.text = if (allListings.isEmpty()) {
                "No listings are available yet."
            } else {
                "No listings match your filters."
            }
            status.visibility = View.VISIBLE
            retry.visibility = View.GONE
            return
        }

        status.visibility = View.GONE
        filtered.forEach { listing ->
            val card = TextView(requireContext()).apply {
                val price = String.format(Locale.getDefault(), "%.2f", listing.price)
                text = "${listing.title}\nPrice: R$price\n${listing.itemCondition} • ${listing.category}"
                textSize = 16f
                setTextColor(resources.getColor(R.color.text_primary, null))
                setPadding(40, 32, 40, 32)
                setBackgroundResource(R.color.brand_white)
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    findNavController().navigate(
                        R.id.action_browse_to_listingDetails,
                        Bundle().apply { putInt("listingId", listing.listingId) }
                    )
                }
            }
            val spacing = (12 * resources.displayMetrics.density).toInt()
            listingContainer.addView(
                card,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = spacing }
            )
        }
    }

    private fun redirectToVerification() {
        if (!isAdded || isRedirecting) return
        val navController = findNavController()
        if (navController.currentDestination?.id != R.id.browseListingsFragment) return
        isRedirecting = true
        navController.navigate(R.id.emailVerificationFragment)
    }

    private fun redirectToSignIn() {
        if (!isAdded || isRedirecting) return
        val navController = findNavController()
        if (navController.currentDestination?.id != R.id.browseListingsFragment) return
        isRedirecting = true
        navController.navigate(R.id.signInFragment)
    }
}