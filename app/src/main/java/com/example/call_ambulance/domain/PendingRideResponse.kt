package com.example.call_ambulance.domain



data class PendingRideResponse(
    val message: String,
    val ride: List<PendingRideItem>
)

data class PendingRideItem(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val createdAt: String
)
