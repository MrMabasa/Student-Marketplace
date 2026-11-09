package com.studentmarketplace.ui.marketplace

import android.widget.ImageView
import android.os.Bundle
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.studentmarketplace.R

// Displays the listing form only to verified students.
class CreateListingFragment : Fragment() {

    // Provides access to Firebase Authentication.
    private lateinit var firebaseAuth: FirebaseAuth

    // Prevents repeated redirects during verification checks.
    private var isRedirecting = false

    // Stores the photo selected by the student.
    private var selectedPhotoUri: Uri? = null

    // Opens the phone's photo picker.
    private val photoPickerLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            selectedPhotoUri = uri

            if (uri != null) {
                // Displays the selected photo in the preview.
                view?.findViewById<ImageView>(R.id.ivListingPhotoPreview)?.apply {
                    setImageURI(uri)
                    visibility = View.VISIBLE
                }
                Toast.makeText(
                    requireContext(),
                    "Photo selected successfully.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    requireContext(),
                    "No photo selected.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

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

    // Checks account verification before enabling the listing form.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialises Firebase Authentication.
        firebaseAuth = FirebaseAuth.getInstance()

        // Hides the form until verification is confirmed.
        view.visibility = View.INVISIBLE

        // Retrieves the currently signed-in user.
        val user = firebaseAuth.currentUser

        // Redirects users without an active session.
        if (user == null) {
            redirectToSignIn()
            return
        }

        // Refreshes the account's latest verification status.
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

                // Signs out when account status cannot be confirmed.
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

            // Enables the form only for verified students.
            if (refreshedUser.isEmailVerified) {
                view.visibility = View.VISIBLE
                setupListingForm(view)
            } else {
                // Keeps the session active for email verification.
                redirectToVerification()
            }
        }
    }

    // Sets up the condition dropdown and submission navigation.
    private fun setupListingForm(view: View) {
        // Defines the standard item conditions available to sellers.
        val conditions = arrayOf(
            "Brand New",
            "Like New",
            "Good",
            "Fair",
            "For Parts / Repair"
        )
        // Defines the approved marketplace categories.
        val categories = arrayOf(
            "Textbooks",
            "Stationery",
            "Electronics",
            "Essentials",
            "Other"
        )
        // Opens the phone's photo picker when the button is tapped.
        view.findViewById<View>(R.id.btnAddListingPhoto).setOnClickListener {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly
                )
            )
        }

// Connects the category choices to the dropdown field.
        val categoryDropdown =
            view.findViewById<AutoCompleteTextView>(R.id.etListingCategory)
// Creates the category dropdown adapter.
        val categoryAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            categories
        )
// Displays the available categories when tapped.
        categoryDropdown.setAdapter(categoryAdapter)
        categoryDropdown.setOnClickListener {
            categoryDropdown.showDropDown()
        }
        // Connects the condition choices to the dropdown field.
        val conditionDropdown =
            view.findViewById<AutoCompleteTextView>(R.id.etListingCondition)

        // Creates the dropdown list adapter.
        val conditionAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            conditions
        )

        // Displays the available conditions when the field is tapped.
        conditionDropdown.setAdapter(conditionAdapter)
        conditionDropdown.setOnClickListener {
            conditionDropdown.showDropDown()
        }

        // Preserves the existing temporary submission navigation.
        view.findViewById<View>(R.id.btnSubmitListing).setOnClickListener {
            findNavController().navigate(R.id.action_createListing_to_browse)
        }
    }

    // Redirects unverified students to email verification.
    private fun redirectToVerification() {
        if (!isAdded || isRedirecting) return

        val navController = findNavController()

        if (navController.currentDestination?.id != R.id.createListingFragment) {
            return
        }

        isRedirecting = true
        navController.navigate(R.id.emailVerificationFragment)
    }

    // Redirects unauthenticated students to sign-in.
    private fun redirectToSignIn() {
        if (!isAdded || isRedirecting) return

        val navController = findNavController()

        if (navController.currentDestination?.id != R.id.createListingFragment) {
            return
        }

        isRedirecting = true
        navController.navigate(R.id.signInFragment)
    }
}