package com.example.call_ambulance.service.api

data class AmbulancePartnerResponse(
    val message: String,
    val partner: partner
)

data class partner(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val email: String,
    val imageUrl: String,
    val vehicleNumber: String,
    val isOnline: Boolean
)
