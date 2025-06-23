package com.example.call_ambulance.service.api

import com.example.call_ambulance.domain.PendingRideResponse
import com.example.call_ambulance.domain.SingleAcceptedRideResponse
import com.example.call_ambulance.domain.StatusUpdateRequest
import com.example.call_ambulance.myui.profile.ChangePasswordResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // 🔐 Login
    @POST("api/v1/ambulancepartner/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    // 🔄 Change password
    @PATCH("api/v1/ambulancepartner/changepassword/{id}")
    fun changePassword(
        @Path("id") userId: String,
        @Body body: Map<String, String>
    ): Call<ChangePasswordResponse>

    // 🟢 Update online/offline status
    @PATCH("api/v1/ambulancepartner/changestatus/{id}")
    fun updateOnlineStatus(
        @Path("id") userId: String,
        @Body body: StatusUpdateRequest
    ): Call<Void>





    // 👤 Get customer support profile by ID
    @GET("api/v1/ambulancepartner/getambulancepartner/{id}")
    fun getAmbulancePartnerById(@Path("id") id: String): Call<AmbulancePartnerResponse>

    // 📞 Get pending call list
    @GET("api/v1/ride/getpendingambulancelist")
    suspend fun getPendingRideList(): Response<PendingRideResponse>

    // ✅ Accept call (patch)
    @PATCH("api/v1/ride/accept/ambulancepartner/{rideId}")
    suspend fun AcceptRidebyAmbulnacePartner(
        @Path("rideId") rideId: String,
        @Body requestBody: Map<String, String>
    ): Response<Any>


    @GET("api/v1/ride/getambulancepartnerride/{id}")
    suspend fun getRidesByAmbulancePartner(
        @Path("id") userId: String,
        @Query("status") status: String // active OR complete
    ): Response<SingleAcceptedRideResponse>



    // ✅ Complete call (patch)
    @PATCH("api/v1/ride/complete/ambulancepartner/{rideId}")
    suspend fun CompletedByAmbulancePartner(
        @Path("rideId") rideId: String,
        @Body requestBody: Map<String, String>
    ): Response<Any>

    // Decline
    @DELETE("/api/v1/ride/decline/{rideId}")
    suspend fun declineRide(@Path("rideId") rideId: String): Response<Unit>

    // SMS
    @GET("api/v1/ride/sendsms/{rideId}")
    suspend fun sendSMS(
        @Path("rideId") rideId: String
    ): Response<Void>


}
