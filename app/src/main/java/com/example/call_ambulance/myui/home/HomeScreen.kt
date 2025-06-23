package com.example.call_ambulance.myui.home


import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.icu.util.TimeZone
import android.net.Uri
import android.os.Build
import android.os.Looper
import android.os.PowerManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.accompanist.pager.*
import com.valentinilk.shimmer.ShimmerBounds
import com.valentinilk.shimmer.rememberShimmer
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.call_ambulance.R
import com.example.call_ambulance.domain.EmergencyCall
import com.example.call_ambulance.domain.HomeViewModel
import java.util.Date
import java.util.Locale
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner

import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.android.gms.location.*



val CustomFontFamily = FontFamily(
    Font(R.font.poetsenoneregular, FontWeight.Normal),
    Font(R.font.poetsenoneregular, FontWeight.Bold)
)

@OptIn(ExperimentalPagerApi::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val sharedPref = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
    val userId = sharedPref.getString("user_id", null) ?: return

    val isLoading by viewModel.isLoading.collectAsState()
    val name by viewModel.name.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    var isExpanded by remember { mutableStateOf(false) }

    // Handle back button
    BackHandler { activity?.finishAffinity() }

    // Fetch user data on screen load
    LaunchedEffect(userId) {
        viewModel.fetchUserData(userId, context)
    }

    // Show skeleton loader
    if (name.isEmpty() || isOnline == null) {
        SkeletonScreen()
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (!isExpanded) {
                WelcomeHeader(
                    name = name,
                    isOnline = isOnline,
                    onToggleOnline = { newValue ->
                        if (newValue) {
                            if (ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                // inside HomeScreen's onToggleOnline
                                ensureLocationAndGoOnline(context, userId, viewModel)

                            } else {
                                Toast.makeText(context, "Location permission not granted", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            viewModel.updateStatus(false, 0.0, 0.0, userId, context)
                        }
                    }

                    ,
                    userId = userId,
                    isLoading = isLoading
                )

                ImageSlider()
            }

            Spacer(modifier = Modifier.height(30.dp))

            if (isOnline == true) {
                EmergencyCallsScreen(viewModel, context)
            } else {
                OfflineScreen()
            }
        }
    }
}


@RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
fun ensureLocationAndGoOnline(
    context: Context,
    userId: String,
    viewModel: HomeViewModel
) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val isLocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

    if (!isLocationEnabled) {
        Toast.makeText(context, "Please enable GPS to go online", Toast.LENGTH_SHORT).show()
        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        return
    }

    fusedLocationClient.lastLocation
        .addOnSuccessListener { location ->
            if (location != null) {
                val sharedPref = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                sharedPref.edit()
                    .putString("latitude", location.latitude.toString())
                    .putString("longitude", location.longitude.toString())
                    .apply()
                viewModel.updateStatus(true, location.latitude, location.longitude, userId, context)
                //viewModel.checkBatteryOptimizations(context)
            } else {
                // Request active update
                val locationRequest = LocationRequest.create().apply {
                    interval = 0
                    fastestInterval = 0
                    priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                    numUpdates = 1
                }

                val locationCallback = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        val loc = result.lastLocation
                        if (loc != null) {
                            viewModel.updateStatus(true, loc.latitude, loc.longitude, userId, context)
                           // viewModel.checkBatteryOptimizations(context)
                        } else {
                            Toast.makeText(context, "Unable to fetch location", Toast.LENGTH_SHORT).show()
                        }
                        fusedLocationClient.removeLocationUpdates(this)
                    }
                }

                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
            }
        }
        .addOnFailureListener {
            Toast.makeText(context, "Failed to get location: ${it.message}", Toast.LENGTH_SHORT).show()
        }
}


@Composable
fun WelcomeHeader(
    name: String,
    isOnline: Boolean?,
    onToggleOnline: (Boolean) -> Unit,
    userId: String,
    isLoading: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp)
            )
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
            .padding(bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White)
                        .border(2.dp, Color.White, RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Default Profile",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Welcome,",
                        fontFamily = CustomFontFamily,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = name,
                        fontFamily = CustomFontFamily,
                        fontSize = 25.sp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Switch(
                        checked = isOnline == true,
                        onCheckedChange = onToggleOnline,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            uncheckedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF43A047),
                            uncheckedTrackColor = Color(0xFFE53935)
                        )
                    )
                }
            }
        }
    }
}



@Composable
fun SkeletonScreen() {
    val shimmer = rememberShimmer(shimmerBounds = ShimmerBounds.Window)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Welcome Header Skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .shimmer(shimmer)
                .background(Color.LightGray)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Image Slider Skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(horizontal = 16.dp)
                .shimmer(shimmer)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.LightGray)
        )

        Spacer(modifier = Modifier.height(30.dp))

        // Pending Calls Card Skeletons
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            items(4) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .padding(vertical = 10.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .shimmer(shimmer)
                        .background(Color.LightGray)
                )
            }
        }
    }
}


