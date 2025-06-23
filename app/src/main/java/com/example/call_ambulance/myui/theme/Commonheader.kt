package com.example.call_ambulance.myui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

@Composable
fun CommonHeader(
    title: String,
    onBackClick: (() -> Unit)? = null,
    showMenu: Boolean = false,
    onMenuClick: (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
            )
            .padding(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                start = 16.dp,
                end = 16.dp,
                bottom = 20.dp
            )
            .zIndex(1f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (onBackClick != null) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp)) // for spacing balance
            }

            Text(
                text = title,
                fontSize = 20.sp,
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium
            )

            if (showMenu) {
                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = Color.White
                        )
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("About") },
                            onClick = {
                                expanded = false
                                onMenuClick?.invoke()
                            }
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp)) // for spacing balance
            }
        }
    }
}
