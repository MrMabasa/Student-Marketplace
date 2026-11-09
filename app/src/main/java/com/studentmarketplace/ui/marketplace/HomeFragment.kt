
package com.studentmarketplace.ui.marketplace

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.studentmarketplace.R

// Displays the marketplace dashboard only to verified students.
class HomeFragment : Fragment() {

    // Provides access to Firebase Authentication.
    private lateinit var firebaseAuth: FirebaseAuth

    // Prevents multiple authentication redirects.
    private var isRedirecting = false

    // Loads the Home screen XML layout.
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_home,
            container,
            false
        )
    }

    // Checks email verification before enabling Home screen actions.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialises Firebase Authentication.
        firebaseAuth = FirebaseAuth.getInstance()

        // Hides Home until the account check is complete.
        view.visibility = View.INVISIBLE

        // Retrieves the currently signed-in Firebase user.
        val user = firebaseAuth.currentUser

        // Redirects users without a signed-in session.
        if (user == null) {
            redirectToSignIn()
            return
        }

        // Refreshes the user's latest verification status from Firebase.
        user.reload().addOnCompleteListener { task ->

            // Stops if the fragment is no longer attached.
            if (!isAdded || viewLifecycleOwner.lifecycle.currentState
                == androidx.lifecycle.Lifecycle.State.DESTROYED
            ) return@addOnCompleteListener

            // Blocks access if Firebase could not refresh the account.
            if (!task.isSuccessful) {
                Toast.makeText(
                    requireContext(),
                    "Unable to verify your account. Please sign in again.",
                    Toast.LENGTH_LONG
                ).show()

                // Signs out to prevent access with an uncertain session.
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

            // Allows Home access only after Firebase confirms verification.
            if (refreshedUser.isEmailVerified) {
                view.visibility = View.VISIBLE
                connectHomeButtons(view)
            } else {
                // Keeps the session active so the student can verify their email.
                redirectToVerification()
            }
        }
    }

    // Connects marketplace actions after successful verification.
    private fun connectHomeButtons(view: View) {

        // Opens the available marketplace listings.
        view.findViewById<View>(R.id.btnBrowseListings).setOnClickListener {
            findNavController().navigate(
                R.id.action_home_to_browse
            )
        }

        // Opens the student's saved favourite listings.
        view.findViewById<View>(R.id.btnFavourites).setOnClickListener {
            findNavController().navigate(
                R.id.favouritesFragment
            )
        }

        // Opens the form for creating a marketplace listing.
        view.findViewById<View>(R.id.btnCreateListing).setOnClickListener {
            findNavController().navigate(
                R.id.action_home_to_createListing
            )
        }
    }

    // Redirects an unverified account to the verification screen.
    private fun redirectToVerification() {
        if (!isAdded || isRedirecting) return

        val navController = findNavController()

        // Confirms Home is still the current destination.
        if (navController.currentDestination?.id != R.id.homeFragment) return

        isRedirecting = true

        // Opens the email verification screen.
        navController.navigate(
            R.id.emailVerificationFragment
        )
    }

    // Redirects an unauthenticated account to sign-in.
    private fun redirectToSignIn() {
        if (!isAdded || isRedirecting) return

        val navController = findNavController()

        // Confirms Home is still the current destination.
        if (navController.currentDestination?.id != R.id.homeFragment) return

        isRedirecting = true

        // Returns the user to the sign-in screen.
        navController.navigate(
            R.id.signInFragment
        )
    }
}
