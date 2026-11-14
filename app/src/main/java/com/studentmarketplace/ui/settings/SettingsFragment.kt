
package com.studentmarketplace.ui.settings

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import android.widget.Spinner
import com.studentmarketplace.R
import androidx.core.os.LocaleListCompat

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private val languageCodes = listOf("en", "ts", "tn")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupLanguageSelector(view)
        setupDarkModeSwitch(view)
        setupNotificationSwitch(view)
    }

    private fun setupLanguageSelector(view: View) {
        val spinner = view.findViewById<Spinner>(R.id.spinnerLanguage)

        val languages = listOf("English", "XiTsonga", "Setswana")
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            languages
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        val savedLanguage = AppPreferences.getLanguage(requireContext())
        val selectedIndex = languageCodes.indexOf(savedLanguage).coerceAtLeast(0)
        spinner.setSelection(selectedIndex, false)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                selectedView: View?,
                position: Int,
                id: Long
            ) {
                val languageCode = languageCodes[position]

                if (languageCode != AppPreferences.getLanguage(requireContext())) {
                    AppPreferences.setLanguage(requireContext(), languageCode)
                    AppCompatDelegate.setApplicationLocales(
                        LocaleListCompat.forLanguageTags(languageCode)
                    )
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun setupDarkModeSwitch(view: View) {
        val darkModeSwitch = view.findViewById<SwitchCompat>(R.id.switchDarkMode)

        darkModeSwitch.isChecked =
            AppPreferences.isDarkModeEnabled(requireContext())

        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            AppPreferences.setDarkModeEnabled(requireContext(), isChecked)

            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) {
                    AppCompatDelegate.MODE_NIGHT_YES
                } else {
                    AppCompatDelegate.MODE_NIGHT_NO
                }
            )
        }
    }

    private fun setupNotificationSwitch(view: View) {
        val notificationSwitch =
            view.findViewById<SwitchCompat>(R.id.switchNotifications)

        notificationSwitch.isChecked =
            AppPreferences.areNotificationsEnabled(requireContext())

        notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            AppPreferences.setNotificationsEnabled(requireContext(), isChecked)

            Snackbar.make(
                view,
                if (isChecked) "Notifications enabled" else "Notifications disabled",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }
}