@Composable
fun EmergencyCallsScreen(viewModel: HomeViewModel, context: Context) {
    val pendingCalls by viewModel.pendingCalls.collectAsState()
    val acceptedCalls by viewModel.acceptedCalls.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabTitles = listOf("Pending", "Active")

    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = isLoading)

    Column {
        // 🔹 Tab Layout
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp)),
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        height = 4.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                divider = {},
                containerColor = Color.Transparent
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selectedTabIndex == index) MaterialTheme.colorScheme.primary else Color.Gray,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    )
                }
            }
        }

        // 🔄 Swipe to Refresh
        val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = viewModel.isSwipeRefreshing)

        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = {
                val sharedPref = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
                val userId = sharedPref.getString("user_id", null) ?: return@SwipeRefresh
                viewModel.refreshAllCalls(userId, context)
            },
            modifier = Modifier.fillMaxSize()
        ) {
            when (selectedTabIndex) {
                0 -> PendingCallsList(
                    calls = pendingCalls,
                    isLoading = isLoading, // For progress inside list (optional)
                    onAccept = { call -> viewModel.acceptCall(call.id, context) }
                )
                1 -> AcceptedCallsList(
                    calls = acceptedCalls,
                    isLoading = isLoading,
                    onComplete = { call -> viewModel.completeCall(call.id, context) },
                    onDecline = { call -> viewModel.declineCall(call.id, context) }
                )
            }
        }

    }

    // 🔄 Initial Load
    LaunchedEffect(Unit) {
        val sharedPref = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val userId = sharedPref.getString("user_id", null) ?: return@LaunchedEffect
       // viewModel.fetchPendingCalls(context)
       // viewModel.fetchActiveCalls(userId, context)
    }
}



@Composable
fun PendingCallsList(
    calls: List<EmergencyCall>,
    isLoading: Boolean,
    onAccept: (EmergencyCall) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp)
    ) {
        if (isLoading) {
            items(4) {
                CallItemSkeleton()
            }
        } else if (calls.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = Color.Gray,
                            modifier = Modifier.size(100.dp) // Increased icon size
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Currently not available for ride",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                    }
                }
            }
        } else {
            items(calls) { call ->
                PendingCallItem(
                    call = call,
                    onAccept = { onAccept(call) },
                    context = LocalContext.current
                )
            }
        }
    }
}


@Composable
fun PendingCallItem(
    call: EmergencyCall,
    onAccept: () -> Unit,
    context: Context = LocalContext.current
) {
    val formattedDate = call.createdAt?.let { getFormattedDate(it) } ?: "N/A"
    val formattedTime = call.createdAt?.let { getFormattedTime(it) } ?: "N/A"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onAccept() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8F9FA),
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with time/date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Emergency Call",
                    color = Color(0xFFD32F2F),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
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

            Spacer(modifier = Modifier.height(16.dp))

            // Patient Info
            InfoRowVertical(
                icon = Icons.Default.Person,
                label = "Patient:",
                value = call.patientName,
                iconColor = Color(0xFF4285F4)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Phone Info
            InfoRowVertical(
                icon = Icons.Default.Call,
                label = "Phone:",
                value = call.phoneNumber,
                iconColor = Color(0xFF34A853)
            )

            Spacer(modifier = Modifier.height(8.dp))


            Spacer(modifier = Modifier.height(16.dp))

            // Accept Button
            Button(
                onClick = onAccept,
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
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Accept",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Accept Ride",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun InfoRowVertical(
    icon: ImageVector,
    label: String,
    value: String,
    iconColor: Color = Color(0xFF6C757D)
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                color = Color(0xFF6C757D),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                color = Color(0xFF212529),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}


@Composable
fun AcceptedCallsList(
    calls: List<EmergencyCall>,
    isLoading: Boolean,
    onComplete: (EmergencyCall) -> Unit,
    onDecline: (EmergencyCall) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp)
    ) {
        if (isLoading) {
            items(4) {
                CallItemSkeleton()
            }
        } else if (calls.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No accepted calls available")
                }
            }
        } else {
            items(calls) { call ->
                AcceptedCallItem(
                    call = call,
                    onComplete = { onComplete(call) },
                    onDecline = { onDecline(call) },
                    context = LocalContext.current
                )
            }
        }
    }
}


