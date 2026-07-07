package com.example.petpalfeeder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import java.text.SimpleDateFormat
import java.util.*

// --- Data Models ---
data class FeedingEntry(val time: String, val amount: String)
data class PetProfile(val name: String, val type: String, val food: String)
data class ScheduleEntry(val name: String, val time: String)

// --- Color Palette ---
val TealHeader = Color(0xFF00796B)
val PurpleHeader = Color(0xFF673AB7)
val BlueHeader = Color(0xFF2196F3)
val GreenHeader = Color(0xFF4CAF50)
val OrangeHeader = Color(0xFFFF9800)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PetPalTrackerApp()
        }
    }
}

@Composable
fun PetPalTrackerApp() {
    val navController = rememberNavController()
    
    // Shared State (Mock Data)
    var petProfile by remember { mutableStateOf(PetProfile("Shadow", "Dog", "1 cup kibble")) }
    val feedingHistory = remember { mutableStateListOf(FeedingEntry("Today 8:00 AM", "1 cup"), FeedingEntry("Yesterday 6:00 PM", "1 cup"), FeedingEntry("Yesterday 8:00 AM", "1 cup")) }
    var lastFedTime by remember { mutableStateOf("8:00 AM") }
    var nextMealTime by remember { mutableStateOf("6:00 PM") }
    var schedule by remember { mutableStateOf(listOf(ScheduleEntry("Breakfast", "8:00 AM"), ScheduleEntry("Dinner", "6:00 PM"))) }
    var remindersOn by remember { mutableStateOf(true) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute != "welcome" && currentRoute != null) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        selected = currentRoute == "home",
                        onClick = { navController.navigate("home") }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Log") },
                        label = { Text("Log") },
                        selected = currentRoute == "log",
                        onClick = { navController.navigate("log") }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = "History") },
                        label = { Text("Settings") },
                        selected = currentRoute == "history",
                        onClick = { navController.navigate("history") }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "welcome",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("welcome") { WelcomeScreen { navController.navigate("home") } }
            composable("home") { 
                HomeScreen(
                    navController, 
                    petProfile.name, 
                    lastFedTime, 
                    nextMealTime
                ) 
            }
            composable("add_pet") { 
                AddPetScreen(
                    petProfile,
                    onSave = { name, type, food -> 
                        petProfile = PetProfile(name, type, food)
                        navController.popBackStack()
                    }
                ) 
            }
            composable("schedule") { 
                ScheduleScreen(
                    schedule,
                    remindersOn,
                    onSave = { newSchedule, reminders ->
                        schedule = newSchedule
                        remindersOn = reminders
                        navController.popBackStack()
                    }
                ) 
            }
            composable("log") { 
                FeedingLogScreen(
                    onSave = { time, amount ->
                        feedingHistory.add(0, FeedingEntry(time, amount))
                        lastFedTime = time
                        navController.navigate("home")
                    }
                ) 
            }
            composable("history") { HistoryScreen(feedingHistory) }
        }
    }
}

// --- Screens ---

