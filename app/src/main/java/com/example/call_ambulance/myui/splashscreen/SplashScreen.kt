package com.example.call_ambulance.myui.splashscreen

import android.content.Context
import androidx.compose.ui.Alignment
import android.view.animation.OvershootInterpolator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.call_ambulance.R

@Composable
fun SplashScreen(navController: NavController) {
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }
    val context = LocalContext.current // 👈 context chahiye for SharedPreferences

    val customFont = FontFamily(Font(R.font.lobster_regular))

    LaunchedEffect(true) {
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 1500,
                    easing = {
                        OvershootInterpolator(4f).getInterpolation(it)
                    }
                )
            )
        }
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1500)
            )
        }

        delay(2000)

        val isLoggedIn = isUserLoggedIn(context)

        val targetRoute = if (isLoggedIn) "MainScreen/home" else "login"
        navController.navigate(if (isLoggedIn) "MainScreen/home" else "login") {
            popUpTo("splash") { inclusive = true }
        }


    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F8FA)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.logonav),
                contentDescription = "Mediisist Logo",
                modifier = Modifier
                    .scale(scale.value)
                    .alpha(alpha.value)
                    .size(200.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Optional Text
        }
    }
}

fun isUserLoggedIn(context: Context): Boolean {
    val sharedPref = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
    return sharedPref.getBoolean("isLogin", false)
}