@Composable
fun AcceptedCallItem(
    call: EmergencyCall,
    onComplete: () -> Unit,
    onDecline: () -> Unit,
    context: Context = LocalContext.current
) {
    val formattedDate = call.createdAt?.let { getFormattedDate(it) } ?: "N/A"
    val formattedTime = call.createdAt?.let { getFormattedTime(it) } ?: "N/A"

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
            // 🔸 Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Call",
                    color = Color(0xFFFF9800),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = "Time", tint = Color(0xFF6C757D), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("$formattedTime • $formattedDate", fontSize = 12.sp, color = Color(0xFF6C757D), fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            InfoRowVertical(icon = Icons.Default.Person, label = "Patient:", value = call.patientName, iconColor = Color(
                0xFFCE2029
            )
            )
            Spacer(modifier = Modifier.height(8.dp))
            InfoRowVertical(icon = Icons.Default.Call, label = "Phone:", value = call.phoneNumber, iconColor = Color(0xFF34A853))
            Spacer(modifier = Modifier.height(16.dp))

            // 🔸 Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (call.isLocationAvail == true) {
                    // ✅ Show Get Route if location is available
                    Button(
                        onClick = {
                            val lat = call.lat
                            val lng = call.lng

                            val gmapsUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng")
                            val intent = Intent(Intent.ACTION_VIEW, gmapsUri).apply {
                                setPackage("com.google.android.apps.maps")
                            }

                            try {
                                context.startActivity(intent)
                            } catch (e: ActivityNotFoundException) {
                                context.startActivity(Intent(Intent.ACTION_VIEW, gmapsUri))
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEE0202), contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = "Route", modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Get Route")
                    }
                } else {
                    // ❌ Show disabled button if location not available
                    Button(
                        onClick = { /* No action */ },
                        enabled = false,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Gray,
                            contentColor = Color.White,
                            disabledContainerColor = Color.LightGray,
                            disabledContentColor = Color.DarkGray
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.LocationDisabled, contentDescription = "No Location", modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Waiting for Location")
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // ✅ Complete Button
                Button(
                    onClick = onComplete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C61EC), contentColor = Color.White) ,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.DoneAll, contentDescription = "Complete", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Complete")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 📞 Call Button
            // 📞 Call Button
            Row(modifier = Modifier.fillMaxWidth()) {
                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        val intent = Intent(Intent.ACTION_CALL).apply {
                            data = Uri.parse("tel:${call.phoneNumber}")
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Call failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Call permission not granted", Toast.LENGTH_SHORT).show()
                    }
                }

                Button(
                    onClick = {
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)
                            == PackageManager.PERMISSION_GRANTED
                        ) {
                            val intent = Intent(Intent.ACTION_CALL).apply {
                                data = Uri.parse("tel:${call.phoneNumber}")
                            }
                            context.startActivity(intent)
                        } else {
                            launcher.launch(Manifest.permission.CALL_PHONE)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = "Call", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Call")
                }
            }

        }
    }
}








@Composable
fun OfflineScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ambulance_offline_new),
            contentDescription = "Offline Image",
            modifier = Modifier
                .padding(bottom = 20.dp)
                .size(300.dp)
        )
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun ImageSlider() {
    val imageList = listOf(R.drawable.ambulance, R.drawable.logo, R.drawable.logonav)
    val pagerState = rememberPagerState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            val nextPage = (pagerState.currentPage + 1) % imageList.size
            coroutineScope.launch { pagerState.animateScrollToPage(nextPage) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 14.dp, top = 12.dp, bottom = 6.dp)
            .height(180.dp)
    ) {
        HorizontalPager(
            count = imageList.size,
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            Image(
                painter = painterResource(id = imageList[page]),
                contentDescription = "Slider Image",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .border(
                        width = 1.dp,
                        color = Color.Gray,
                        shape = RoundedCornerShape(20.dp)
                    )
            )
        }

        HorizontalPagerIndicator(
            pagerState = pagerState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        )
    }
}

@Composable
fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.DarkGray,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = buildAnnotatedString {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(label)
                }
                append(value)
            },
            fontSize = 18.sp,
            color = Color.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun CallItemSkeleton() {
    val shimmer = rememberShimmer(shimmerBounds = ShimmerBounds.Window)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .padding(horizontal = 30.dp, vertical = 10.dp)
            .shimmer(shimmer)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.LightGray)
    )
}

private fun checkBatteryOptimizations(context: Context) {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val packageName = context.packageName
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                Toast.makeText(
                    context,
                    "Battery optimization settings not available",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}

fun getFormattedDate(createdAt: String?): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val date = inputFormat.parse(createdAt ?: "")
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        "N/A"
    }
}

fun getFormattedTime(createdAt: String?): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        inputFormat.timeZone = TimeZone.getTimeZone("UTC") // assume server sent UTC

        val outputFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        outputFormat.timeZone = TimeZone.getDefault() // your local time

        val date = inputFormat.parse(createdAt ?: "")
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        "N/A"
    }
}










