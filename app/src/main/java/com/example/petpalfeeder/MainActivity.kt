package com.example.petpalfeeder

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.core.content.edit
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.petpalfeeder.ui.theme.PetPalFeederTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

// --- Data Models ---
data class FeedingEntry(val date: String, val time: String, val amount: String, val type: String = "Meal")
data class PetProfile(val name: String, val type: String, val breed: String, val birthDate: String, val weight: String)
data class ScheduleEntry(val name: String, val time: String)

// --- Helper Functions ---
fun calculateAge(birthDateString: String, strings: Map<String, String>): String {
    return try {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val birthDate = sdf.parse(birthDateString) ?: return strings["unknown_age"] ?: "Unknown age"
        
        val birthCalendar = Calendar.getInstance()
        birthCalendar.time = birthDate
        
        val today = Calendar.getInstance()
        
        var years = today.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR)
        var months = today.get(Calendar.MONTH) - birthCalendar.get(Calendar.MONTH)
        
        if (today.get(Calendar.DAY_OF_MONTH) < birthCalendar.get(Calendar.DAY_OF_MONTH)) {
            months--
        }
        
        if (months < 0) {
            years--
            months += 12
        }
        
        val yearsPart = if (years > 0) "$years ${if (years == 1) strings["year"] else strings["years"]}" else ""
        val monthsPart = if (months > 0) "$months ${if (months == 1) strings["month"] else strings["months"]}" else ""
        
        when {
            yearsPart.isNotEmpty() && monthsPart.isNotEmpty() -> "$yearsPart, $monthsPart"
            yearsPart.isNotEmpty() -> yearsPart
            monthsPart.isNotEmpty() -> monthsPart
            else -> strings["newborn"] ?: "Newborn"
        }
    } catch (e: Exception) {
        strings["unknown_age"] ?: "Unknown age"
    }
}

// --- Localization ---
val LocalStrings = staticCompositionLocalOf { mapOf<String, String>() }

@Composable
fun t(key: String): String {
    return LocalStrings.current[key] ?: key
}

val enStrings = mapOf(
    "welcome" to "Welcome",
    "petpal" to "PetPal",
    "feeder" to "Feeder",
    "tagline" to "Track meals and feeding reminders for your pet.",
    "get_started" to "Get Started",
    "tracker" to "Tracker",
    "hi_parent" to "Hi, Paw Parent! 👋",
    "happy_pet" to "Let's make sure your pet is happy and well fed.",
    "my_pet" to "My Pet",
    "add_pet" to "Add a Pet",
    "log_meal" to "Log a Meal",
    "today" to "Today",
    "meals_logged" to "Meals Logged",
    "water_logged" to "Water Logged",
    "add_pet_title" to "Add Pet",
    "edit_pet_title" to "Edit Pet",
    "pet_profile" to "Pet Profile",
    "name" to "Name",
    "pet_type" to "Pet Type",
    "dog" to "Dog",
    "cat" to "Cat",
    "breed" to "Breed",
    "birth_date" to "Birth Date",
    "weight" to "Weight",
    "save_pet" to "Save Pet",
    "update_profile" to "Update Profile",
    "weight_hint" to "e.g. 15 lbs",
    "schedule" to "Schedule",
    "feeding_schedule" to "Feeding Schedule",
    "reminders_on" to "Reminders ON",
    "reminders_off" to "Reminders OFF",
    "save_schedule" to "Save Schedule",
    "log_meal_title" to "Log a Meal",
    "edit_entry" to "Edit Entry",
    "meal_time" to "Meal Time",
    "select_dt" to "Select Date & Time...",
    "water_amt" to "Water Amount",
    "food_amt" to "Food Amount",
    "entry_type" to "Entry Type",
    "kibble" to "Kibble",
    "wet_food" to "Wet Food",
    "treat" to "Treat",
    "water" to "Water",
    "save_meal" to "Save Meal",
    "update_entry" to "Update Entry",
    "cup" to "cup",
    "bowl" to "bowl",
    "history" to "History",
    "undo" to "Undo",
    "all" to "All",
    "meals" to "Meals",
    "logs" to "Logs",
    "profile" to "Profile",
    "notes" to "Notes",
    "about" to "About",
    "settings" to "Settings",
    "app_settings" to "App Settings",
    "dark_mode" to "Dark Mode",
    "language" to "Language",
    "home" to "Home",
    "newborn" to "Newborn",
    "unknown_age" to "Unknown age",
    "year" to "year",
    "years" to "years",
    "month" to "month",
    "months" to "months"
)

