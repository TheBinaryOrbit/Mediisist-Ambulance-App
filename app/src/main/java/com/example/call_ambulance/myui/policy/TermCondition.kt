package com.example.call_ambulance.myui.policy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun TermsAndConditionsScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
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
                        popUpTo("MainScreen/profile") { inclusive = false }
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
                    text = "Terms & Conditions",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 20.sp,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )

                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(48.dp)) // Symmetry
            }
        }

        // Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {




            SectionTitle("1. Use of the App")
            BodyText("You agree to use this app only for lawful purposes, particularly for emergency support or assistance services. Misuse or false reporting may result in permanent account suspension.")

            SectionTitle("2. Eligibility")
            BodyText("You must be at least 18 years old to use this app. By using the app, you confirm that you are of legal age.")

            SectionTitle("3. User Conduct")
            BodyText("You agree not to:\n" +
                    "- Submit false emergency requests\n" +
                    "- Harass or threaten any person\n" +
                    "- Attempt to disrupt app functionality")

            SectionTitle("4. Data Usage")
            BodyText("We may collect data such as your location, device information, and emergency request logs to help provide services. Please refer to our Privacy Policy for full details.")

            SectionTitle("5. Limitation of Liability")
            BodyText("We are not liable for:\n" +
                    "- Delays in emergency responses\n" +
                    "- Actions taken by third-party emergency services\n" +
                    "- Technical issues or app downtime")

            SectionTitle("6. Modifications")
            BodyText("We reserve the right to modify these Terms at any time. Continued use of the app constitutes acceptance of updated Terms.")

            SectionTitle("7. Termination")
            BodyText("We may suspend or terminate access if these Terms are violated.")

            SectionTitle("8. Contact")
            BodyText("For questions, contact us at: support@example.com")

            Spacer(modifier = Modifier.height(100.dp))
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
