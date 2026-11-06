
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
import androidx.navigation.fragment.findNavController
import com.studentmarketplace.R
import com.studentmarketplace.utils.AppErrorHandler
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest


// Displays the student registration screen and validates registration details.
class RegisterFragment : Fragment() {

    // Provides the Firebase Authentication instance for registration.
    private lateinit var firebaseAuth: FirebaseAuth
    // Loads the registration XML layout.
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

    // Sets up institution selection and registration validation.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Connects this registration screen to Firebase Authentication.
        firebaseAuth = FirebaseAuth.getInstance()

        // Lists available South African institutions.
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
            "Richfield Graduate Institute of technology",
            "Other / Not Listed"
        )

        // Finds the institution dropdown and custom institution field.
        val institutionDropdown =
            view.findViewById<AutoCompleteTextView>(R.id.etInstitution)

        val customInstitution =
            view.findViewById<EditText>(R.id.etCustomInstitution)

        // Finds the registration input fields.
        val fullName = view.findViewById<EditText>(R.id.etRegisterName)
        val email = view.findViewById<EditText>(R.id.etRegisterEmail)
        val studentNumber = view.findViewById<EditText>(R.id.etStudentNumber)
        val password = view.findViewById<EditText>(R.id.etRegisterPassword)
        val confirmPassword = view.findViewById<EditText>(R.id.etConfirmPassword)

        // Connects the available institutions to the dropdown.
        val institutionAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            institutions
        )

        institutionDropdown.setAdapter(institutionAdapter)

        // Opens the institution dropdown when tapped.
        institutionDropdown.setOnClickListener {
            institutionDropdown.showDropDown()
        }

        // Shows the custom field only when Other / Not Listed is selected.
        institutionDropdown.setOnItemClickListener { _, _, position, _ ->
            if (institutions[position] == "Other / Not Listed") {
                customInstitution.visibility = View.VISIBLE
            } else {
                customInstitution.visibility = View.GONE
                customInstitution.text.clear()
            }

            // Clears any previous institution selection error.
            institutionDropdown.error = null
        }

        // Validates the form when the registration button is pressed.
        view.findViewById<View>(R.id.btnRegister).setOnClickListener {

            // Checks that the student's full name is provided.
            if (fullName.text.toString().trim().isEmpty()) {
                AppErrorHandler.showFieldError(
                    view, fullName, "Please enter your full name."
                )
                return@setOnClickListener
            }

            // Checks that the email address has a valid format.
            val emailValue = email.text.toString().trim()

            if (emailValue.isEmpty() ||
                !Patterns.EMAIL_ADDRESS.matcher(emailValue).matches()
            ) {
                AppErrorHandler.showFieldError(
                    view, email, "Please enter a valid email address."
                )
                return@setOnClickListener
            }

            // Checks that the student number is provided.
            if (studentNumber.text.toString().trim().isEmpty()) {
                AppErrorHandler.showFieldError(
                    view, studentNumber, "Please enter your student number."
                )
                return@setOnClickListener
            }

            // Checks that an institution has been selected.
            val selectedInstitution = institutionDropdown.text.toString().trim()

            if (selectedInstitution.isEmpty()) {
                AppErrorHandler.showFieldError(
                    view, institutionDropdown, "Please select your institution."
                )
                return@setOnClickListener
            }

            // Checks that a custom institution name is provided when required.
            if (selectedInstitution == "Other / Not Listed" &&
                customInstitution.text.toString().trim().isEmpty()
            ) {
                AppErrorHandler.showFieldError(
                    view, customInstitution, "Please enter your institution name."
                )
                return@setOnClickListener
            }

            // Reads the password entered by the student.
            val passwordValue = password.text.toString()

            // Enforces the minimum password length.
            if (passwordValue.length < 8) {
                AppErrorHandler.showFieldError(
                    view, password, "Password must contain at least 8 characters."
                )
                return@setOnClickListener
            }

            // Requires at least one uppercase letter.
            if (!passwordValue.any { it.isUpperCase() }) {
                AppErrorHandler.showFieldError(
                    view, password, "Password must contain an uppercase letter."
                )
                return@setOnClickListener
            }

            // Requires at least one lowercase letter.
            if (!passwordValue.any { it.isLowerCase() }) {
                AppErrorHandler.showFieldError(
                    view, password, "Password must contain a lowercase letter."
                )
                return@setOnClickListener
            }

            // Requires at least one number.
            if (!passwordValue.any { it.isDigit() }) {
                AppErrorHandler.showFieldError(
                    view, password, "Password must contain a number."
                )
                return@setOnClickListener
            }

            // Requires at least one special character.
            if (!passwordValue.any { !it.isLetterOrDigit() }) {
                AppErrorHandler.showFieldError(
                    view, password, "Password must contain a special character."
                )
                return@setOnClickListener
            }

            // Checks that the confirmation password is provided.
            if (confirmPassword.text.toString().isEmpty()) {
                AppErrorHandler.showFieldError(
                    view, confirmPassword, "Please confirm your password."
                )
                return@setOnClickListener
            }

            // Checks that both password entries match.
            if (passwordValue != confirmPassword.text.toString()) {
                AppErrorHandler.showFieldError(
                    view, confirmPassword, "Your passwords do not match."
                )
                return@setOnClickListener
            }

            // Clears field errors after successful validation.
            fullName.error = null
            email.error = null
            studentNumber.error = null
            institutionDropdown.error = null
            customInstitution.error = null
            password.error = null
            confirmPassword.error = null

            // Creates a new student account using Firebase Authentication.
            firebaseAuth.createUserWithEmailAndPassword(emailValue, passwordValue)
                .addOnCompleteListener(requireActivity()) { task ->

                    // Checks whether Firebase successfully created the account.
                    if (task.isSuccessful) {

                        // Gets the newly created Firebase user.
                        val firebaseUser = firebaseAuth.currentUser

                        // Sets the student's full name as their Firebase display name.
                        val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                            .setDisplayName(fullName.text.toString().trim())
                            .build()

                        // Saves the student's display name to their Firebase profile.
                        firebaseUser?.updateProfile(profileUpdates)
                            ?.addOnCompleteListener { profileTask ->

                                // Confirms that the profile update succeeded.
                                if (profileTask.isSuccessful) {

                                    // Sends an email verification link to the newly registered student.
                                    firebaseUser.sendEmailVerification()
                                        .addOnCompleteListener { verificationTask ->

                                            // Checks whether Firebase successfully sent the verification email.
                                            if (verificationTask.isSuccessful) {

                                                // Confirms that the verification email was sent.
                                                AppErrorHandler.showSuccess(
                                                    view,
                                                    "Account created! Please check your email to verify your account."
                                                )

                                            } else {

                                                // Explains that the account exists but email delivery needs retrying.
                                                AppErrorHandler.showError(
                                                    view,
                                                    "Your account was created, but the verification email could not be sent. You can resend it on the next screen."
                                                )
                                            }

                                            // Opens the verification screen whether email delivery succeeded or failed.
                                            findNavController().navigate(
                                                R.id.action_register_to_emailVerification
                                            )
                                        }

                                } else {

                                    // Reports a profile update problem.
                                    AppErrorHandler.showError(
                                        view, "Account created, but profile setup failed. Please sign in again."
                                    )
                                }
                            }

                    } else {

                        // Displays Firebase's registration error.
                        AppErrorHandler.showError(
                            view,
                            task.exception?.localizedMessage
                                ?: "Registration failed. Please try again."
                        )
                    }
                }
        }
    }
}
