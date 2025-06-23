package com.example.call_ambulance.myui.home

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import com.example.call_ambulance.domain.EmergencyCall
import com.example.call_ambulance.myui.profile.ProfileScreen
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@Composable
fun MainScreen(
    navController: NavHostController,
    acceptedCalls: SnapshotStateList<EmergencyCall>,
    initialTab: String = "home"
) {
    val systemUiController = rememberSystemUiController()
    val useDarkIcons = false
    val statusBarColor = Color(0xFFCE2029)

    LaunchedEffect(Unit) {
        systemUiController.setStatusBarColor(
            color = statusBarColor,
            darkIcons = useDarkIcons
        )
    }

    var selectedTab by remember { mutableStateOf(initialTab) }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(selectedTab) { tab ->
                selectedTab = tab
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                "home" -> HomeScreen(navController)
                "profile" -> ProfileScreen(
                    navController = navController,
                    acceptedCalls = acceptedCalls
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    val items = listOf("home", "profile")
    NavigationBar {
        items.forEach { screen ->
            val icon = when (screen) {
                "home" -> Icons.Default.Home
                "profile" -> Icons.Default.Person
                else -> Icons.Default.Home
            }

            NavigationBarItem(
                icon = { Icon(imageVector = icon, contentDescription = screen) },
                label = { Text(screen.replaceFirstChar { it.uppercase() }) },
                selected = selectedTab == screen,
                onClick = { onTabSelected(screen) }
            )
        }
    }
}
