package com.savingcoach.app.data.model

data class User(
    val uid: String = "",
    val email: String = "",
    val username: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val age: Int? = null,
    val gender: String? = null,
    val salaryRange: String? = null,
    val fieldOfWork: String? = null,
    val onboardingCompleted: Boolean = false,
    val currencyPreference: String = "MMK",
    val languagePreference: String = "en"
)

