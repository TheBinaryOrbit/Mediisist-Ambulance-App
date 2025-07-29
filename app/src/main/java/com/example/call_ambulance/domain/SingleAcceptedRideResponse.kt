package com.example.call_ambulance.domain

// ✅ Accepted Ride Item model
data class AcceptedRideItem(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val isLocationAvail: Boolean,
    val address: String,
    val isCallAccepted: Boolean,
    val isRideAccepted: Boolean,
    val createdAt: String,
    val lat : String,
    val lng : String
)

// ✅ Single Accepted Ride Response
data class SingleAcceptedRideResponse(
    val message: String,
    val address: String,
    val ride:  List<AcceptedRideItem>
)
