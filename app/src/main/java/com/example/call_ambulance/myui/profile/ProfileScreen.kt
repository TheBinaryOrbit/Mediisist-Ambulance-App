package com.example.call_ambulance.myui.profile

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.call_ambulance.domain.EmergencyCall

@Composable
fun ProfileScreen(
    navController: NavController,
    acceptedCalls: SnapshotStateList<EmergencyCall>

) {
    var showAboutDialog by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        ProfileHeaderCentered(title = "Profile")

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 20.dp)
        ) {
            item {
                SectionItem(
                    title = "My Account",
                    icon = Icons.Default.Person,
                    onArrowClick = { navController.navigate("myAccount") },
                    wholeRowClickable = false
                )
            }

            item {
                SectionItem(
                    title = "Completed Rides",
                    icon = Icons.Default.Phone,
                    onArrowClick = { navController.navigate("acceptedCalls") },
                    wholeRowClickable = false
                )
            }

            item {
                SectionItem(
                    title = "Privacy Policy",
                    icon = Icons.Default.Lock,
                    onArrowClick = { navController.navigate("privacy") },
                    wholeRowClickable = false
                )
            }

            item {
                SectionItem(
                    title = "Terms & Conditions",
                    icon = Icons.Default.Info,
                    onArrowClick = { navController.navigate("terms") },
                    wholeRowClickable = false
                )
            }

            item {
                SectionItem(
                    title = "About",
                    icon = Icons.Default.Info,
                    onArrowClick = { showAboutDialog = true },
                    wholeRowClickable = false
                )
            }

            item {
                SectionItem(
                    title = "Logout",
                    icon = Icons.Default.ExitToApp,
                    iconTint = Color.Red,
                    textColor = Color.Red,
                    onClick = { showLogoutConfirm = true },
                    wholeRowClickable = true
                )
            }
        }

        if (showAboutDialog) {
            AlertDialog(
                onDismissRequest = { showAboutDialog = false },
                confirmButton = {
                    TextButton(onClick = { showAboutDialog = false }) {
                        Text("OK")
                    }
                },
                title = { Text("About") },
                text = {
                    Text("Mediisist Ambulance Partner\nVersion 1.0.0\nDeveloped by Binary Orbit ")
                }
            )
        }

        if (showLogoutConfirm) {
            AlertDialog(
                onDismissRequest = { showLogoutConfirm = false },
                title = { Text("Confirm Logout") },
                text = { Text("Do you really want to logout?") },
                confirmButton = {
                    TextButton(onClick = {
                        showLogoutConfirm = false

                        // ✅ Set isLogin = false in SharedPreferences
                        val sharedPref = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
                        sharedPref.edit()
                            .putBoolean("isLogin", false)
                            .remove("user_id")
                            .apply()

                        // 🔁 Navigate to login screen and clear all back stack
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }) {
                        Text("Yes", color = Color.Red)
                }

                }
            )
        }
    }
}

@Composable
fun ProfileHeaderCentered(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
            )
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
            .padding(bottom = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SectionItem(
    title: String,
    icon: ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    textColor: Color = Color.Black,
    onArrowClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    wholeRowClickable: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (wholeRowClickable) Modifier.clickable { onClick?.invoke() } else Modifier
            )
            .padding(vertical = 20.dp, horizontal = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
            }

            if (onArrowClick != null) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Navigate",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onArrowClick() }
                )
            }
        }
    }
}
