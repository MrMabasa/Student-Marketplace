package com.studentmarketplace.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.studentmarketplace.R

// Displays the new student account registration screen.
class RegisterFragment : Fragment() {

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

    // Connects the registration button to the next screen.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Temporary navigation for testing.
        // Actual account creation and validation will be added later.
        view.findViewById<View>(R.id.btnRegister).setOnClickListener {
            findNavController().navigate(R.id.action_register_to_home)
        }
    }
}