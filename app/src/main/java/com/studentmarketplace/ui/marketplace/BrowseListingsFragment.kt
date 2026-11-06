package com.studentmarketplace.ui.marketplace

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.studentmarketplace.R

// Displays searchable marketplace listings to verified students.
class BrowseListingsFragment : Fragment() {

    // Provides access to Firebase Authentication.
    private lateinit var firebaseAuth: FirebaseAuth

    // Prevents repeated navigation during access checks.
    private var isRedirecting = false

    // Represents one sample marketplace listing.
    private data class Listing(
        val title: String,
        val description: String,
        val category: String,
        val price: Double,
        val condition: String,
        val moduleCode: String
    )

    // Temporary sample listings for testing the filters.
    private val sampleListings = listOf(
        Listing(
            "Introduction to Programming Textbook",
            "Programming fundamentals textbook",
            "Textbooks and Study Material",
            250.0,
            "Good",
            "OPSC6312"
        ),
        Listing(
            "Scientific Calculator",
            "Casio scientific calculator",
            "Electronics and Accessories",
            180.0,
            "Like New",
            ""
        ),
        Listing(
            "Student Stationery Pack",
            "Pens, notebooks and highlighters",
            "Stationery and Course Equipment",
            90.0,
            "Brand New",
            ""
        ),
        Listing(
            "Desk Lamp",
            "Study lamp for a residence room",
            "Dorm and Daily Essentials",
            150.0,
            "Good",
            ""
        ),
        Listing(
            "USB Headphones",
            "Wired headphones for online lectures",
            "Other Student Items",
            120.0,
            "Fair",
            ""
        )
    )

    // Stores the currently selected listing.
    private var selectedListing: Listing? = null

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

    // Checks account verification before displaying listings.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialises Firebase Authentication.
        firebaseAuth = FirebaseAuth.getInstance()

        // Hides the screen until verification is confirmed.
        view.visibility = View.INVISIBLE

        // Retrieves the signed-in user.
        val user = firebaseAuth.currentUser

        // Redirects users without an active session.
        if (user == null) {
            redirectToSignIn()
            return
        }

