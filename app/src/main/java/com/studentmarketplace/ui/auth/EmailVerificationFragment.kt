
package com.studentmarketplace.ui.auth

// Android lifecycle and layout imports.
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView

// AndroidX navigation and fragment imports.
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

// App-specific imports.
import com.studentmarketplace.R
import com.studentmarketplace.utils.AppErrorHandler

// Firebase Authentication import.
import com.google.firebase.auth.FirebaseAuth

// Handles email verification and automatically detects successful verification.
class EmailVerificationFragment : Fragment() {

    // Holds the Firebase Authentication instance.
    private lateinit var firebaseAuth: FirebaseAuth

    // Prevents repeated navigation after successful verification.
    private var isNavigatingToHome = false

    // Loads the email verification layout.
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_email_verification,
            container,
            false
        )
    }

    // Initialises Firebase and configures the verification screen.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Connects this screen to Firebase Authentication.
        firebaseAuth = FirebaseAuth.getInstance()

        // Finds the screen elements.
        val emailText = view.findViewById<TextView>(
            R.id.tvVerificationEmail
        )

        val statusText = view.findViewById<TextView>(
            R.id.tvVerificationStatus
        )

        val checkButton = view.findViewById<Button>(
            R.id.btnCheckVerification
        )

        val resendButton = view.findViewById<Button>(
            R.id.btnResendVerification
        )

        val returnButton = view.findViewById<Button>(
            R.id.btnReturnToSignIn
        )

        // Displays the email address awaiting verification.
        emailText.text = firebaseAuth.currentUser?.email
            ?: "Email address unavailable"

        // Shows the initial verification status.
        statusText.text = "Waiting for email verification..."

        // Checks the Firebase verification status when requested.
        checkButton.setOnClickListener {
            statusText.text = "Checking your verification status..."
            refreshVerificationStatus()
        }

        // Sends another verification email when requested.
        resendButton.setOnClickListener {

            // Gets the currently authenticated Firebase user.
            val user = firebaseAuth.currentUser

            // Checks that a user is available.
            if (user == null) {
                AppErrorHandler.showError(
                    view,
                    "Your account session has expired. Please sign in again."
                )
                return@setOnClickListener
            }

            // Prevents unnecessary repeat emails.
            resendButton.isEnabled = false

            // Sends a new Firebase email verification link.
            user.sendEmailVerification()
                .addOnCompleteListener { task ->

                    // Re-enables the resend button.
                    resendButton.isEnabled = true

                    // Displays the result of the email request.
                    if (task.isSuccessful) {
                        AppErrorHandler.showSuccess(
                            view,
                            "Verification email sent. Please check your inbox."
                        )
                    } else {
                        AppErrorHandler.showError(
                            view,
                            task.exception?.localizedMessage
                                ?: "Unable to send verification email."
                        )
                    }
                }
        }

        // Allows the student to sign out and use another account.
        returnButton.setOnClickListener {

            // Clears the current Firebase authentication session.
            firebaseAuth.signOut()

            // Returns the student to the sign-in screen.
            findNavController().navigate(
                R.id.action_emailVerification_to_signIn
            )
        }
    }

    // Refreshes Firebase verification status when the student returns to the app.
    override fun onResume() {
        super.onResume()

        // Checks the latest verification status.
        if (::firebaseAuth.isInitialized) {
            refreshVerificationStatus()
        }
    }

    // Reloads the Firebase user to obtain the latest verification status.
    private fun refreshVerificationStatus() {

        // Avoids repeated navigation after verification succeeds.
        if (isNavigatingToHome) return

        // Gets the current Firebase user.
        val user = firebaseAuth.currentUser

        // Handles a missing authentication session.
        if (user == null) {
            if (isAdded) {
                findNavController().navigate(
                    R.id.action_emailVerification_to_signIn
                )
            }
            return
        }

        // Requests the latest user information from Firebase.
        user.reload().addOnCompleteListener { task ->

            // Ensures that the fragment is still active.
            if (!isAdded || isNavigatingToHome) return@addOnCompleteListener

            // Checks whether the user information was refreshed.
            if (task.isSuccessful) {

                // Gets the refreshed Firebase user.
                val refreshedUser = firebaseAuth.currentUser

                // Checks the verified email status.
                if (refreshedUser?.isEmailVerified == true) {

                    // Prevents duplicate navigation.
                    isNavigatingToHome = true

                    // Shows successful verification feedback.
                    AppErrorHandler.showSuccess(
                        requireView(),
                        "Email verified successfully! Welcome to The Student Marketplace."
                    )

                    // Opens the marketplace after verification.
                    findNavController().navigate(
                        R.id.action_emailVerification_to_home
                    )

                } else {

                    // Keeps the student on the verification screen.
                    view?.findViewById<TextView>(
                        R.id.tvVerificationStatus
                    )?.text = "Your email is not verified yet. Please check your inbox."
                }

            } else {

                // Displays a temporary network or refresh error.
                view?.findViewById<TextView>(
                    R.id.tvVerificationStatus
                )?.text = "Unable to check verification status. Please try again."
            }
        }
    }
}
