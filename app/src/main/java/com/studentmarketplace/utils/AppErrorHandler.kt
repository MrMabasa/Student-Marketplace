
package com.studentmarketplace.utils

import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.EditText
import com.google.android.material.snackbar.Snackbar

// Provides reusable error messages and animations throughout the app.
object AppErrorHandler {

    // Displays an error, shakes the affected field and shows a Snackbar.
    fun showFieldError(
        rootView: View,
        field: EditText,
        message: String
    ) {
        // Displays the error beneath the affected input field.
        field.error = message

        // Moves focus to the field that needs attention.
        field.requestFocus()

        // Creates a short horizontal shake animation.
        ObjectAnimator.ofFloat(
            field,
            "translationX",
            0f, -12f, 12f, -8f, 8f, 0f
        ).apply {
            duration = 400
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

        // Displays a consistent visible error message.
        Snackbar.make(
            rootView,
            message,
            Snackbar.LENGTH_LONG
        ).show()
    }

    // Displays a general success message.
    fun showSuccess(
        rootView: View,
        message: String
    ) {
        // Shows confirmation using the standard Snackbar.
        Snackbar.make(
            rootView,
            message,
            Snackbar.LENGTH_SHORT
        ).show()
    }

    // Displays a general error message without shaking a field.
    fun showError(
        rootView: View,
        message: String
    ) {
        // Shows a visible error for screen-wide problems.
        Snackbar.make(
            rootView,
            message,
            Snackbar.LENGTH_LONG
        ).show()
    }
}
