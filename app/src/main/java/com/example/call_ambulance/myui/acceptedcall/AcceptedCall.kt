package com.example.call_ambulance.myui.acceptedcall

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import com.example.call_ambulance.domain.EmergencyCall
import com.example.call_ambulance.myui.home.InfoRowVertical
import com.example.call_ambulance.myui.home.getFormattedDate
import com.example.call_ambulance.myui.home.getFormattedTime
import com.example.call_ambulance.service.api.ApiClient
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay


@Composable
fun AcceptedRideCallsScreen(navController: NavController) {
    val context = LocalContext.current
    val viewModel: AcceptedCallsViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                AcceptedCallsViewModel(context.applicationContext as Application)
            }
        }
    )

    val acceptedCalls by viewModel.acceptedCalls.collectAsState()
    val isRefreshing by viewModel.isSwipeRefreshing.collectAsState()

    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = isRefreshing)

    LaunchedEffect(Unit) {
        viewModel.fetchAcceptedCalls()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)

    ) {
        ScreenHeader(title = "Call Logs") {
            navController.navigate("MainScreen/profile") {
                popUpTo("MainScreen/profile") { inclusive = true }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = { viewModel.refreshWithDelay() }
        ) {
            if (acceptedCalls.isEmpty() && !isRefreshing) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No completed ride yet.",
                        color = Color.Gray,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                LazyColumn {
                    items(acceptedCalls) { call ->
                        AcceptedCallItem(call = call)
                    }
                }
            }
        }
    }
}


// --------------------- ViewModel Inside ---------------------
class AcceptedCallsViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    private val _acceptedCalls = MutableStateFlow<List<EmergencyCall>>(emptyList())
    val acceptedCalls: StateFlow<List<EmergencyCall>> = _acceptedCalls
    private val _isSwipeRefreshing = MutableStateFlow(false)
    val isSwipeRefreshing: StateFlow<Boolean> = _isSwipeRefreshing

    fun refreshWithDelay() {
        viewModelScope.launch {
            _isSwipeRefreshing.value = true
            delay(1500)
            fetchAcceptedCalls()
            _isSwipeRefreshing.value = false
        }
    }


    fun fetchAcceptedCalls() {
        val sharedPref = context.getSharedPreferences("UserSession", Application.MODE_PRIVATE)
        val userId = sharedPref.getString("user_id", null)
        Log.d("AcceptedCallsVM", "User ID from prefs: $userId")

        if (userId == null) {
            Log.e("AcceptedCallsVM", "user_id is null from SharedPreferences")
            return
        }

        viewModelScope.launch {
            try {
                Log.d("AcceptedCallsVM", "Calling API for userId: $userId")
                val response = ApiClient.apiService.getRidesByAmbulancePartner(userId, status = "complete")
                if (response.isSuccessful) {
                    Log.d("MapRedirect", "Mapped List: ${response.body()}")
                    val rideList = response.body()?.ride ?: emptyList()
                    val mappedList = rideList.map { ride ->
                        EmergencyCall(
                            id = ride.id,
                            patientName = ride.name,
                            phoneNumber = ride.phoneNumber,
                            address = ride.address,
                            lat = ride.lat,
                            lng = ride.lng,
                            status = "accepted",
                            createdAt = ride.createdAt
                        )
                    }



                    _acceptedCalls.value = mappedList
                } else {
                    Log.e("AcceptedCallsVM", "Failed to fetch: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("AcceptedCallsVM", "Error: ${e.message}", e)
            }
        }
    }

}

// --------------------- UI Composables ---------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenHeader(title: String, onBackClick: (() -> Unit)? = null) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(
                    topStart = 0.dp,
                    topEnd = 0.dp,
                    bottomStart = 32.dp,
                    bottomEnd = 32.dp
                )
            )
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
            .padding(bottom = 16.dp)
    ) {
        if (onBackClick != null) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        Text(
            text = title,
            fontSize = 20.sp,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}


@Composable
fun AcceptedCallItem(call: EmergencyCall?) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Show "Fetching data..." if call is null
    if (call == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Fetching data...",
                fontSize = 16.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
        }
        return
    }

    val formattedDate = call.createdAt?.let { getFormattedDate(it) } ?: "N/A"
    val formattedTime = call.createdAt?.let { getFormattedTime(it) } ?: "N/A"

    var isLoading by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    if (showSuccess) {
        LaunchedEffect(Unit) {
            delay(3000)
            showSuccess = false
        }
    }

    if (showError) {
        LaunchedEffect(Unit) {
            delay(3000)
            showError = false
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp, pressedElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Completed Call",
                    color = Color(0xFFFF9800),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Time",
                        tint = Color(0xFF6C757D),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$formattedTime • $formattedDate",
                        fontSize = 12.sp,
                        color = Color(0xFF6C757D),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            InfoRowVertical(
                icon = Icons.Default.Person,
                label = "Patient:",
                value = call.patientName,
                iconColor = Color(0xFF4285F4)
            )

            Spacer(modifier = Modifier.height(8.dp))

            InfoRowVertical(
                icon = Icons.Default.Call,
                label = "Phone:",
                value = call.phoneNumber,
                iconColor = Color(0xFF34A853)
            )

            Spacer(modifier = Modifier.height(8.dp))

            InfoRowVertical(
                icon = Icons.Default.Home,
                label = "Address:",
                value = call.address,
                iconColor = Color(0xFF34A853)
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (showSuccess) {
                Text(
                    text = "SMS sent successfully!",
                    color = Color.Green,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (showError) {
                Text(
                    text = errorMessage,
                    color = Color.Red,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        if (call.id == null) {
                            errorMessage = "Error: Missing ride ID"
                            showError = true
                            return@Button
                        }

                        val lat = call.lat
                        val lng = call.lng
                        Log.d("MapRedirect", "Call object: $call")

                        if (lat != null && lng != null) {
                            val gmapsUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng")
                            val intent = Intent(Intent.ACTION_VIEW, gmapsUri).apply {
                                setPackage("com.google.android.apps.maps")
                            }

                            try {
                                context.startActivity(intent)
                            } catch (e: ActivityNotFoundException) {
                                val fallbackIntent = Intent(Intent.ACTION_VIEW, gmapsUri)
                                context.startActivity(fallbackIntent)
                            }
                        } else {
                            errorMessage = "Error: Missing latitude or longitude"
                            showError = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 2.dp,
                        pressedElevation = 0.dp
                    ),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Get Route",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Get Route",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
