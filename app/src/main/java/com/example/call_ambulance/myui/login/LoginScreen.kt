package com.example.call_ambulance.myui.login

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.call_ambulance.R
import com.example.call_ambulance.service.api.ApiClient
import com.example.call_ambulance.service.api.LoginRequest
import com.example.call_ambulance.service.api.LoginResponse
import com.google.firebase.messaging.FirebaseMessaging
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {
    var phone by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var fcmToken by remember { mutableStateOf("1234") }
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    fcmToken = task.result ?: "1234"
                }
            }
    }

    val baseColor = Color(0xFFCE2029)
    val textFieldBackground = Color(0xFFF1F5F9)
    val customFont = FontFamily(Font(R.font.ubnatu_bold))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFCE2029)) // 👈 Blue background remains!
    ) {


        // 🔹 Background Image (Full Screen)
        Image(
            painter = painterResource(id = R.drawable.man_ambulnace),
            contentDescription = "Human Image",
            modifier = Modifier
                .fillMaxSize() // 👈 Ensures image covers full screen
                .align(Alignment.Center) // 👈 Keeps it centered
                .offset(x=10.dp,y=-160.dp)
        )

        // 🔹 Logo Image (Top Left)
//        Image(
//            painter = painterResource(id = R.drawable.footerlogo),
//            contentDescription = "Logo",
//            modifier = Modifier
//                .size(190.dp) // 👈 Adjust size as needed
//                .align(Alignment.TopStart) // 👈 Stick to top-left
//                .padding(start = 10.dp, top = 100.dp) // 👈 Positioning
//        )

        // 🔹 Login Section (Overlapping on Image)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 50.dp, topEnd = 50.dp))
                .background(Color.White) // 👈 White curve background
                .align(Alignment.BottomCenter) // 👈 Places login section at bottom
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Login", fontSize = 30.sp, fontFamily = customFont, color = Color.Black)
                Text(
                    text = "Sign into your account",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    placeholder = { Text("Phone Number") },
                    leadingIcon = {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = Color.Gray)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        containerColor = textFieldBackground,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number // ✅ Only numeric keyboard
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                )


                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("Password") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray)
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = Color.Gray
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        containerColor = textFieldBackground,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = Color.Red,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .fillMaxWidth()
                    )
                }

                var isLoading by remember { mutableStateOf(false) }

                Button(
                    onClick = {
                        if (phone.isNotEmpty() && password.isNotEmpty()) {
                            if (fcmToken.isEmpty()) {
                                Toast.makeText(context, "Getting device token. Please wait...", Toast.LENGTH_SHORT).show()
                                Log.w("LOGIN", "FCM token not yet available.")
                                return@Button
                            }

                            isLoading = true

                            val loginRequest = LoginRequest(phone, password, fcmToken)

                            ApiClient.apiService.login(loginRequest)
                                .enqueue(object : Callback<LoginResponse> {
                                    override fun onResponse(
                                        call: Call<LoginResponse>,
                                        response: Response<LoginResponse>
                                    ) {
                                        isLoading = false
                                        if (response.code() == 200) {
                                            val name = response.body()?.partner?.name ?: "User"
                                            val sharedPref = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
                                            sharedPref.edit()
                                                .putString("user_id", response.body()?.partner?.id)
                                                .putBoolean("isLogin", true) // ✅ yeh line add karo
                                                .apply()


                                            Toast.makeText(context, "Welcome $name", Toast.LENGTH_SHORT).show()
                                            navController.navigate("MainScreen/home")
                                        } else {
                                            Log.d("ErrorMessage", response.body()?.message.toString())
                                            Log.d("ErrorMessagge", response.toString())
                                            val msg = response.body()?.error ?: "Login Failed"
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    }

                                    override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                                        isLoading = false
                                        Toast.makeText(context, "Network error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    }
                                })
                        } else {
                            Toast.makeText(context, "Fill all fields", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFCE2029), // 🔵 Always Blue
                        disabledContainerColor = Color(0xFFCE2029),
                        contentColor = Color.White,
                        disabledContentColor = Color.White
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text("Login")
                    }
                }


                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
