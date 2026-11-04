package com.studentmarketplace.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.studentmarketplace.R

// Displays the first screen when the Student Marketplace app opens.
class WelcomeFragment : Fragment() {

    // Connects this Fragment to its XML layout.
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_welcome,
            container,
            false
        )
    }

    // Sets up the buttons after the screen has been created.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Opens the sign-in screen when the user taps Sign In.
        view.findViewById<View>(R.id.btnWelcomeSignIn).setOnClickListener {
            findNavController().navigate(R.id.action_welcome_to_signIn)
        }

        // Opens the registration screen when the user taps Register.
        view.findViewById<View>(R.id.btnWelcomeRegister).setOnClickListener {
            findNavController().navigate(R.id.action_welcome_to_register)
        }
    }
}