
package com.studentmarketplace.ui.auth

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.studentmarketplace.R
import com.studentmarketplace.data.remote.ListingApiClient
import com.studentmarketplace.data.remote.RegisterRequest
import com.studentmarketplace.utils.AppErrorHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class RegisterFragment : Fragment() {

    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_register,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()

        val institutions = arrayOf(
            "University of South Africa (UNISA)",
            "Rosebank International",
            "University of Pretoria (UP)",
            "University of Johannesburg (UJ)",
            "University of the Witwatersrand (Wits)",
            "University of Cape Town (UCT)",
            "Eduvos",
            "Stellenbosch University",
            "North-West University (NWU)",
            "University of Limpopo (UL)",
            "University of Venda (UNIVEN)",
            "University of the Free State (UFS)",
            "Tshwane University of Technology (TUT)",
            "Vaal University of Technology (VUT)",
            "Durban University of Technology (DUT)",
            "Cape Peninsula University of Technology (CPUT)",
            "Richfield Graduate Institute of technology",
            "Other / Not Listed"
        )

        val institutionDropdown =
            view.findViewById<AutoCompleteTextView>(R.id.etInstitution)

        val customInstitution =
            view.findViewById<EditText>(R.id.etCustomInstitution)

        val fullName =
            view.findViewById<EditText>(R.id.etRegisterName)

        val email =
            view.findViewById<EditText>(R.id.etRegisterEmail)

        val studentNumber =
            view.findViewById<EditText>(R.id.etStudentNumber)

        val password =
            view.findViewById<EditText>(R.id.etRegisterPassword)

        val confirmPassword =
            view.findViewById<EditText>(R.id.etConfirmPassword)

        val registerButton =
            view.findViewById<View>(R.id.btnRegister)

        val institutionAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            institutions
        )

        institutionDropdown.setAdapter(institutionAdapter)

        institutionDropdown.setOnClickListener {
            institutionDropdown.showDropDown()
        }

        institutionDropdown.setOnItemClickListener { _, _, position, _ ->

            if (institutions[position] == "Other / Not Listed") {
                customInstitution.visibility = View.VISIBLE
            } else {
                customInstitution.visibility = View.GONE
                customInstitution.text.clear()
            }

            institutionDropdown.error = null
        }

        registerButton.setOnClickListener {

            val fullNameValue = fullName.text.toString().trim()
            val emailValue = email.text.toString().trim()
            val studentNumberValue = studentNumber.text.toString().trim()
            val selectedInstitution =
                institutionDropdown.text.toString().trim()

            // Validate full name.
            if (fullNameValue.isEmpty()) {
                AppErrorHandler.showFieldError(
                    view,
                    fullName,
                    "Please enter your full name."
                )
                return@setOnClickListener
            }

            // Validate email.
            if (
                emailValue.isEmpty() ||
                !Patterns.EMAIL_ADDRESS.matcher(emailValue).matches()
            ) {
                AppErrorHandler.showFieldError(
                    view,
                    email,
                    "Please enter a valid email address."
                )
                return@setOnClickListener
            }

            // Validate student number.
            if (studentNumberValue.isEmpty()) {
                AppErrorHandler.showFieldError(
                    view,
                    studentNumber,
                    "Please enter your student number."
                )
                return@setOnClickListener
            }

            // Validate institution.
            if (selectedInstitution.isEmpty()) {
                AppErrorHandler.showFieldError(
                    view,
                    institutionDropdown,
                    "Please select your institution."
                )
                return@setOnClickListener
            }

            if (
                selectedInstitution == "Other / Not Listed" &&
                customInstitution.text.toString().trim().isEmpty()
            ) {
                AppErrorHandler.showFieldError(
                    view,
                    customInstitution,
                    "Please enter your institution name."
                )
                return@setOnClickListener
            }

            val institutionValue =
                if (selectedInstitution == "Other / Not Listed") {
                    customInstitution.text.toString().trim()
                } else {
                    selectedInstitution
                }

            val passwordValue = password.text.toString()

            // Validate password length.
            if (passwordValue.length < 8) {
                AppErrorHandler.showFieldError(
                    view,
                    password,
                    "Password must contain at least 8 characters."
                )
                return@setOnClickListener
            }

            // Validate uppercase character.
            if (!passwordValue.any { it.isUpperCase() }) {
                AppErrorHandler.showFieldError(
                    view,
                    password,
                    "Password must contain an uppercase letter."
                )
                return@setOnClickListener
            }

            // Validate lowercase character.
            if (!passwordValue.any { it.isLowerCase() }) {
                AppErrorHandler.showFieldError(
                    view,
                    password,
                    "Password must contain a lowercase letter."
                )
                return@setOnClickListener
            }

            // Validate number.
            if (!passwordValue.any { it.isDigit() }) {
                AppErrorHandler.showFieldError(
                    view,
                    password,
                    "Password must contain a number."
                )
                return@setOnClickListener
            }

            // Validate special character.
            if (!passwordValue.any { !it.isLetterOrDigit() }) {
                AppErrorHandler.showFieldError(
                    view,
                    password,
                    "Password must contain a special character."
                )
                return@setOnClickListener
            }

            // Validate password confirmation.
            if (confirmPassword.text.toString().isEmpty()) {
                AppErrorHandler.showFieldError(
                    view,
                    confirmPassword,
                    "Please confirm your password."
                )
                return@setOnClickListener
            }

            if (passwordValue != confirmPassword.text.toString()) {
                AppErrorHandler.showFieldError(
                    view,
                    confirmPassword,
                    "Your passwords do not match."
                )
                return@setOnClickListener
            }

            // Clear validation errors.
            fullName.error = null
            email.error = null
            studentNumber.error = null
            institutionDropdown.error = null
            customInstitution.error = null
            password.error = null
            confirmPassword.error = null

            registerButton.isEnabled = false

            // First try creating a new Firebase account.
            firebaseAuth.createUserWithEmailAndPassword(
                emailValue,
                passwordValue
            ).addOnCompleteListener createListener@{ task ->

                if (task.isSuccessful) {

                    val firebaseUser = firebaseAuth.currentUser

                    if (firebaseUser == null) {
                        registerButton.isEnabled = true

                        AppErrorHandler.showError(
                            view,
                            "Your Firebase account was created, but could not be accessed. Please try signing in."
                        )
                        return@createListener
                    }

                    completeMarketplaceRegistration(
                        view,
                        firebaseUser,
                        fullNameValue,
                        studentNumberValue,
                        institutionValue,
                        registerButton
                    )

                } else if (
                    task.exception is FirebaseAuthUserCollisionException
                ) {

                    // Existing Firebase account: sign in instead.
                    firebaseAuth.signInWithEmailAndPassword(
                        emailValue,
                        passwordValue
                    ).addOnCompleteListener signInListener@{ signInTask ->

                        if (!signInTask.isSuccessful) {
                            registerButton.isEnabled = true

                            AppErrorHandler.showError(
                                view,
                                "This email is already registered. Please enter the correct password to complete your marketplace registration."
                            )
                            return@signInListener
                        }

                        val existingFirebaseUser = firebaseAuth.currentUser

                        if (existingFirebaseUser == null) {
                            registerButton.isEnabled = true

                            AppErrorHandler.showError(
                                view,
                                "Could not access your existing account. Please try again."
                            )
                            return@signInListener
                        }

                        completeMarketplaceRegistration(
                            view,
                            existingFirebaseUser,
                            fullNameValue,
                            studentNumberValue,
                            institutionValue,
                            registerButton
                        )
                    }

                } else {

                    registerButton.isEnabled = true

                    AppErrorHandler.showError(
                        view,
                        task.exception?.localizedMessage
                            ?: "Registration failed. Please try again."
                    )
                }
            }
        }
    }

    private fun completeMarketplaceRegistration(
        view: View,
        firebaseUser: FirebaseUser,
        fullName: String,
        studentNumber: String,
        institution: String,
        registerButton: View
    ) {

        // Update Firebase display name.
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(fullName)
            .build()

        firebaseUser.updateProfile(profileUpdates)
            .addOnCompleteListener profileListener@{ profileTask ->

                if (!profileTask.isSuccessful) {
                    registerButton.isEnabled = true

                    AppErrorHandler.showError(
                        view,
                        "Profile setup failed. Please try again."
                    )
                    return@profileListener
                }

                // Refresh the Firebase authentication token.
                firebaseUser.getIdToken(true)
                    .addOnSuccessListener {

                        viewLifecycleOwner.lifecycleScope.launch {

                            try {

                                // Register the user in the MySQL marketplace database.
                                withContext(Dispatchers.IO) {

                                    ListingApiClient.service.registerUser(
                                        RegisterRequest(
                                            fullName = fullName,
                                            studentNumber = studentNumber,
                                            institution = institution
                                        )
                                    )
                                }

                                // Send verification email if not already verified.
                                if (!firebaseUser.isEmailVerified) {

                                    firebaseUser.sendEmailVerification()
                                        .addOnCompleteListener verificationListener@{ verificationTask ->

                                            registerButton.isEnabled = true

                                            if (verificationTask.isSuccessful) {
                                                AppErrorHandler.showSuccess(
                                                    view,
                                                    "Marketplace account created. Please check your email to verify your account."
                                                )
                                            } else {
                                                AppErrorHandler.showError(
                                                    view,
                                                    "Marketplace account created, but the verification email could not be sent. You can resend it on the next screen."
                                                )
                                            }

                                            findNavController().navigate(
                                                R.id.action_register_to_emailVerification
                                            )
                                        }

                                } else {

                                    registerButton.isEnabled = true

                                    AppErrorHandler.showSuccess(
                                        view,
                                        "Your marketplace account is ready."
                                    )

                                    findNavController().navigate(
                                        R.id.action_register_to_emailVerification
                                    )
                                }

                            } catch (error: Exception) {

                                registerButton.isEnabled = true

                                val message = if (error is HttpException) {
                                    error.response()
                                        ?.errorBody()
                                        ?.string()
                                        ?: "Marketplace registration failed."
                                } else {
                                    error.localizedMessage
                                        ?: "Could not connect to the marketplace server."
                                }

                                AppErrorHandler.showError(
                                    view,
                                    "Marketplace registration failed: $message"
                                )
                            }
                        }
                    }
                    .addOnFailureListener { error ->

                        registerButton.isEnabled = true

                        AppErrorHandler.showError(
                            view,
                            "Authentication failed: ${error.localizedMessage}"
                        )
                    }
            }
    }
}
