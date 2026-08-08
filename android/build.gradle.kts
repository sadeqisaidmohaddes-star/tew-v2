// Root build file — plugins declared here (apply false) so each module
// applies only what it needs. See IMPLEMENTATION.md for module boundaries.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
