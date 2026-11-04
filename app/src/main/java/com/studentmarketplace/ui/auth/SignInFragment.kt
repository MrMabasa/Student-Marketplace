package com.studentmarketplace.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.studentmarketplace.R

// Displays the student sign-in screen.
class SignInFragment : Fragment() {

    // Loads the sign-in XML layout.
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_sign_in,
            container,
            false
        )
    }

    // Connects the sign-in button to the next screen.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Temporary navigation for layout testing.
        // Real authentication will be implemented later.
        view.findViewById<View>(R.id.btnSignIn).setOnClickListener {
            findNavController().navigate(R.id.action_signIn_to_home)
        }
    }
}