// Declares plugins available to the project modules.
plugins {
    // Android application plugin.
    alias(libs.plugins.android.application) apply false

    // Firebase Google Services plugin.
    alias(libs.plugins.google.services) apply false
}