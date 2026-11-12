
package com.studentmarketplace.ui.marketplace

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.studentmarketplace.R
import com.studentmarketplace.data.repository.ListingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class CreateListingFragment : Fragment() {

    private lateinit var firebaseAuth: FirebaseAuth

    private var isRedirecting = false

    private var selectedPhotoUri: Uri? = null

    // Opens the phone's photo picker and displays the selected photo.
    private val photoPickerLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->

            selectedPhotoUri = uri

            if (uri != null) {
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

            if (!isAdded || viewLifecycleOwnerLiveData.value == null) {
                return@addOnCompleteListener
            }

            if (!task.isSuccessful) {
                Toast.makeText(
                    requireContext(),
                    "Unable to verify your account. Please sign in again.",
                    Toast.LENGTH_LONG
                ).show()

                firebaseAuth.signOut()
                redirectToSignIn()
                return@addOnCompleteListener
            }

            val refreshedUser = firebaseAuth.currentUser

            if (refreshedUser == null) {
                redirectToSignIn()
                return@addOnCompleteListener
            }

            if (refreshedUser.isEmailVerified) {
                view.visibility = View.VISIBLE
                setupListingForm(view)
            } else {
                redirectToVerification()
            }
        }
    }

    private fun setupListingForm(view: View) {

        val conditions = arrayOf(
            "Brand New",
            "Like New",
            "Good",
            "Fair",
            "For Parts / Repair"
        )

        val categories = arrayOf(
            "Textbooks",
            "Stationery",
            "Electronics",
            "Essentials",
            "Other"
        )

        // Opens the phone's photo picker.
        view.findViewById<View>(R.id.btnAddListingPhoto)
            .setOnClickListener {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                )
            }

        // Sets up the category dropdown.
        val categoryDropdown =
            view.findViewById<AutoCompleteTextView>(R.id.etListingCategory)

        val categoryAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            categories
        )

        categoryDropdown.setAdapter(categoryAdapter)

        categoryDropdown.setOnClickListener {
            categoryDropdown.showDropDown()
        }

        // Sets up the condition dropdown.
        val conditionDropdown =
            view.findViewById<AutoCompleteTextView>(R.id.etListingCondition)

        val conditionAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            conditions
        )

        conditionDropdown.setAdapter(conditionAdapter)

        conditionDropdown.setOnClickListener {
            conditionDropdown.showDropDown()
        }

        // Publishes the listing.
        view.findViewById<View>(R.id.btnSubmitListing)
            .setOnClickListener {

                val title = view.findViewById<EditText>(
                    R.id.etListingTitle
                ).text.toString().trim()

                val description = view.findViewById<EditText>(
                    R.id.etListingDescription
                ).text.toString().trim()

                val priceText = view.findViewById<EditText>(
                    R.id.etListingPrice
                ).text.toString().trim()

                val category = categoryDropdown.text.toString().trim()

                val condition = conditionDropdown.text.toString().trim()

                val moduleCode = view.findViewById<EditText>(
                    R.id.etModuleCode
                ).text.toString().trim()

                val price = priceText.toDoubleOrNull()

                if (
                    title.isBlank() ||
                    description.isBlank() ||
                    category.isBlank() ||
                    condition.isBlank() ||
                    price == null ||
                    price <= 0
                ) {
                    Toast.makeText(
                        requireContext(),
                        "Please complete all required fields with a valid price.",
                        Toast.LENGTH_LONG
                    ).show()

                    return@setOnClickListener
                }

                val context = requireContext()

                val imageBytes = try {
                    selectedPhotoUri?.let { uri ->
                        context.contentResolver.openInputStream(uri)?.use {
                            it.readBytes()
                        }
                    }
                } catch (exception: Exception) {
                    Toast.makeText(
                        context,
                        "Could not read the selected photo.",
                        Toast.LENGTH_LONG
                    ).show()

                    return@setOnClickListener
                }

                val imageMimeType = selectedPhotoUri?.let { uri ->
                    context.contentResolver.getType(uri)
                }

                if (selectedPhotoUri != null &&
                    (imageBytes == null || imageMimeType == null)
                ) {
                    Toast.makeText(
                        context,
                        "Could not access the selected photo. Please select it again.",
                        Toast.LENGTH_LONG
                    ).show()

                    return@setOnClickListener
                }

                val submitButton =
                    view.findViewById<View>(R.id.btnSubmitListing)

                submitButton.isEnabled = false

                viewLifecycleOwner.lifecycleScope.launch {

                    try {
                        withContext(Dispatchers.IO) {

                            ListingRepository.createListing(
                                title = title,
                                description = description,
                                price = price,
                                condition = condition,
                                category = category,
                                moduleCode = moduleCode.ifBlank { null },
                                imageBytes = imageBytes,
                                imageMimeType = imageMimeType
                            )
                        }

                        Toast.makeText(
                            context,
                            "Listing published successfully.",
                            Toast.LENGTH_LONG
                        ).show()

                        findNavController().navigate(
                            R.id.action_createListing_to_browse
                        )

                    } catch (exception: Exception) {

                        Toast.makeText(
                            context,
                            "Could not publish listing: ${
                                exception.message ?: "Please try again."
                            }",
                            Toast.LENGTH_LONG
                        ).show()

                    } finally {
                        submitButton.isEnabled = true
                    }
                }
            }
    }

    private fun redirectToVerification() {
        if (!isAdded || isRedirecting) return

        val navController = findNavController()

        if (navController.currentDestination?.id != R.id.createListingFragment) {
            return
        }

        isRedirecting = true
        navController.navigate(R.id.emailVerificationFragment)
    }

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
