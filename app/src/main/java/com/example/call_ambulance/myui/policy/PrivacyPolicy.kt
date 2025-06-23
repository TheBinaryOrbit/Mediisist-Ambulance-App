package com.example.call_ambulance.myui.policy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun PrivacyPolicyScreen(navController: NavController) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.primary,
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
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "Privacy Policy",
                    color = Color.White,
                    fontSize = 20.sp,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(3f),
                )

                Spacer(modifier = Modifier.weight(1f))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {



            SectionTitle("1. Information We Collect")
            BodyText("- Name, phone number, and address (only when provided by the user).\n" +
                    "- Location data (only used to provide emergency services).\n" +
                    "- Device information (such as model, OS version) for performance monitoring.")

            SectionTitle("2. How We Use Information")
            BodyText("- Provide emergency support and connect users with ambulance or assistance services.\n" +
                    "- Improve app functionality and performance.\n" +
                    "- Notify users in case of emergencies via push notifications.")

            SectionTitle("3. Data Sharing")
            BodyText("We do not share your personal data with third parties, except:\n" +
                    "- When required by law.\n" +
                    "- With emergency service providers to assist you.")

            SectionTitle("4. Data Security")
            BodyText("We implement standard security measures to protect your data. However, no method of transmission over the internet is 100% secure.")

            SectionTitle("5. Your Choices")
            BodyText("- Deny location or notification permissions through your device settings.\n" +
                    "- Request deletion of your data by contacting us.")

            SectionTitle("6. Changes to This Policy")
            BodyText("We may update this policy from time to time. Updates will be reflected in the app.")

            SectionTitle("7. Contact Us")
            BodyText("If you have any questions, please contact us at: support@example.com")

            Spacer(modifier = Modifier.height(100.dp)) // Bottom spacing
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun BodyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}