        // Refreshes the latest account verification status.
        user.reload().addOnCompleteListener { task ->

            // Stops if the fragment is no longer attached.
            if (!isAdded || viewLifecycleOwnerLiveData.value == null) {
                return@addOnCompleteListener
            }

            // Blocks access if Firebase cannot refresh the account.
            if (!task.isSuccessful) {
                Toast.makeText(
                    requireContext(),
                    "Unable to verify your account. Please sign in again.",
                    Toast.LENGTH_LONG
                ).show()

                // Signs out when account status is uncertain.
                firebaseAuth.signOut()
                redirectToSignIn()
                return@addOnCompleteListener
            }

            // Retrieves the refreshed Firebase user.
            val refreshedUser = firebaseAuth.currentUser

            // Blocks access if the account no longer exists.
            if (refreshedUser == null) {
                redirectToSignIn()
                return@addOnCompleteListener
            }

            // Enables browsing only for verified students.
            if (refreshedUser.isEmailVerified) {
                view.visibility = View.VISIBLE
                setupSearchAndFilters(view)
            } else {
                // Keeps the session active for email verification.
                redirectToVerification()
            }
        }
    }

    // Connects search, category, price and condition filters.
    private fun setupSearchAndFilters(view: View) {

        // Retrieves the search and filter controls.
        val searchField = view.findViewById<EditText>(R.id.etSearchListings)
        val categoryField =
            view.findViewById<AutoCompleteTextView>(R.id.etFilterCategory)
        val conditionField =
            view.findViewById<AutoCompleteTextView>(R.id.etFilterCondition)
        val minPriceField = view.findViewById<EditText>(R.id.etMinPrice)
        val maxPriceField = view.findViewById<EditText>(R.id.etMaxPrice)
        val resultsLabel = view.findViewById<TextView>(R.id.tvListingResults)
        val listingLabel = view.findViewById<TextView>(R.id.tvSampleListing)
        val detailsButton = view.findViewById<Button>(R.id.btnViewListingDetails)
        val clearButton = view.findViewById<Button>(R.id.btnClearFilters)

        // Defines the available marketplace categories.
        val categories = listOf(
            "All Categories",
            "Textbooks and Study Material",
            "Electronics and Accessories",
            "Stationery and Course Equipment",
            "Dorm and Daily Essentials",
            "Other Student Items"
        )

        // Defines the available item conditions.
        val conditions = listOf(
            "All Conditions",
            "Brand New",
            "Like New",
            "Good",
            "Fair",
            "For Parts / Repair"
        )

        // Connects categories to their dropdown.
        categoryField.setAdapter(
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                categories
            )
        )

        // Connects conditions to their dropdown.
        conditionField.setAdapter(
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                conditions
            )
        )

        // Selects the default filter options.
        categoryField.setText(categories[0], false)
        conditionField.setText(conditions[0], false)

        // Applies the selected filters to sample listings.
        fun applyFilters() {

            // Reads the user's search phrase.
            val query = searchField.text.toString().trim()

            // Reads the selected category and condition.
            val category = categoryField.text.toString()
            val condition = conditionField.text.toString()

            // Reads optional price limits.
            val minPrice = minPriceField.text.toString().toDoubleOrNull()
            val maxPrice = maxPriceField.text.toString().toDoubleOrNull()

            // Filters listings using all selected criteria.
            val filteredListings = sampleListings.filter { listing ->

                val matchesSearch =
                    query.isBlank() ||
                            listing.title.contains(query, ignoreCase = true) ||
                            listing.description.contains(query, ignoreCase = true) ||
                            listing.moduleCode.contains(query, ignoreCase = true)

                val matchesCategory =
                    category == "All Categories" ||
                            listing.category == category

                val matchesCondition =
                    condition == "All Conditions" ||
                            listing.condition == condition

                val matchesMinimum =
                    minPrice == null || listing.price >= minPrice

                val matchesMaximum =
                    maxPrice == null || listing.price <= maxPrice

                matchesSearch &&
                        matchesCategory &&
                        matchesCondition &&
                        matchesMinimum &&
                        matchesMaximum
            }

            // Updates the number of matching listings.
            resultsLabel.text =
                "Available Items (${filteredListings.size})"

            // Displays the first matching sample listing.
            val firstListing = filteredListings.firstOrNull()
            selectedListing = firstListing

            if (firstListing != null) {
                listingLabel.text =
                    "${firstListing.title}\n" +
                            "Price: R${firstListing.price}\n" +
                            "Condition: ${firstListing.condition}\n" +
                            "Category: ${firstListing.category}\n" +
                            "Module: ${firstListing.moduleCode.ifBlank { "Not applicable" }}"

                detailsButton.visibility = View.VISIBLE
            } else {
                listingLabel.text = "No listings match your filters."
                detailsButton.visibility = View.GONE
            }
        }

        // Refreshes results whenever the search phrase changes.
        searchField.doAfterTextChanged {
            applyFilters()
        }

        // Refreshes results when a category is selected.
        categoryField.setOnItemClickListener { _, _, _, _ ->
            applyFilters()
        }

        // Refreshes results when a condition is selected.
        conditionField.setOnItemClickListener { _, _, _, _ ->
            applyFilters()
        }

        // Refreshes results when a price limit changes.
        minPriceField.doAfterTextChanged {
            applyFilters()
        }

        maxPriceField.doAfterTextChanged {
            applyFilters()
        }

        // Clears all filters and restores the sample listings.
        clearButton.setOnClickListener {
            searchField.text.clear()
            minPriceField.text.clear()
            maxPriceField.text.clear()
            categoryField.setText(categories[0], false)
            conditionField.setText(conditions[0], false)
            applyFilters()
        }

        // Opens details for the selected sample listing.
        detailsButton.setOnClickListener {
            findNavController().navigate(
                R.id.action_browse_to_listingDetails
            )
        }

        // Displays all sample listings initially.
        applyFilters()
    }

    // Redirects unverified students to email verification.
    private fun redirectToVerification() {
        if (!isAdded || isRedirecting) return

        val navController = findNavController()

        if (navController.currentDestination?.id != R.id.browseListingsFragment) {
            return
        }

        isRedirecting = true
        navController.navigate(R.id.emailVerificationFragment)
    }

    // Redirects unauthenticated students to sign-in.
    private fun redirectToSignIn() {
        if (!isAdded || isRedirecting) return

        val navController = findNavController()

        if (navController.currentDestination?.id != R.id.browseListingsFragment) {
            return
        }

        isRedirecting = true
        navController.navigate(R.id.signInFragment)
    }
}
