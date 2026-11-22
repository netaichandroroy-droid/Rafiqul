package com.example.model

data class Member(
    val id: String,
    val name: String,
    val designation: String,
    val phone: String,
    val depot: String,
    val code: String = "",
    val area: String = "",
    val isActive: Boolean = true,
    val createdAtMs: Long = System.currentTimeMillis(),
    val passwordHash: String = "",
    val isDefaultPassword: Boolean = true,
    val email: String = "",
    val address: String = "",
    val joiningDate: String = "",
    val profilePhotoPath: String = "",
    val salesAmount: Double = 0.0
)