val arStrings = mapOf(
    "welcome" to "مرحباً",
    "petpal" to "بيت بال",
    "feeder" to "فييدر",
    "tagline" to "تتبع وجبات الطعام وتذكيرات التغذية لحيوانك الأليف.",
    "get_started" to "ابدأ الآن",
    "tracker" to "تراكر",
    "hi_parent" to "مرحباً يا مربي الأليف! 👋",
    "happy_pet" to "لنضمن أن حيوانك الأليف سعيد ويتغذى جيداً.",
    "my_pet" to "حيواني الأليف",
    "add_pet" to "إضافة حيوان أليف",
    "log_meal" to "تسجيل وجبة",
    "today" to "اليوم",
    "meals_logged" to "وجبات مسجلة",
    "water_logged" to "ماء مسجل",
    "add_pet_title" to "إضافة حيوان أليف",
    "edit_pet_title" to "تعديل الملف الشخصي",
    "pet_profile" to "ملف الحيوان الأليف",
    "name" to "الاسم",
    "pet_type" to "نوع الحيوان",
    "dog" to "كلب",
    "cat" to "قطة",
    "breed" to "السلالة",
    "birth_date" to "تاريخ الميلاد",
    "weight" to "الوزن",
    "save_pet" to "حفظ الحيوان",
    "update_profile" to "تحديث الملف",
    "weight_hint" to "مثلاً 15 رطلاً",
    "schedule" to "الجدول",
    "feeding_schedule" to "جدول التغذية",
    "reminders_on" to "التذكيرات مفعلة",
    "reminders_off" to "التذكيرات معطلة",
    "save_schedule" to "حفظ الجدول",
    "log_meal_title" to "تسجيل وجبة",
    "edit_entry" to "تعديل المدخل",
    "meal_time" to "وقت الوجبة",
    "select_dt" to "اختر التاريخ والوقت...",
    "water_amt" to "كمية الماء",
    "food_amt" to "كمية الطعام",
    "entry_type" to "نوع المدخل",
    "kibble" to "طعام جاف",
    "wet_food" to "طعام رطب",
    "treat" to "مكافأة",
    "water" to "ماء",
    "save_meal" to "حفظ الوجبة",
    "update_entry" to "تحديث المدخل",
    "cup" to "كوب",
    "bowl" to "وعاء",
    "history" to "السجل",
    "undo" to "تراجع",
    "all" to "الكل",
    "meals" to "الوجبات",
    "logs" to "السجلات",
    "profile" to "الملف الشخصي",
    "notes" to "ملاحظات",
    "about" to "حول",
    "settings" to "الإعدادات",
    "app_settings" to "إعدادات التطبيق",
    "dark_mode" to "الوضع الداكن",
    "language" to "اللغة",
    "home" to "الرئيسية",
    "newborn" to "حديث الولادة",
    "unknown_age" to "عمر غير معروف",
    "year" to "سنة",
    "years" to "سنوات",
    "month" to "شهر",
    "months" to "شهور"
)

val urStrings = mapOf(
    "welcome" to "خوش آمدید",
    "petpal" to "پیٹ پال",
    "feeder" to "فیڈر",
    "tagline" to "اپنے پالتو جانوروں کے کھانے اور یاد دہانیوں کا ریکارڈ رکھیں۔",
    "get_started" to "شروع کریں",
    "tracker" to "ٹریکر",
    "hi_parent" to "ہیلو پالتو جانوروں کے مالک! 👋",
    "happy_pet" to "آئیے یقینی بنائیں کہ آپ کا پالتو جانور خوش اور اچھی طرح سے کھلایا گیا ہے۔",
    "my_pet" to "میرا پالتو جانور",
    "add_pet" to "پالتو جانور شامل کریں",
    "log_meal" to "کھانا درج کریں",
    "today" to "آج",
    "meals_logged" to "کھانے درج کیے گئے",
    "water_logged" to "پانی درج کیا گیا",
    "add_pet_title" to "پالتو جانور شامل کریں",
    "edit_pet_title" to "پروفائل تبدیل کریں",
    "pet_profile" to "پالتو جانور کا پروفائل",
    "name" to "نام",
    "pet_type" to "پالتو جانور کی قسم",
    "dog" to "کتا",
    "cat" to "بلی",
    "breed" to "نسل",
    "birth_date" to "تاریخ پیدائش",
    "weight" to "وزن",
    "save_pet" to "محفوظ کریں",
    "update_profile" to "پروفائل اپ ڈیٹ کریں",
    "weight_hint" to "مثلاً 15 پاؤنڈ",
    "schedule" to "شیڈول",
    "feeding_schedule" to "کھانے کا شیڈول",
    "reminders_on" to "یاد دہانیاں آن",
    "reminders_off" to "یاد دہانیاں آف",
    "save_schedule" to "شیڈول محفوظ کریں",
    "log_meal_title" to "کھانا درج کریں",
    "edit_entry" to "اندراج تبدیل کریں",
    "meal_time" to "کھانے کا وقت",
    "select_dt" to "تاریخ اور وقت منتخب کریں...",
    "water_amt" to "پانی کی مقدار",
    "food_amt" to "کھانے کی مقدار",
    "entry_type" to "اندراج کی قسم",
    "kibble" to "کبل",
    "wet_food" to "گیلا کھانا",
    "treat" to "ٹریٹ",
    "water" to "پانی",
    "save_meal" to "محفوظ کریں",
    "update_entry" to "اپ ڈیٹ کریں",
    "cup" to "کپ",
    "bowl" to "پیالہ",
    "history" to "تاریخ",
    "undo" to "واپس لیں",
    "all" to "سب",
    "meals" to "کھانے",
    "logs" to "لاگز",
    "profile" to "پروفائل",
    "notes" to "نوٹس",
    "about" to "بارے میں",
    "settings" to "سیٹنگز",
    "app_settings" to "ایپ سیٹنگز",
    "dark_mode" to "ڈارک موڈ",
    "language" to "زبان",
    "home" to "ہوم",
    "newborn" to "نومولود",
    "unknown_age" to "نامعلوم عمر",
    "year" to "سال",
    "years" to "سال",
    "month" to "مہینہ",
    "months" to "مہینے"
)