@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(TealHeader),
            contentAlignment = Alignment.CenterStart
        ) {
            Text("Welcome", color = Color.White, fontSize = 24.sp, modifier = Modifier.padding(start = 16.dp))
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(TealHeader, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Pets, contentDescription = null, tint = Color.White, modifier = Modifier.size(60.dp))
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("PetPal Tracker", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(
            "Track meals and feeding reminders for your pet.",
            modifier = Modifier.padding(horizontal = 40.dp, vertical = 8.dp),
            fontSize = 16.sp,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        Button(
            onClick = onGetStarted,
            colors = ButtonDefaults.buttonColors(containerColor = TealHeader),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth(0.6f).height(50.dp)
        ) {
            Text("Get Started")
        }
        Spacer(modifier = Modifier.weight(1.5f))
    }
}

@Composable
fun HomeScreen(
    navController: NavHostController,
    petName: String,
    lastFed: String,
    nextMeal: String
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(TealHeader),
            contentAlignment = Alignment.CenterStart
        ) {
            Text("Home", color = Color.White, fontSize = 24.sp, modifier = Modifier.padding(start = 16.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Surface(
            color = Color(0xFFE0F2F1),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text("Pet: $petName", modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), color = TealHeader)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Last fed", color = Color.Gray, fontSize = 16.sp)
        Text(lastFed, fontSize = 48.sp, fontWeight = FontWeight.Bold)
        Text("Next meal: $nextMeal", color = Color.Gray, fontSize = 16.sp)

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = { navController.navigate("log") },
            colors = ButtonDefaults.buttonColors(containerColor = GreenHeader),
            modifier = Modifier.fillMaxWidth(0.8f).height(55.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Feed Now", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { navController.navigate("add_pet") },
            colors = ButtonDefaults.buttonColors(containerColor = BlueHeader),
            modifier = Modifier.fillMaxWidth(0.8f).height(55.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("+ Add Pet", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { navController.navigate("schedule") },
            colors = ButtonDefaults.buttonColors(containerColor = PurpleHeader),
            modifier = Modifier.fillMaxWidth(0.8f).height(55.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Schedule", fontSize = 18.sp)
        }
    }
}

@Composable
fun AddPetScreen(currentProfile: PetProfile, onSave: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf(currentProfile.name) }
    var type by remember { mutableStateOf(currentProfile.type) }
    var food by remember { mutableStateOf(currentProfile.food) }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(BlueHeader),
            contentAlignment = Alignment.CenterStart
        ) {
            Text("Add Pet", color = Color.White, fontSize = 24.sp, modifier = Modifier.padding(start = 16.dp))
        }
        
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Pet Profile", fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Spacer(modifier = Modifier.height(24.dp))
            Text("Name", color = Color.Gray)
            OutlinedTextField(value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
            Text("Pet Type", color = Color.Gray)
            OutlinedTextField(value = type, onValueChange = { type = it }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
            Text("Food", color = Color.Gray)
            OutlinedTextField(value = food, onValueChange = { food = it }, modifier = Modifier.fillMaxWidth())
            
            Spacer(modifier = Modifier.height(40.dp))
            Button(
                onClick = { onSave(name, type, food) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = BlueHeader),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Pet", fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun ScheduleScreen(
    currentSchedule: List<ScheduleEntry>,
    remindersOn: Boolean,
    onSave: (List<ScheduleEntry>, Boolean) -> Unit
) {
    var reminders by remember { mutableStateOf(remindersOn) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(PurpleHeader),
            contentAlignment = Alignment.CenterStart
        ) {
            Text("Schedule", color = Color.White, fontSize = 24.sp, modifier = Modifier.padding(start = 16.dp))
        }
        
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Feeding Schedule", fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Spacer(modifier = Modifier.height(16.dp))
            
            currentSchedule.forEach { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(entry.name, fontSize = 20.sp)
                    Text(entry.time, fontSize = 20.sp, color = BlueHeader)
                }
                HorizontalDivider()
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Surface(
                color = Color(0xFFF3E5F5),
                shape = RoundedCornerShape(16.dp),
                onClick = { reminders = !reminders }
            ) {
                Text(
                    if (reminders) "Reminders ON" else "Reminders OFF",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    color = PurpleHeader
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            Button(
                onClick = { onSave(currentSchedule, reminders) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PurpleHeader),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Schedule", fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun FeedingLogScreen(onSave: (String, String) -> Unit) {
    var amount by remember { mutableStateOf("1 cup") }
    val currentTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(GreenHeader),
            contentAlignment = Alignment.CenterStart
        ) {
            Text("Feeding Log", color = Color.White, fontSize = 24.sp, modifier = Modifier.padding(start = 16.dp))
        }
        
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Log Feeding", fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Time", color = Color.Gray)
            OutlinedTextField(value = currentTime, onValueChange = {}, modifier = Modifier.fillMaxWidth(), enabled = false)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Amount", color = Color.Gray)
            OutlinedTextField(value = amount, onValueChange = { amount = it }, modifier = Modifier.fillMaxWidth())
            
            Spacer(modifier = Modifier.height(40.dp))
            Button(
                onClick = { onSave(currentTime, amount) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = GreenHeader),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Feeding", fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun HistoryScreen(history: List<FeedingEntry>) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(OrangeHeader),
            contentAlignment = Alignment.CenterStart
        ) {
            Text("History", color = Color.White, fontSize = 24.sp, modifier = Modifier.padding(start = 16.dp))
        }
        
        Column(modifier = Modifier.padding(24.dp)) {
            Text("History & Settings", fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(history) { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(entry.time, fontSize = 18.sp)
                        Text("— ${entry.amount}", fontSize = 18.sp, color = Color.Gray)
                    }
                    HorizontalDivider()
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = OrangeHeader),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Edit Settings", fontSize = 18.sp)
            }
        }
    }
}
