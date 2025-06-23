package com.example.call_ambulance.service.api

data class LoginResponse(
    val token: String?,                     // Optional token
    val message: String?,                 // Message from server
    val error: String?,
    val status : Int,
    val partner: partner?   // Nested object
)