// --- Color Palette ---
val AppOrange = Color(0xFFE67E50)
val DarkNavy = Color(0xFF242B38)
val SoftOrange = Color(0xFFFFF1EB)
val TealHeader = Color(0xFF242B38) // Updated to match logo's dark navy
val PurpleHeader = Color(0xFF673AB7)
val BlueHeader = Color(0xFF2196F3)
val GreenHeader = Color(0xFF4CAF50)
val OrangeHeader = Color(0xFFFF9800)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val dataManager = remember(context) { DataManager(context) }
            val isDarkMode = remember { mutableStateOf(dataManager.loadDarkMode()) }
            
            PetPalFeederTheme(darkTheme = isDarkMode.value) {
                PetPalTrackerApp(dataManager, isDarkMode)
            }
        }
    }
}

@Composable
fun PetPalTrackerApp(dataManager: DataManager, isDarkModeState: MutableState<Boolean>) {
    val navController = rememberNavController()
    
    // Shared State (Loaded from SharedPreferences)
    val petProfiles = remember { 
        mutableStateListOf<PetProfile>().apply { addAll(dataManager.loadPetProfiles()) }
    }
    var selectedPetIndex by remember { mutableStateOf(dataManager.loadSelectedPetIndex()) }
    val currentPet = if (petProfiles.isNotEmpty()) petProfiles[selectedPetIndex] else PetProfile("None", "Unknown", "Unknown", "0 years", "0 lbs")

    val feedingHistory = remember { 
        val list = mutableStateListOf<FeedingEntry>()
        list.addAll(dataManager.loadFeedingHistory())
        list
    }
    var lastFedTime by remember { 
        mutableStateOf(if (feedingHistory.isNotEmpty()) feedingHistory[0].time else "---") 
    }
    var nextMealTime by remember { mutableStateOf(dataManager.loadNextMealTime()) }
    var schedule by remember { mutableStateOf(dataManager.loadSchedule()) }
    var remindersOn by remember { mutableStateOf(dataManager.loadRemindersOn()) }
    var currentLanguage by remember { mutableStateOf(dataManager.loadLanguage()) }

    val layoutDirection = if (currentLanguage == "Arabic" || currentLanguage == "Urdu") {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }

    val currentStrings = when (currentLanguage) {
        "Arabic" -> arStrings
        "Urdu" -> urStrings
        else -> enStrings
    }

    // Save language when it changes
    LaunchedEffect(currentLanguage) {
        dataManager.saveLanguage(currentLanguage)
    }

    // Save dark mode when it changes
    LaunchedEffect(isDarkModeState.value) {
        dataManager.saveDarkMode(isDarkModeState.value)
    }

    // Save profiles when they change
    LaunchedEffect(petProfiles.toList()) {
        dataManager.savePetProfiles(petProfiles.toList())
    }

    // Save selected index when it changes
    LaunchedEffect(selectedPetIndex) {
        dataManager.saveSelectedPetIndex(selectedPetIndex)
    }

    // Save schedule when it changes
    LaunchedEffect(schedule) {
        dataManager.saveSchedule(schedule)
    }

    // Save next meal time when it changes
    LaunchedEffect(nextMealTime) {
        dataManager.saveNextMealTime(nextMealTime)
    }

    // Save reminders setting when it changes
    LaunchedEffect(remindersOn) {
        dataManager.saveRemindersOn(remindersOn)
    }

    // Save feeding history when it changes (including Undo)
    LaunchedEffect(feedingHistory.toList()) {
        dataManager.saveFeedingHistory(feedingHistory.toList())
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection,
        LocalStrings provides currentStrings
    ) {
        Scaffold(
            bottomBar = {
                if (currentRoute != "welcome" && currentRoute != null) {
                    NavigationBar {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Home, contentDescription = t("home")) },
                            label = { Text(t("home")) },
                            selected = currentRoute == "home",
                            onClick = { navController.navigate("home") }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.History, contentDescription = t("history")) },
                            label = { Text(t("history")) },
                            selected = currentRoute == "history",
                            onClick = { navController.navigate("history") }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Person, contentDescription = t("profile")) },
                            label = { Text(t("profile")) },
                            selected = currentRoute == "profile",
                            onClick = { navController.navigate("profile") }
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
                        petProfiles,
                        selectedPetIndex,
                        onPetSelected = { selectedPetIndex = it },
                        onDeletePet = { index ->
                            if (petProfiles.size > 1) {
                                petProfiles.removeAt(index)
                                if (selectedPetIndex >= petProfiles.size) {
                                    selectedPetIndex = petProfiles.size - 1
                                }
                            }
                        },
                        feedingHistory = feedingHistory
                    ) 
                }
                composable("add_pet") { 
                    AddPetScreen(
                        onSave = { name, type, breed, birthDate, weight -> 
                            petProfiles.add(PetProfile(name, type, breed, birthDate, weight))
                            selectedPetIndex = petProfiles.size - 1
                            navController.popBackStack()
                        }
                    ) 
                }
                composable("edit_pet/{index}") { backStackEntry ->
                    val index = backStackEntry.arguments?.getString("index")?.toIntOrNull() ?: 0
                    if (index < petProfiles.size) {
                        AddPetScreen(
                            initialProfile = petProfiles[index],
                            onSave = { name, type, breed, birthDate, weight ->
                                petProfiles[index] = PetProfile(name, type, breed, birthDate, weight)
                                navController.popBackStack()
                            }
                        )
                    }
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
                        petProfile = currentPet,
                        history = feedingHistory,
                        schedule = schedule,
                        onSave = { date, time, amount, type ->
                            feedingHistory.add(0, FeedingEntry(date, time, amount, type))
                            navController.popBackStack()
                        },
                        onBack = { navController.popBackStack() }
                    ) 
                }
                composable("edit_log/{index}") { backStackEntry ->
                    val index = backStackEntry.arguments?.getString("index")?.toIntOrNull() ?: 0
                    if (index < feedingHistory.size) {
                        FeedingLogScreen(
                            petProfile = currentPet,
                            history = feedingHistory,
                            schedule = schedule,
                            initialEntry = feedingHistory[index],
                            onSave = { date, time, amount, type ->
                                feedingHistory[index] = FeedingEntry(date, time, amount, type)
                                navController.popBackStack()
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
                composable("history") { 
                    HistoryScreen(
                        history = feedingHistory,
                        onUndo = { 
                            if (feedingHistory.isNotEmpty()) {
                                feedingHistory.removeAt(0)
                            }
                        },
                        onEditEntry = { index ->
                            navController.navigate("edit_log/$index")
                        }
                    ) 
                }
                composable("profile") {
                    ProfileScreen(
                        petProfile = currentPet,
                        onNavigateToSchedule = { navController.navigate("schedule") },
                        onEditPet = { navController.navigate("edit_pet/$selectedPetIndex") },
                        isDarkMode = isDarkModeState.value,
                        onDarkModeToggle = { isDarkModeState.value = it },
                        currentLanguage = currentLanguage,
                        onLanguageChange = { currentLanguage = it }
                    )
                }
                composable("settings") {
                    SettingsScreen(
                        isDarkMode = isDarkModeState.value,
                        onDarkModeToggle = { isDarkModeState.value = it },
                        currentLanguage = currentLanguage,
                        onLanguageChange = { currentLanguage = it },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
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
            Text(t("welcome"), color = Color.White, fontSize = 24.sp, modifier = Modifier.padding(start = 16.dp))
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(AppOrange.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Pets, 
                contentDescription = null, 
                tint = AppOrange, 
                modifier = Modifier.size(80.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Row {
            Text(t("petpal") + " ", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = DarkNavy)
            Text(t("feeder"), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = AppOrange)
        }
        Text(
            t("tagline"),
            modifier = Modifier.padding(horizontal = 40.dp, vertical = 8.dp),
            fontSize = 16.sp,
            color = Color.Gray,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        Button(
            onClick = onGetStarted,
            colors = ButtonDefaults.buttonColors(containerColor = AppOrange),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(0.7f).height(56.dp)
        ) {
            Text(t("get_started"), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.weight(1.5f))
    }
}

@Composable
fun HomeScreen(
    navController: NavHostController,
    petProfiles: List<PetProfile>,
    selectedIndex: Int,
    onPetSelected: (Int) -> Unit,
    onDeletePet: (Int) -> Unit,
    feedingHistory: List<FeedingEntry>
) {
    val currentPet = if (petProfiles.isNotEmpty()) petProfiles[selectedIndex] else null
    val todayDate = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date())
    val todayMeals = feedingHistory.filter { it.date == todayDate && it.type != "Water" }
    val todayWater = feedingHistory.filter { it.date == todayDate && it.type == "Water" }
    val strings = LocalStrings.current

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Logo and Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).background(SoftOrange, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Pets, contentDescription = null, tint = AppOrange)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(t("petpal"), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text(t("tracker"), color = AppOrange, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            }
            IconButton(
                onClick = { navController.navigate("settings") },
                modifier = Modifier.background(Color.White, CircleShape).border(1.dp, Color.LightGray, CircleShape)
            ) {
                Icon(Icons.Default.Settings, contentDescription = t("settings"), tint = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(t("hi_parent"), fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(t("happy_pet"), color = Color.Gray, fontSize = 14.sp)

        Spacer(modifier = Modifier.height(24.dp))

        // Pet Card
        Text(t("my_pet"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))
        currentPet?.let { pet ->
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFF0F0F0)),
                modifier = Modifier.fillMaxWidth(),
                onClick = { navController.navigate("edit_pet/$selectedIndex") }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Placeholder for Avatar
                    Box(
                        modifier = Modifier.size(60.dp).background(SoftOrange, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Pets, contentDescription = null, tint = AppOrange, modifier = Modifier.size(30.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(pet.name ?: t("unknown"), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("(${calculateAge(pet.birthDate, strings)})", color = AppOrange, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                        Text(pet.breed ?: t("unknown"), color = Color.Gray, fontSize = 14.sp)
                        Text(pet.birthDate ?: t("unknown"), color = Color.Gray, fontSize = 14.sp)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                }
            }
        } ?: run {
            Button(onClick = { navController.navigate("add_pet") }) {
                Text(t("add_pet"))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { navController.navigate("log") },
            colors = ButtonDefaults.buttonColors(containerColor = AppOrange),
            modifier = Modifier.fillMaxWidth().height(55.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(t("log_meal"), fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(t("today"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SummaryCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Restaurant,
                count = todayMeals.size.toString(),
                label = t("meals_logged")
            )
            SummaryCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.WaterDrop,
                count = todayWater.size.toString(),
                label = t("water_logged"),
                iconColor = Color(0xFF64B5F6)
            )
        }
    }
}

@Composable
fun SummaryCard(modifier: Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector, count: String, label: String, iconColor: Color = AppOrange) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFF0F0F0)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(count, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            }
            Text(label, color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPetScreen(
    initialProfile: PetProfile? = null,
    onSave: (String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialProfile?.name ?: "") }
    var type by remember { mutableStateOf(initialProfile?.type ?: "Dog") }
    var breed by remember { mutableStateOf(initialProfile?.breed ?: "") }
    var birthDate by remember { mutableStateOf(initialProfile?.birthDate ?: "") }
    var weight by remember { mutableStateOf(initialProfile?.weight ?: "") }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            birthDate = dateFormat.format(calendar.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    var typeExpanded by remember { mutableStateOf(false) }
    var breedExpanded by remember { mutableStateOf(false) }

    val petTypes = listOf("Dog", "Cat")
    val dogBreeds = listOf("Golden Retriever", "German Shepherd", "Labrador", "French Bulldog", "Beagle", "Poodle", "Rottweiler", "Yorkshire Terrier", "Bulldog", "Boxer", "Other")
    val catBreeds = listOf("Persian", "Maine Coon", "Siamese", "Ragdoll", "Bengal", "Sphynx", "British Shorthair", "Abyssinian", "Scottish Fold", "Other")
    
    val breeds = if (type == "Dog" || type == t("dog")) dogBreeds else catBreeds

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(AppOrange),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(if (initialProfile == null) t("add_pet_title") else t("edit_pet_title"), color = Color.White, fontSize = 24.sp, modifier = Modifier.padding(start = 16.dp))
        }
        
        Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
            Text(t("pet_profile"), fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(t("name"), color = Color.Gray)
            OutlinedTextField(
                value = name, 
                onValueChange = { name = it }, 
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(t("pet_type"), color = Color.Gray)
            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = !typeExpanded }
            ) {
                OutlinedTextField(
                    value = if (type == "Dog") t("dog") else if (type == "Cat") t("cat") else type,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                    petTypes.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(if (option == "Dog") t("dog") else t("cat")) },
                            onClick = { 
                                if (type != option) {
                                    type = option
                                    breed = "" // Reset breed when type changes
                                }
                                typeExpanded = false 
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text(t("breed"), color = Color.Gray)
            ExposedDropdownMenuBox(
                expanded = breedExpanded,
                onExpandedChange = { breedExpanded = !breedExpanded }
            ) {
                OutlinedTextField(
                    value = breed,
                    onValueChange = { breed = it },
                    modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = breedExpanded) },
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(expanded = breedExpanded, onDismissRequest = { breedExpanded = false }) {
                    breeds.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = { breed = option; breedExpanded = false }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text(t("birth_date"), color = Color.Gray)
            OutlinedTextField(
                value = birthDate,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = { 
                    IconButton(onClick = { datePickerDialog.show() }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null)
                    }
                },
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(t("weight"), color = Color.Gray)
            OutlinedTextField(
                value = weight, 
                onValueChange = { weight = it }, 
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text(t("weight_hint")) }
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            Button(
                onClick = { onSave(name, type, breed, birthDate, weight) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppOrange),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (initialProfile == null) t("save_pet") else t("update_profile"), fontSize = 18.sp)
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
                .background(AppOrange),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(t("schedule"), color = Color.White, fontSize = 24.sp, modifier = Modifier.padding(start = 16.dp))
        }
        
        Column(modifier = Modifier.padding(24.dp)) {
            Text(t("feeding_schedule"), fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Spacer(modifier = Modifier.height(16.dp))
            
            currentSchedule.forEach { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(entry.name ?: t("meals"), fontSize = 20.sp)
                    Text(entry.time ?: "--:--", fontSize = 20.sp, color = BlueHeader)
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
                    if (reminders) t("reminders_on") else t("reminders_off"),
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
                Text(t("save_schedule"), fontSize = 18.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedingLogScreen(
    petProfile: PetProfile?,
    history: List<FeedingEntry>, 
    schedule: List<ScheduleEntry>,
    initialEntry: FeedingEntry? = null,
    onSave: (String, String, String, String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
    
    var amountValue by remember { 
        mutableStateOf(initialEntry?.amount?.replace(" cup", "")?.replace(" bowl", "")?.toFloatOrNull() ?: 1.0f) 
    }
    var logTime by remember { mutableStateOf(initialEntry?.time ?: timeFormat.format(Date())) }
    var logDate by remember { mutableStateOf(initialEntry?.date ?: dateFormat.format(Date())) }
    var foodType by remember { mutableStateOf(initialEntry?.type ?: "Kibble") }

    val calendar = remember { Calendar.getInstance() }

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hour, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            logTime = timeFormat.format(calendar.time)
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        false
    )

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            logDate = dateFormat.format(calendar.time)
            timePickerDialog.show()
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    // Dropdown States
    var dateExpanded by remember { mutableStateOf(false) }
    var typeExpanded by remember { mutableStateOf(false) }

    val foodOptions = listOf("Kibble", "Wet Food", "Treat", "Water")

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (initialEntry == null) t("log_meal_title") else t("edit_entry"), fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Pet Info
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(80.dp).background(SoftOrange, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Pets, contentDescription = null, tint = AppOrange, modifier = Modifier.size(40.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(petProfile?.name ?: t("unknown"), fontWeight = FontWeight.Bold, fontSize = 22.sp)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Meal Time
        Text(t("meal_time"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))
        ExposedDropdownMenuBox(
            expanded = dateExpanded,
            onExpandedChange = { dateExpanded = !dateExpanded }
        ) {
            OutlinedTextField(
                value = "$logDate, $logTime",
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dateExpanded) },
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(expanded = dateExpanded, onDismissRequest = { dateExpanded = false }) {
                DropdownMenuItem(
                    text = { Text(t("select_dt")) },
                    onClick = { datePickerDialog.show(); dateExpanded = false },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) }
                )
                if (schedule.isNotEmpty()) {
                    HorizontalDivider()
                    schedule.forEach { entry ->
                        DropdownMenuItem(
                            text = { Text(t("today") + ", ${entry.time}") },
                            onClick = { 
                                logTime = entry.time ?: logTime
                                logDate = dateFormat.format(Date())
                                dateExpanded = false 
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Food Amount
        Text(if (foodType == "Water") t("water_amt") else t("food_amt"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color.LightGray)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { if (amountValue > 0.25f) amountValue -= 0.25f },
                    modifier = Modifier.size(40.dp).background(SoftOrange, RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = AppOrange)
                }
                
                Text("${if (amountValue % 1 == 0f) amountValue.toInt() else amountValue} ${if (foodType == "Water") t("bowl") else t("cup")}", fontSize = 18.sp)

                IconButton(
                    onClick = { amountValue += 0.25f },
                    modifier = Modifier.size(40.dp).background(SoftOrange, RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase", tint = AppOrange)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Food Type
        Text(t("entry_type"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))
        ExposedDropdownMenuBox(
            expanded = typeExpanded,
            onExpandedChange = { typeExpanded = !typeExpanded }
        ) {
            OutlinedTextField(
                value = when(foodType) {
                    "Kibble" -> t("kibble")
                    "Wet Food" -> t("wet_food")
                    "Treat" -> t("treat")
                    "Water" -> t("water")
                    else -> foodType
                },
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                leadingIcon = { Icon(if (foodType == "Water") Icons.Default.WaterDrop else Icons.Default.Restaurant, contentDescription = null, tint = if (foodType == "Water") Color(0xFF64B5F6) else AppOrange) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                foodOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(when(option) {
                            "Kibble" -> t("kibble")
                            "Wet Food" -> t("wet_food")
                            "Treat" -> t("treat")
                            "Water" -> t("water")
                            else -> option
                        }) },
                        onClick = { 
                            foodType = option
                            typeExpanded = false 
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { 
                val unit = if (foodType == "Water") "bowl" else "cup"
                onSave(logDate, logTime, "${amountValue} $unit", foodType) 
            },
            colors = ButtonDefaults.buttonColors(containerColor = AppOrange),
            modifier = Modifier.fillMaxWidth().height(55.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(if (initialEntry == null) t("save_meal") else t("update_entry"), fontSize = 18.sp)
        }
    }
}

@Composable
fun HistoryScreen(history: List<FeedingEntry>, onUndo: () -> Unit, onEditEntry: (Int) -> Unit) {
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Meals", "Water")

    val filteredHistory = remember(selectedFilter, history) {
        when (selectedFilter) {
            "Meals" -> history.mapIndexed { index, entry -> index to entry }.filter { it.second.type != "Water" }
            "Water" -> history.mapIndexed { index, entry -> index to entry }.filter { it.second.type == "Water" }
            else -> history.mapIndexed { index, entry -> index to entry }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(t("history"), fontWeight = FontWeight.Bold, fontSize = 24.sp)
            if (history.isNotEmpty()) {
                IconButton(onClick = onUndo) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = t("undo"), tint = AppOrange)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            filters.forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(when(filter) {
                        "All" -> t("all")
                        "Meals" -> t("meals")
                        "Water" -> t("water")
                        else -> filter
                    }) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (filter == "Water") Color(0xFF64B5F6) else AppOrange,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(t("logs"), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(filteredHistory) { (originalIndex, entry) ->
                HistoryItem(entry, onClick = { onEditEntry(originalIndex) })
            }
        }
    }
}

@Composable
fun HistoryItem(entry: FeedingEntry, onClick: () -> Unit) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFF0F0F0)),
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).background(SoftOrange, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                val icon = if (entry.type == "Water") Icons.Default.WaterDrop else Icons.Default.Restaurant
                val iconColor = if (entry.type == "Water") Color(0xFF64B5F6) else AppOrange
                Icon(icon, contentDescription = null, tint = iconColor)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(when(entry.type) {
                    "Kibble" -> t("kibble")
                    "Wet Food" -> t("wet_food")
                    "Treat" -> t("treat")
                    "Water" -> t("water")
                    else -> entry.type ?: t("meals")
                }, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("${entry.time ?: "--:--"} • ${entry.amount ?: "--"}", color = Color.Gray, fontSize = 14.sp)
            }
            Icon(Icons.Default.Edit, contentDescription = t("edit_entry"), tint = Color.LightGray, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun ProfileScreen(
    petProfile: PetProfile?,
    onNavigateToSchedule: () -> Unit,
    onEditPet: () -> Unit,
    isDarkMode: Boolean,
    onDarkModeToggle: (Boolean) -> Unit,
    currentLanguage: String,
    onLanguageChange: (String) -> Unit
) {
    val strings = LocalStrings.current
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(48.dp)) // For symmetry
            Text(t("profile"), fontWeight = FontWeight.Bold, fontSize = 24.sp)
            IconButton(onClick = onEditPet) {
                Icon(Icons.Default.Edit, contentDescription = t("edit_pet_title"), tint = AppOrange)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        // Avatar and Info
        Box(
            modifier = Modifier.size(100.dp).background(SoftOrange, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Pets, contentDescription = null, tint = AppOrange, modifier = Modifier.size(50.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(petProfile?.name ?: t("unknown"), fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text("(${calculateAge(petProfile?.birthDate ?: "", strings)})", color = AppOrange, fontSize = 16.sp)
        }
        Text("${petProfile?.breed ?: t("unknown")} • ${petProfile?.birthDate ?: t("unknown")}", color = Color.Gray)

        Spacer(modifier = Modifier.height(32.dp))

        ProfileMenuItem(icon = Icons.Default.Restaurant, label = t("feeding_schedule"), onClick = onNavigateToSchedule)
        ProfileMenuItem(icon = Icons.Default.Description, label = t("notes"), onClick = {})
        ProfileMenuItem(icon = Icons.Default.Favorite, label = t("about") + " ${petProfile?.name ?: t("unknown")}", onClick = {})
    }
}

@Composable
fun ProfileMenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFF0F0F0)),
        color = Color.White
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.Gray)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(label, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isDarkMode: Boolean,
    onDarkModeToggle: (Boolean) -> Unit,
    currentLanguage: String,
    onLanguageChange: (String) -> Unit,
    onBack: () -> Unit
) {
    var langExpanded by remember { mutableStateOf(false) }
    val languages = listOf("English", "Arabic", "Urdu")

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(t("settings"), fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.height(24.dp))

        Text(t("app_settings"), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(t("dark_mode"), fontSize = 16.sp)
            Switch(checked = isDarkMode, onCheckedChange = onDarkModeToggle)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(t("language"), color = Color.Gray)
        ExposedDropdownMenuBox(
            expanded = langExpanded,
            onExpandedChange = { langExpanded = !langExpanded }
        ) {
            OutlinedTextField(
                value = currentLanguage,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = langExpanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(expanded = langExpanded, onDismissRequest = { langExpanded = false }) {
                languages.forEach { language ->
                    DropdownMenuItem(
                        text = { Text(language) },
                        onClick = { onLanguageChange(language); langExpanded = false }
                    )
                }
            }
        }
    }
}

// --- Data Management ---
class DataManager(context: Context) {
    private val dbHelper = DatabaseHelper(context)
    private val prefs = context.getSharedPreferences("PetPalPrefs", Context.MODE_PRIVATE)

    // Profiles
    fun savePetProfiles(profiles: List<PetProfile>) {
        dbHelper.savePetProfiles(profiles)
    }

    fun loadPetProfiles(): List<PetProfile> {
        val profiles = dbHelper.getPetProfiles()
        return if (profiles.isEmpty()) {
            listOf(PetProfile("Name", "Pet", "Breed", "mm/dd/yyyy", "lbs"))
        } else {
            profiles
        }
    }

    fun saveSelectedPetIndex(index: Int) {
        prefs.edit { putInt("selected_pet_index", index) }
    }

    fun loadSelectedPetIndex(): Int {
        return prefs.getInt("selected_pet_index", 0)
    }

    // History
    fun saveFeedingHistory(history: List<FeedingEntry>) {
        dbHelper.saveFeedingHistory(history)
    }

    fun loadFeedingHistory(): List<FeedingEntry> {
        return dbHelper.getFeedingHistory()
    }

    // Schedule
    fun saveSchedule(schedule: List<ScheduleEntry>) {
        dbHelper.saveSchedule(schedule)
    }

    fun loadSchedule(): List<ScheduleEntry> {
        val schedule = dbHelper.getSchedule()
        return if (schedule.isEmpty()) {
            listOf(ScheduleEntry("Breakfast", "8:00 AM"), ScheduleEntry("Dinner", "6:00 PM"))
        } else {
            schedule
        }
    }

    // Settings (Keeping in Prefs for simplicity as they are single values)
    fun saveNextMealTime(time: String) {
        prefs.edit { putString("next_meal_time", time) }
    }

    fun loadNextMealTime(): String {
        return prefs.getString("next_meal_time", "6:00 PM") ?: "6:00 PM"
    }

    fun saveRemindersOn(enabled: Boolean) {
        prefs.edit { putBoolean("reminders_on", enabled) }
    }

    fun loadRemindersOn(): Boolean {
        return prefs.getBoolean("reminders_on", true)
    }

    fun saveDarkMode(enabled: Boolean) {
        prefs.edit { putBoolean("dark_mode", enabled) }
    }

    fun loadDarkMode(): Boolean {
        return prefs.getBoolean("dark_mode", false)
    }

    fun saveLanguage(language: String) {
        prefs.edit { putString("language", language) }
    }

    fun loadLanguage(): String {
        return prefs.getString("language", "English") ?: "English"
    }
}

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private const val DATABASE_NAME = "PetPalFeeder.db"
        private const val DATABASE_VERSION = 1

        // Tables
        private const val TABLE_PETS = "pets"
        private const val TABLE_HISTORY = "history"
        private const val TABLE_SCHEDULE = "schedule"

        // Columns
        private const val COL_ID = "id"
        private const val COL_NAME = "name"
        private const val COL_TYPE = "type"
        private const val COL_BREED = "breed"
        private const val COL_DATE = "date"
        private const val COL_TIME = "time"
        private const val COL_BIRTH = "birthDate"
        private const val COL_WEIGHT = "weight"
        private const val COL_AMOUNT = "amount"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE $TABLE_PETS ($COL_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COL_NAME TEXT, $COL_TYPE TEXT, $COL_BREED TEXT, $COL_BIRTH TEXT, $COL_WEIGHT TEXT)")
        db.execSQL("CREATE TABLE $TABLE_HISTORY ($COL_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COL_DATE TEXT, $COL_TIME TEXT, $COL_AMOUNT TEXT, $COL_TYPE TEXT)")
        db.execSQL("CREATE TABLE $TABLE_SCHEDULE ($COL_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COL_NAME TEXT, $COL_TIME TEXT)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PETS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_HISTORY")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SCHEDULE")
        onCreate(db)
    }

    fun savePetProfiles(profiles: List<PetProfile>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_PETS, null, null)
            profiles.forEach { profile ->
                val values = ContentValues().apply {
                    put(COL_NAME, profile.name)
                    put(COL_TYPE, profile.type)
                    put(COL_BREED, profile.breed)
                    put(COL_BIRTH, profile.birthDate)
                    put(COL_WEIGHT, profile.weight)
                }
                db.insert(TABLE_PETS, null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getPetProfiles(): List<PetProfile> {
        val profiles = mutableListOf<PetProfile>()
        val db = readableDatabase
        val cursor = db.query(TABLE_PETS, null, null, null, null, null, null)
        with(cursor) {
            while (moveToNext()) {
                profiles.add(PetProfile(
                    getString(getColumnIndexOrThrow(COL_NAME)),
                    getString(getColumnIndexOrThrow(COL_TYPE)),
                    getString(getColumnIndexOrThrow(COL_BREED)),
                    getString(getColumnIndexOrThrow(COL_BIRTH)),
                    getString(getColumnIndexOrThrow(COL_WEIGHT))
                ))
            }
            close()
        }
        return profiles
    }

    fun saveFeedingHistory(history: List<FeedingEntry>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_HISTORY, null, null)
            history.forEach { entry ->
                val values = ContentValues().apply {
                    put(COL_DATE, entry.date)
                    put(COL_TIME, entry.time)
                    put(COL_AMOUNT, entry.amount)
                    put(COL_TYPE, entry.type)
                }
                db.insert(TABLE_HISTORY, null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getFeedingHistory(): List<FeedingEntry> {
        val history = mutableListOf<FeedingEntry>()
        val db = readableDatabase
        val cursor = db.query(TABLE_HISTORY, null, null, null, null, null, "$COL_ID DESC")
        with(cursor) {
            while (moveToNext()) {
                history.add(FeedingEntry(
                    getString(getColumnIndexOrThrow(COL_DATE)),
                    getString(getColumnIndexOrThrow(COL_TIME)),
                    getString(getColumnIndexOrThrow(COL_AMOUNT)),
                    getString(getColumnIndexOrThrow(COL_TYPE))
                ))
            }
            close()
        }
        return history
    }

    fun saveSchedule(schedule: List<ScheduleEntry>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_SCHEDULE, null, null)
            schedule.forEach { entry ->
                val values = ContentValues().apply {
                    put(COL_NAME, entry.name)
                    put(COL_TIME, entry.time)
                }
                db.insert(TABLE_SCHEDULE, null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getSchedule(): List<ScheduleEntry> {
        val schedule = mutableListOf<ScheduleEntry>()
        val db = readableDatabase
        val cursor = db.query(TABLE_SCHEDULE, null, null, null, null, null, null)
        with(cursor) {
            while (moveToNext()) {
                schedule.add(ScheduleEntry(
                    getString(getColumnIndexOrThrow(COL_NAME)),
                    getString(getColumnIndexOrThrow(COL_TIME))
                ))
            }
            close()
        }
        return schedule
    }
}
