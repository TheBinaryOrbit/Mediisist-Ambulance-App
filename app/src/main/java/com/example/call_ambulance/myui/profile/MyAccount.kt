package com.example.call_ambulance.myui.profile

import android.R.attr.name
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.content.MediaType.Companion.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role.Companion.Image
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.call_ambulance.service.api.ApiClient
import com.example.call_ambulance.service.api.AmbulancePartnerResponse
import com.example.call_ambulance.R
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun MyAccountScreen(navController: NavController) {
    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
    val userId = sharedPref.getString("user_id", null)

    var name by remember { mutableStateOf("") }
    var vehiclenum by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var photoUrl by remember { mutableStateOf<String?>(null) }

    var isProfileLoading by remember { mutableStateOf(true) }

    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var isPasswordLoading by remember { mutableStateOf(false) }


    // Fetch user profile data

    LaunchedEffect(userId) {
        userId?.let {
            isProfileLoading = true
            ApiClient.apiService.getAmbulancePartnerById(it)
                .enqueue(object : Callback<AmbulancePartnerResponse> {
                    override fun onResponse(
                        call: Call<AmbulancePartnerResponse>,
                        response: Response<AmbulancePartnerResponse>
                    ) {
                        isProfileLoading = false
                        if (response.isSuccessful) {
                            Log.d("RESPONSE_JSON", response.body().toString())
                            response.body()?.partner?.let { cs ->
                                name = cs.name
                                phone = cs.phoneNumber
                                email = cs.email
                                vehiclenum= cs.vehicleNumber
                                val baseUrl = "https://2q766kvz-9000.inc1.devtunnels.ms/"
                                photoUrl = cs.imageUrl?.takeIf { it.isNotBlank() }?.let { "$baseUrl$it" }

                                Log.d("USER_NAME", name)
                                Log.d("USER_PHONE", phone)
                                Log.d("USER_EMAIL", email)
                                Log.d("USER_IMAGE_URL", photoUrl ?: "No Image URL - using default")

                                sharedPref.edit().apply {
                                    putString("name", cs.name)
                                    putString("phone", cs.phoneNumber)
                                    putString("email", cs.email)
                                    putString("photoUrl", photoUrl ?: "")
                                    apply()
                                }
                            }
                        } else {
                            Toast.makeText(
                                context,
                                "Failed to load profile info",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onFailure(call: Call<AmbulancePartnerResponse>, t: Throwable) {
                        isProfileLoading = false
                        Toast.makeText(
                            context,
                            "Network error: ${t.localizedMessage}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                )
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    navController.navigate("MainScreen/profile") {
                        popUpTo("MainScreen/profile") { inclusive = true }
                        launchSingleTop = true
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "My Account",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.size(48.dp))
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF7F7F7))
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Image
            if (photoUrl.isNullOrEmpty()) {
                Image(
                    painter = painterResource(id = R.drawable.profile_bg_no), // your default image
                    contentDescription = "Profile Image",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color.Gray, CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = "Profile Image",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color.Gray, CircleShape),
                    contentScale = ContentScale.Crop
                )
            }


            Spacer(modifier = Modifier.height(20.dp))

            // Name
            Text(
                text = name,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            // Role
            Text(
                text = email,
                fontSize = 16.sp,
                color = Color.DarkGray
            )

            Spacer(modifier = Modifier.height(30.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 100.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                ReadOnlyField("Name", name, isProfileLoading)
                ReadOnlyField("Phone", phone, isProfileLoading)
                ReadOnlyField("Email", email, isProfileLoading)
                ReadOnlyField("VehicleNum", vehiclenum, isProfileLoading)

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Change Password",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 24.dp, bottom = 8.dp)
                )

                AccountField("Old Password", oldPassword, isPasswordLoading) { oldPassword = it }
                AccountField("New Password", newPassword, isPasswordLoading) { newPassword = it }

                Button(
                    onClick = {
                        if (userId.isNullOrEmpty()) {
                            Toast.makeText(context, "User ID not found. Please login again.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        isPasswordLoading = true

                        val request = mapOf(
                            "oldPassword" to oldPassword,
                            "newPassword" to newPassword
                        )

                        ApiClient.apiService.changePassword(userId, request)
                            .enqueue(object : Callback<ChangePasswordResponse> {
                                override fun onResponse(
                                    call: Call<ChangePasswordResponse>,
                                    response: Response<ChangePasswordResponse>
                                ) {
                                    isPasswordLoading = false
                                    if (response.code() == 200) {
                                        Toast.makeText(
                                            context,
                                            response.body()?.message ?: "Password updated",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        oldPassword = ""
                                        newPassword = ""
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Failed to update password (code: ${response.code()})",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }

                                override fun onFailure(
                                    call: Call<ChangePasswordResponse>,
                                    t: Throwable
                                ) {
                                    isPasswordLoading = false
                                    Toast.makeText(
                                        context,
                                        "Network error: ${t.localizedMessage}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            })
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isPasswordLoading
                ) {
                    if (isPasswordLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Update Password")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun AccountField(label: String, value: String, isLoading: Boolean, onValueChange: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, shape = RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = TextStyle(fontSize = 16.sp),
                    modifier = Modifier.weight(1f)
                )
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}

@Composable
fun ReadOnlyField(label: String, value: String, isLoading: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, shape = RoundedCornerShape(8.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value.ifEmpty { "..." },
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            }
        }
    }
}


