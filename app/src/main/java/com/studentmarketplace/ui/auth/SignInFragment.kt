
package com.studentmarketplace.ui.auth

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.studentmarketplace.R
import com.studentmarketplace.utils.AppErrorHandler
import com.studentmarketplace.notifications.MarketplaceFirebaseMessagingService

// Handles student sign-in and prevents unverified accounts from entering Home.
class SignInFragment : Fragment() {

    // Provides access to Firebase Authentication.
    private lateinit var firebaseAuth: FirebaseAuth

    // Inflates the sign-in screen layout.
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_sign_in, container, false)
    }

    // Connects the sign-in controls and handles authentication.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialises Firebase Authentication.
        firebaseAuth = FirebaseAuth.getInstance()

        // Gets the email and password input fields.
        val email = view.findViewById<EditText>(R.id.etSignInEmail)
        val password = view.findViewById<EditText>(R.id.etSignInPassword)

        // Handles the student's sign-in button.
        view.findViewById<View>(R.id.btnSignIn).setOnClickListener {

            // Reads the entered credentials.
            val emailValue = email.text.toString().trim()
            val passwordValue = password.text.toString()

            // Validates the email address.
            if (emailValue.isEmpty() ||
                !Patterns.EMAIL_ADDRESS.matcher(emailValue).matches()
            ) {
                AppErrorHandler.showFieldError(
                    view,
                    email,
                    "Please enter a valid email address."
                )
                return@setOnClickListener
            }

            // Validates that a password was entered.
            if (passwordValue.isEmpty()) {
                AppErrorHandler.showFieldError(
                    view,
                    password,
                    "Please enter your password."
                )
                return@setOnClickListener
            }

            // Clears previous validation errors.
            email.error = null
            password.error = null

            // Authenticates the student with Firebase.
            firebaseAuth.signInWithEmailAndPassword(emailValue, passwordValue)
                .addOnCompleteListener { signInTask ->

                    // Checks whether Firebase accepted the credentials.
                    if (signInTask.isSuccessful) {

                        // Retrieves the authenticated Firebase user.
                        val firebaseUser = firebaseAuth.currentUser

                        if (firebaseUser == null) {
                            AppErrorHandler.showError(
                                view,
                                "Sign-in could not be completed. Please try again."
                            )
                            return@addOnCompleteListener
                        }

                        // Refreshes the user's latest verification status from Firebase.
                        firebaseUser.reload().addOnCompleteListener { reloadTask ->

                            // Stops UI operations if this fragment is no longer active.
                            if (!isAdded) return@addOnCompleteListener

                            // Checks whether Firebase could refresh the account.
                            if (!reloadTask.isSuccessful) {
                                AppErrorHandler.showError(
                                    view,
                                    "Could not check your verification status. Please try again."
                                )
                                return@addOnCompleteListener
                            }

                            // Retrieves the refreshed user information.
                            val refreshedUser: FirebaseUser? = firebaseAuth.currentUser

                            if (refreshedUser == null) {
                                AppErrorHandler.showError(
                                    view,
                                    "Your sign-in session could not be verified. Please try again."
                                )
                                return@addOnCompleteListener
                            }

                            // Allows access to Home only when the email is verified.
                            if (refreshedUser.isEmailVerified) {
                                // Register this device for marketplace notifications.
                                MarketplaceFirebaseMessagingService.refreshAndRegisterToken(
                                    requireContext().applicationContext
                                )

                                AppErrorHandler.showSuccess(
                                    view,
                                    "Welcome back! Your email is verified."
                                )

                                // Navigates verified students to the marketplace.
                                findNavController().navigate(
                                    R.id.action_signIn_to_home
                                )

                            } else {

                                // Keeps the authenticated session for email verification.
                                AppErrorHandler.showError(
                                    view,
                                    "Please verify your email before accessing the marketplace."
                                )

                                // Sends unverified students to the verification screen.
                                findNavController().navigate(
                                    R.id.action_signIn_to_emailVerification
                                )
                            }
                        }

                    } else {

                        // Displays Firebase's sign-in error.
                        AppErrorHandler.showError(
                            view,
                            signInTask.exception?.localizedMessage
                                ?: "Sign-in failed. Please check your details."
                        )
                    }
                }
        }
    }
}
