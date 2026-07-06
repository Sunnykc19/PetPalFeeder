package com.example.petpalfeeder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PetPalFeederApp()
        }
    }
}

@Composable
fun PetPalFeederApp() {
    var lastFedTime by remember { mutableStateOf("Not fed yet") }
    var lastWateredTime by remember { mutableStateOf("Has not drank water yet") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "PetPal Feeder",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Pet Name: Shadow",
                fontSize = 22.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Food Section
            Text(
                text = "Last Fed:",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = lastFedTime,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    lastFedTime = getCurrentTime()
                }
            ) {
                Text(text = "Feed Now")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Water Section
            Text(
                text = "Water?:",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = lastWateredTime,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    lastWateredTime = getCurrentTime()
                }
            ) {
                Text(text = "Drank Water Now")
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    lastFedTime = "Not fed yet"
                    lastWateredTime = "Has not drank water yet"
                }
            ) {
                Text(text = "Reset All")
            }
        }
    }
}

fun getCurrentTime(): String {
    val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return formatter.format(Date())
}
