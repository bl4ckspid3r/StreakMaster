package com.example.streakmaster

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem

import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.shadow

import java.io.InputStream

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size

import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults

import androidx.compose.material3.Icon


import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.geometry.Offset

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.geometry.Size

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StreakMasterApp()
        }
    }
}

// Data class to represent a streak
data class Streak(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val startTime: Long,
    val resetTimes: MutableList<Long> = mutableListOf()
)

// Main app composable with navigation
@Composable
fun StreakMasterApp() {
    // Custom dark color scheme
    val darkColorScheme = darkColorScheme(
        primary = Color(0xFF76FF03), // Green for buttons and accents
        onPrimary = Color.Black,
        background = Color.Black, // Pitch black background
        onBackground = Color.White, // White text on black background
        surface = Color(0xFF1A1A1A), // Dark gray for cards
        onSurface = Color.White, // White text on surface
        secondary = Color(0xFF76FF03), // Green for secondary elements
        onSecondary = Color.Black
    )

    // Navigation items
    val items = listOf(
        Screen.CreateStreak,
        Screen.AllStreaks,
        Screen.Statistics
    )

    // Setup navigation controller
    val navController = rememberNavController()

    // Setup streaks state
    val context = LocalContext.current
    val streaksState = remember { mutableStateOf(loadStreaks(context)) }
    val streaks by streaksState

    // Currently selected streak for details view
    val selectedStreak = remember { mutableStateOf<Streak?>(null) }

    // Define saved view state to know if we're viewing streak details
    val isViewingStreakDetails = remember { mutableStateOf(false) }

    MaterialTheme(colorScheme = darkColorScheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Main content area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = Screen.AllStreaks.route
                    ) {
                        composable(Screen.CreateStreak.route) {
                            CreateStreakScreen(
                                onStreakCreated = { streak ->
                                    val updatedStreaks = streaks.toMutableList()
                                    updatedStreaks.add(streak)
                                    streaksState.value = updatedStreaks
                                    saveStreaks(context, updatedStreaks)
                                    navController.navigate(Screen.AllStreaks.route) {
                                        popUpTo(navController.graph.findStartDestination().id)
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                        composable(Screen.AllStreaks.route) {
                            if (isViewingStreakDetails.value && selectedStreak.value != null) {
                                StreakDetailScreen(
                                    streak = selectedStreak.value!!,
                                    onBackPressed = {
                                        isViewingStreakDetails.value = false
                                    },
                                    onReset = { streak ->
                                        // Find the streak in our list and update it
                                        val updatedStreaks = streaks.toMutableList()
                                        val index = updatedStreaks.indexOfFirst { it.id == streak.id }
                                        if (index != -1) {
                                            updatedStreaks[index] = streak
                                            streaksState.value = updatedStreaks
                                            saveStreaks(context, updatedStreaks)
                                            selectedStreak.value = streak
                                        }
                                    }
                                )
                            } else {
                                AllStreaksScreen(
                                    streaks = streaks,
                                    onStreakSelected = { streak ->
                                        selectedStreak.value = streak
                                        isViewingStreakDetails.value = true
                                    },
                                    onStreakDeleted = { streak ->
                                        val updatedStreaks = streaks.toMutableList()
                                        updatedStreaks.remove(streak)
                                        streaksState.value = updatedStreaks
                                        saveStreaks(context, updatedStreaks)
                                    }
                                )
                            }
                        }
                        composable(Screen.Statistics.route) {
                            StatisticsScreen(
                                streaks = streaks,
                                onExportData = {
                                    exportData(context, streaks)
                                },
                                onImportData = { importedStreaks ->
                                    streaksState.value = importedStreaks
                                    saveStreaks(context, importedStreaks)
                                }
                            )
                        }
                    }
                }

                // Bottom navigation
                NavigationBar(
                    containerColor = Color(0xFF121212),
                    contentColor = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true && !isViewingStreakDetails.value,
                            onClick = {
                                // Reset viewing detail state when changing tabs
                                isViewingStreakDetails.value = false
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF76FF03),
                                selectedTextColor = Color(0xFF76FF03),
                                //indicatorColor = Color(0xFF1A1A1A)
                                //indicatorColor = Color.White,
                                unselectedIconColor = Color.White,
                                unselectedTextColor = Color.White
                            )
                        )
                    }
                }
            }
        }
    }
}

// Navigation screens
sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object CreateStreak : Screen("create_streak", "Create", Icons.Default.Add)
    object AllStreaks : Screen("all_streaks", "Streaks", Icons.Default.DateRange)
    object Statistics : Screen("statistics", "Stats", Icons.Default.BarChart)
}

// Create new streak screen
@Composable
fun CreateStreakScreen(onStreakCreated: (Streak) -> Unit) {
    var streakName by remember { mutableStateOf("") }
    var startDateText by remember { mutableStateOf("Select date") }
    var startTimeText by remember { mutableStateOf("Select time") }
    var startDateMillis by remember { mutableStateOf<Long?>(null) }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Create New Streak",
            fontSize = 24.sp,
            color = Color.White,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Name your streak",
                    color = Color.White,
                    fontSize = 16.sp
                )

                TextField(
                    value = streakName,
                    onValueChange = { streakName = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF262626),
                        unfocusedContainerColor = Color(0xFF262626),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    placeholder = { Text("e.g., NoFap, No Sugar, Meditation") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "When did you start?",
                    color = Color.White,
                    fontSize = 16.sp
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = {
                            val calendar = Calendar.getInstance()
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    calendar.set(year, month, day)
                                    startDateMillis = calendar.timeInMillis
                                    startDateText = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                        .format(calendar.time)
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        //Color(0xFF262626)
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Select date",
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(startDateText)
                        }
                    }

                    Button(
                        onClick = {
                            val calendar = Calendar.getInstance()
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    val selectedCalendar = Calendar.getInstance()
                                    selectedCalendar.set(Calendar.HOUR_OF_DAY, hour)
                                    selectedCalendar.set(Calendar.MINUTE, minute)
                                    startTimeText = SimpleDateFormat("HH:mm", Locale.getDefault())
                                        .format(selectedCalendar.time)
                                },
                                calendar.get(Calendar.HOUR_OF_DAY),
                                calendar.get(Calendar.MINUTE),
                                true
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Select time",
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(startTimeText)
                        }
                    }
                }
            }
        }

        Button(
            onClick = {
                // Combine date and time
                if (streakName.isNotBlank() && startDateMillis != null) {
                    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

                    try {
                        // Parse the selected date
                        val date = dateFormat.parse(startDateText)
                        val dateCalendar = Calendar.getInstance()
                        dateCalendar.time = date!!

                        // Parse the selected time
                        val time = timeFormat.parse(startTimeText)
                        val timeCalendar = Calendar.getInstance()
                        timeCalendar.time = time!!

                        // Combine date and time
                        dateCalendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
                        dateCalendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
                        dateCalendar.set(Calendar.SECOND, 0)

                        val finalTimestamp = dateCalendar.timeInMillis

                        // Create the streak and notify parent
                        val newStreak = Streak(
                            name = streakName,
                            startTime = finalTimestamp
                        )
                        onStreakCreated(newStreak)
                    } catch (e: Exception) {
                        // Handle parsing errors
                        // In a real app, you'd want to show an error message
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF76FF03))
        ) {
            Text(
                text = "Create Streak",
                color = Color.Black,
                fontSize = 16.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

// All streaks list screen
@Composable
fun AllStreaksScreen(
    streaks: List<Streak>,
    onStreakSelected: (Streak) -> Unit,
    onStreakDeleted: (Streak) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf<Streak?>(null) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.background)
    ) {
        Text(
            text = "Your Streaks",
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (streaks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No streaks yet. Create your first streak!",
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn {
                items(streaks) { streak ->
                    val now = System.currentTimeMillis()
                    val lastReset = streak.resetTimes.maxOrNull() ?: streak.startTime
                    val streakStart = max(lastReset, streak.startTime)
                    val diffMillis = now - streakStart
                    val days = diffMillis / (1000 * 60 * 60 * 24)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onStreakSelected(streak) },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = streak.name,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Started: ${
                                            SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                                .format(Date(streak.startTime))
                                        }",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    modifier = Modifier.padding(end = 16.dp)
                                ) {
                                    Text(
                                        text = "$days",
                                        fontSize = 24.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "days",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }

                                IconButton(
                                    onClick = { showDeleteDialog = streak }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Streak",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Delete Confirmation Dialog
        showDeleteDialog?.let { streakToDelete ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text("Delete Streak", color = MaterialTheme.colorScheme.onSurface) },
                text = { Text(
                    "Are you sure you want to delete '${streakToDelete.name}'? This action cannot be undone.",
                    color = MaterialTheme.colorScheme.onSurface
                ) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onStreakDeleted(streakToDelete)
                            showDeleteDialog = null
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error) // Red for delete
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteDialog = null }
                    ) {
                        Text("Cancel", color = MaterialTheme.colorScheme.primary)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            )
        }
    }
}

// Streak detail screen
@Composable
fun StreakDetailScreen(
    streak: Streak,
    onBackPressed: () -> Unit,
    onReset: (Streak) -> Unit
) {
    val startTime = if (streak.resetTimes.isNotEmpty()) {
        streak.resetTimes.maxOrNull() ?: streak.startTime
    } else {
        streak.startTime
    }

    var days by remember { mutableStateOf(0L) }
    var hours by remember { mutableStateOf(0L) }
    var minutes by remember { mutableStateOf(0L) }
    var seconds by remember { mutableStateOf(0L) }

    // Motivational messages
    val context = LocalContext.current
    val motivationalMessages by remember {
        mutableStateOf(loadMotivationalMessages(context))
    }
    val pagerState = rememberPagerState(pageCount = { motivationalMessages.size })

    // Show date and time picker
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackPressed) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                text = streak.name,
                color = Color.White,
                fontSize = 24.sp
            )
            IconButton(onClick = { /* Handle edit */ }) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = Color.White
                )
            }
        }

        // Main Content in a Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Start Date
                Text(
                    text = "Started on ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(streak.startTime))}",
                    color = Color(0xFFB0BEC5),
                    modifier = Modifier.padding(top = 8.dp)
                )

                // If there are resets, show the last reset date
                if (streak.resetTimes.isNotEmpty()) {
                    Text(
                        text = "Last reset: ${
                            SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
                                .format(Date(streak.resetTimes.maxOrNull() ?: 0L))
                        }",
                        color = Color(0xFFB0BEC5),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Streak Display
                Text(
                    text = "Current Streak",
                    color = Color.White,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StreakCounterValue(value = days.toString(), label = "Days")
                    StreakCounterValue(value = hours.toString(), label = "Hours")
                    StreakCounterValue(value = minutes.toString(), label = "Minutes")
                    StreakCounterValue(value = seconds.toString(), label = "Seconds")
                }

                // Reset Button with Date and Time Picker
                Button(
                    onClick = { showDatePicker = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF76FF03)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Reset Counter", color = Color.Black)
                }
            }
        }

        // Swipeable Motivational Cards
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) { page ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
            ) {
                Text(
                    text = motivationalMessages[page],
                    color = Color.White,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                )
            }
        }

        // Pager Dots Indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(motivationalMessages.size) { index ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .padding(horizontal = 2.dp)
                        .background(
                            color = if (pagerState.currentPage == index) Color(0xFF76FF03) else Color.Gray,
                            shape = RoundedCornerShape(4.dp)
                        )
                )
            }
        }
    }

    // Real-time update of the counter
    LaunchedEffect(startTime) {
        while (true) {
            val currentTime = System.currentTimeMillis()
            val diff = currentTime - startTime
            days = diff / (1000 * 60 * 60 * 24)
            hours = (diff / (1000 * 60 * 60)) % 24
            minutes = (diff / (1000 * 60)) % 60
            seconds = (diff / 1000) % 60
            delay(1000)
        }
    }

    // Auto-switch motivational message every minute
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000) // 1 minute delay
            val nextPage = (pagerState.currentPage + 1) % motivationalMessages.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    // Handle date and time pickers
    if (showDatePicker) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, day ->
                calendar.set(year, month, day)
                selectedDateMillis = calendar.timeInMillis
                showDatePicker = false
                showTimePicker = true
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    if (showTimePicker) {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = selectedDateMillis ?: System.currentTimeMillis()
        }
        TimePickerDialog(
            context,
            { _, hour, minute ->
                val updatedCalendar = Calendar.getInstance().apply {
                    timeInMillis = selectedDateMillis ?: System.currentTimeMillis()
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                }

                // Update the streak with the reset time
                val updatedResetTimes = streak.resetTimes.toMutableList()
                updatedResetTimes.add(updatedCalendar.timeInMillis)

                // Update the streak
                val updatedStreak = streak.copy(resetTimes = updatedResetTimes)
                onReset(updatedStreak)

                showTimePicker = false
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }
}


// Enhanced Statistics Screen
@Composable
fun StatisticsScreen(
    streaks: List<Streak>,
    onExportData: () -> Unit,
    onImportData: (List<Streak>) -> Unit
) {
    val context = LocalContext.current
    var showExportMenu by remember { mutableStateOf(false) }

    // File picker for import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val importedStreaks = parseImportedJson(inputStream)
                onImportData(importedStreaks)
            } catch (e: Exception) {
                // In a real app, show an error message to the user
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with export/import options
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Statistics",
                fontSize = 24.sp,
                color = Color.White,
            )

            Box {
                IconButton(onClick = { showExportMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = Color.White
                    )
                }

                DropdownMenu(
                    expanded = showExportMenu,
                    onDismissRequest = { showExportMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Export Data") },
                        onClick = {
                            showExportMenu = false
                            onExportData()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Import Data") },
                        onClick = {
                            showExportMenu = false
                            importLauncher.launch("application/json")
                        }
                    )
                }
            }
        }

        if (streaks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No streak data available yet",
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn {
                items(streaks) { streak ->
                    EnhancedStatisticCard(streak = streak)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedStatisticCard(streak: Streak) {
    // Calculate basic statistics
    val now = System.currentTimeMillis()
    val totalDaysSinceStart = (now - streak.startTime) / (1000 * 60 * 60 * 24)
    val resetCount = streak.resetTimes.size

    val lastReset = streak.resetTimes.maxOrNull() ?: streak.startTime
    val currentStreakStart = max(lastReset, streak.startTime)
    val currentStreakDays = (now - currentStreakStart) / (1000 * 60 * 60 * 24)

    var longestStreak = 0L
    if (streak.resetTimes.isEmpty()) {
        longestStreak = currentStreakDays
    } else {
        val allTimes = mutableListOf<Long>().apply {
            add(streak.startTime)
            addAll(streak.resetTimes)
            add(now)
            sort()
        }
        var maxStreakLength = 0L
        for (i in 0 until allTimes.size - 1) {
            val streakLength = (allTimes[i + 1] - allTimes[i]) / (1000 * 60 * 60 * 24)
            if (streakLength > maxStreakLength) {
                maxStreakLength = streakLength
            }
        }
        longestStreak = maxStreakLength
    }

    val avgStreak = if (resetCount > 0) {
        totalDaysSinceStart / (resetCount + 1)
    } else {
        totalDaysSinceStart
    }

    val timeOptions = listOf("Week", "Month", "Year")
    var selectedTimeOption by remember { mutableStateOf(0) }

    // Format dates for display
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val startDay = dateFormat.format(Date(streak.startTime))
    val lapseDate = if (resetCount > 0) {
        dateFormat.format(Date(lastReset))
    } else {
        "No lapses"
    }

    // Prepare reset periods list
    val allTimes = mutableListOf<Long>().apply {
        add(streak.startTime)
        addAll(streak.resetTimes)
        add(now)
        sort()
    }
    val resetPeriods = mutableListOf<Triple<String, String, Long>>()
    for (i in 0 until allTimes.size - 1) {
        val start = dateFormat.format(Date(allTimes[i]))
        val end = dateFormat.format(Date(allTimes[i + 1]))
        val duration = (allTimes[i + 1] - allTimes[i]) / (1000 * 60 * 60 * 24)
        resetPeriods.add(Triple(start, end, duration))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = streak.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E3C28))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "$currentStreakDays days",
                        fontSize = 16.sp,
                        color = Color(0xFF76FF03)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    StatItem(label = "Longest streak", value = "$longestStreak days")
                    StatItem(label = "Average streak", value = "$avgStreak days")
                }
                Column(modifier = Modifier.weight(1f)) {
                    StatItem(label = "Total resets", value = "$resetCount")
                    StatItem(label = "Days since start", value = "$totalDaysSinceStart days")
                }
            }

            // Streak Timeline Section
            /*
            Text(
                text = "Streak Timeline",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                StatItem(label = "Start Day", value = startDay)
                StatItem(label = "Last Lapse", value = lapseDate)
                StatItem(label = "Total Duration", value = "$totalDaysSinceStart days")
            }*/

            // Reset Periods Section
            if (resetCount > 0) {
                Text(
                    text = "Reset Periods",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 150.dp) // Limit height to avoid overcrowding
                ) {
                    items(resetPeriods.size) { index ->
                        val (start, end, duration) = resetPeriods[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF262626))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Start: $start",
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "End: $end",
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "$duration days",
                                fontSize = 14.sp,
                                color = Color(0xFF76FF03)
                            )
                        }
                    }
                }
            }

            // Streak Observance Section (unchanged)
            /*
            if (streak.resetTimes.isNotEmpty()) {
                Text(
                    text = "Streak Observance",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )

                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    timeOptions.forEachIndexed { index, option ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = timeOptions.size),
                            onClick = { selectedTimeOption = index },
                            selected = index == selectedTimeOption,
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = Color(0xFF76FF03),
                                activeContentColor = Color.Black,
                                inactiveContainerColor = Color(0xFF262626),
                                inactiveContentColor = Color.White
                            )
                        ) {
                            Text(option)
                        }
                    }
                }

                val (observedDays, totalDays) = getObservedDays(streak, timeOptions[selectedTimeOption])

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF262626))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Canvas(
                            modifier = Modifier
                                .size(150.dp)
                        ) {
                            val radius = size.minDimension / 2
                            val center = Offset(size.width / 2, size.height / 2)
                            val total = totalDays.toFloat()
                            val observed = observedDays.toFloat()
                            val observedAngle = if (total > 0) (observed / total) * 360f else 0f

                            drawArc(
                                color = Color(0xFF333333),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = true,
                                topLeft = Offset(center.x - radius, center.y - radius),
                                size = Size(radius * 2, radius * 2)
                            )

                            if (observedAngle > 0) {
                                drawArc(
                                    color = Color(0xFF76FF03),
                                    startAngle = 0f,
                                    sweepAngle = observedAngle,
                                    useCenter = true,
                                    topLeft = Offset(center.x - radius, center.y - radius),
                                    size = Size(radius * 2, radius * 2)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "$observedDays/$totalDays days observed",
                            color = Color.White,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Text(
                    text = "Chart shows the proportion of days the streak was observed",
                    fontSize = 12.sp,
                    color = Color(0xFFB0BEC5),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }*/
        }
    }
}



// Existing helper functions remain unchanged
fun getObservedDays(streak: Streak, timePeriod: String): Pair<Long, Long> {
    val now = System.currentTimeMillis()
    val allTimes = mutableListOf<Long>().apply {
        add(streak.startTime)
        addAll(streak.resetTimes)
        add(now)
        sort()
    }

    val (startTime, totalDays) = when (timePeriod) {
        "Week" -> {
            val weekAgo = Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.DAY_OF_YEAR, -7)
            }.timeInMillis
            Pair(weekAgo, 7L)
        }
        "Month" -> {
            val monthAgo = Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.DAY_OF_YEAR, -30)
            }.timeInMillis
            Pair(monthAgo, 30L)
        }
        "Year" -> {
            val yearAgo = Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.DAY_OF_YEAR, -365)
            }.timeInMillis
            Pair(yearAgo, 365L)
        }
        else -> Pair(streak.startTime, totalDaysSinceStart(streak))
    }

    var observedDays = 0L
    for (i in 0 until allTimes.size - 1) {
        val periodStart = maxOf(allTimes[i], startTime)
        val periodEnd = minOf(allTimes[i + 1], now)
        if (periodEnd > periodStart) {
            val daysInPeriod = (periodEnd - periodStart) / (1000 * 60 * 60 * 24)
            if (periodStart >= startTime) {
                observedDays += daysInPeriod
            }
        }
    }

    val lastReset = streak.resetTimes.maxOrNull() ?: streak.startTime
    if (lastReset < startTime) {
        observedDays = totalDays
    } else if (lastReset >= startTime) {
        val currentStreakStart = maxOf(lastReset, startTime)
        val currentStreakDays = (now - currentStreakStart) / (1000 * 60 * 60 * 24)
        observedDays += currentStreakDays
    }

    observedDays = minOf(observedDays, totalDays)

    return Pair(observedDays, totalDays)
}

fun totalDaysSinceStart(streak: Streak): Long {
    val now = System.currentTimeMillis()
    return (now - streak.startTime) / (1000 * 60 * 60 * 24)
}
// Function to calculate streak lengths (unchanged)
fun getStreakLengthData(streak: Streak, timePeriod: String): List<Long> {
    val now = System.currentTimeMillis()
    val allTimes = mutableListOf<Long>().apply {
        add(streak.startTime)
        addAll(streak.resetTimes)
        add(now) // Add current time to calculate up to now
        sort()
    }

    // Calculate streak lengths (in days) between each reset
    val streakLengths = mutableListOf<Long>()
    for (i in 0 until allTimes.size - 1) {
        val length = (allTimes[i + 1] - allTimes[i]) / (1000 * 60 * 60 * 24)
        streakLengths.add(length)
    }

    // Filter streak lengths based on the time period
    val filteredLengths = mutableListOf<Long>()
    val calendar = Calendar.getInstance()
    val nowCalendar = Calendar.getInstance().apply { timeInMillis = now }

    for (i in 0 until allTimes.size - 1) {
        calendar.timeInMillis = allTimes[i]
        val withinTimePeriod = when (timePeriod) {
            "Week" -> {
                val weekAgo = Calendar.getInstance().apply {
                    timeInMillis = now
                    add(Calendar.DAY_OF_YEAR, -7)
                }
                allTimes[i] >= weekAgo.timeInMillis
            }
            "Month" -> {
                val monthAgo = Calendar.getInstance().apply {
                    timeInMillis = now
                    add(Calendar.MONTH, -1)
                }
                allTimes[i] >= monthAgo.timeInMillis
            }
            "Year" -> {
                val yearAgo = Calendar.getInstance().apply {
                    timeInMillis = now
                    add(Calendar.YEAR, -1)
                }
                allTimes[i] >= yearAgo.timeInMillis
            }
            else -> true
        }

        if (withinTimePeriod) {
            filteredLengths.add(streakLengths[i])
        }
    }

    // Adjust for the selected time period to ensure consistent data points
    return when (timePeriod) {
        "Week" -> {
            // Calculate the streak lengths for each day of the week
            val result = MutableList(7) { 0L }
            val weekAgo = Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.DAY_OF_YEAR, -7)
            }.timeInMillis

            var currentIndex = 0
            for (i in 0 until allTimes.size - 1) {
                if (allTimes[i] >= weekAgo) {
                    val streakLength = streakLengths[i]
                    val startDay = Calendar.getInstance().apply { timeInMillis = allTimes[i] }
                    val endDay = Calendar.getInstance().apply { timeInMillis = allTimes[i + 1] }
                    val daysBetween = (endDay.timeInMillis - startDay.timeInMillis) / (1000 * 60 * 60 * 24)

                    // Map the streak length to the days within the week
                    val startDayOfWeek = startDay.get(Calendar.DAY_OF_WEEK) - 1 // 0 = Sunday, 6 = Saturday
                    val endDayOfWeek = endDay.get(Calendar.DAY_OF_WEEK) - 1
                    val daysSinceWeekStart = ((startDay.timeInMillis - weekAgo) / (1000 * 60 * 60 * 24)).toInt()

                    for (j in 0 until daysBetween.toInt()) {
                        val dayIndex = (startDayOfWeek + j) % 7
                        if (dayIndex in 0..6) {
                            result[dayIndex] = streakLength
                        }
                    }
                }
            }

            // Fill in the current streak for days after the last reset
            val lastResetTime = streak.resetTimes.maxOrNull() ?: streak.startTime
            if (lastResetTime >= weekAgo) {
                val currentStreakDays = ((now - lastResetTime) / (1000 * 60 * 60 * 24)).toInt()
                val lastResetDay = Calendar.getInstance().apply { timeInMillis = lastResetTime }
                val lastResetDayOfWeek = lastResetDay.get(Calendar.DAY_OF_WEEK) - 1
                for (j in 0 until currentStreakDays) {
                    val dayIndex = (lastResetDayOfWeek + j) % 7
                    if (dayIndex in 0..6) {
                        result[dayIndex] = (currentStreakDays - j).toLong()
                    }
                }
            }

            result
        }
        "Month" -> {
            if (filteredLengths.isEmpty()) {
                List(nowCalendar.get(Calendar.DAY_OF_MONTH)) { 0L }
            } else {
                val result = MutableList(nowCalendar.get(Calendar.DAY_OF_MONTH)) { 0L }
                val daysInMonth = (now - allTimes[allTimes.size - filteredLengths.size - 1]) / (1000 * 60 * 60 * 24)
                val startIndex = maxOf(0, nowCalendar.get(Calendar.DAY_OF_MONTH) - daysInMonth.toInt())
                for (i in filteredLengths.indices) {
                    if (i + startIndex < nowCalendar.get(Calendar.DAY_OF_MONTH)) {
                        result[i + startIndex] = filteredLengths[i]
                    }
                }
                result
            }
        }
        "Year" -> {
            if (filteredLengths.isEmpty()) {
                List(12) { 0L }
            } else {
                val result = MutableList(12) { 0L }
                val monthsInYear = (now - allTimes[allTimes.size - filteredLengths.size - 1]) / (1000L * 60 * 60 * 24 * 30)
                val startIndex = maxOf(0, 12 - monthsInYear.toInt())
                for (i in filteredLengths.indices) {
                    if (i + startIndex < 12) {
                        result[i + startIndex] = filteredLengths[i]
                    }
                }
                result
            }
        }
        else -> filteredLengths
    }
}
@Composable
fun StatItem(label: String, value: String) {
    Column(
        modifier = Modifier.padding(vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color(0xFFB0BEC5)
        )
        Text(
            text = value,
            fontSize = 18.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}

// Function to filter reset data by selected time period
fun getFilteredResetData(resetTimes: List<Long>, timePeriod: String): Map<String, Int> {
    val now = System.currentTimeMillis()
    val calendar = Calendar.getInstance()
    val dateFormat = when (timePeriod) {
        "Week" -> SimpleDateFormat("EEE", Locale.getDefault()) // Day of week
        "Month" -> SimpleDateFormat("d", Locale.getDefault()) // Day of month
        "Year" -> SimpleDateFormat("MMM", Locale.getDefault()) // Month
        else -> SimpleDateFormat("d", Locale.getDefault())
    }

    // Filter by time period
    val filteredResets = resetTimes.filter {
        val resetCalendar = Calendar.getInstance().apply { timeInMillis = it }
        val nowCalendar = Calendar.getInstance().apply { timeInMillis = now }

        when (timePeriod) {
            "Week" -> {
                // Reset time is within the last 7 days
                val weekAgo = Calendar.getInstance().apply {
                    timeInMillis = now
                    add(Calendar.DAY_OF_YEAR, -7)
                }
                it >= weekAgo.timeInMillis
            }
            "Month" -> {
                // Reset time is within the current month
                val monthAgo = Calendar.getInstance().apply {
                    timeInMillis = now
                    add(Calendar.MONTH, -1)
                }
                it >= monthAgo.timeInMillis
            }
            "Year" -> {
                // Reset time is within the current year
                val yearAgo = Calendar.getInstance().apply {
                    timeInMillis = now
                    add(Calendar.YEAR, -1)
                }
                it >= yearAgo.timeInMillis
            }
            else -> true
        }
    }

    // Group by formatted date
    val groupedData = filteredResets.groupBy {
        dateFormat.format(Date(it))
    }.mapValues { it.value.size }

    // Sort based on time period
    return when (timePeriod) {
        "Week" -> {
            // Ensure all days of week are represented
            val result = mutableMapOf<String, Int>()
            val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
            val dayNames = (0..6).map { i ->
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_WEEK, -i)
                dayFormat.format(cal.time)
            }.reversed()

            dayNames.forEach { day ->
                result[day] = groupedData[day] ?: 0
            }
            result
        }
        "Month" -> {
            // Ensure all days of month are represented (up to current day)
            val result = mutableMapOf<String, Int>()
            val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

            for (day in 1..currentDay) {
                val dayStr = day.toString()
                result[dayStr] = groupedData[dayStr] ?: 0
            }
            result
        }
        "Year" -> {
            // Ensure all months are represented
            val result = mutableMapOf<String, Int>()
            val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())

            for (i in 0..11) {
                val cal = Calendar.getInstance()
                cal.set(Calendar.MONTH, i)
                val monthStr = monthFormat.format(cal.time)
                result[monthStr] = groupedData[monthStr] ?: 0
            }
            result
        }
        else -> groupedData
    }
}

// Export data function
fun exportData(context: Context, streaks: List<Streak>): Boolean {
    try {
        // Create JSON for export
        val jsonArray = JSONArray()
        streaks.forEach { streak ->
            val jsonObject = JSONObject().apply {
                put("id", streak.id)
                put("name", streak.name)
                put("startTime", streak.startTime)

                // Save reset times
                val resetArray = JSONArray()
                streak.resetTimes.forEach { resetArray.put(it) }
                put("resetTimes", resetArray)
            }
            jsonArray.put(jsonObject)
        }

        // Create file
        val filename = "streak_master_export_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.json"
        val file = File(context.cacheDir, filename)
        file.writeText(jsonArray.toString())

        // Create file URI using FileProvider
        val fileUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        // Create share intent
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, fileUri)
            type = "application/json"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Start the share activity
        context.startActivity(Intent.createChooser(shareIntent, "Export Streak Data"))
        return true
    } catch (e: Exception) {
        // In a real app, show an error message
        return false
    }
}

// Import data function
fun parseImportedJson(inputStream: InputStream?): List<Streak> {
    if (inputStream == null) return emptyList()

    return try {
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        val jsonArray = JSONArray(jsonString)
        val streaks = mutableListOf<Streak>()

        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)

            // Check if the JSON has the required fields
            if (jsonObject.has("id") && jsonObject.has("name") && jsonObject.has("startTime")) {
                val resetTimesArray = if (jsonObject.has("resetTimes")) {
                    jsonObject.getJSONArray("resetTimes")
                } else {
                    JSONArray()
                }

                val resetTimes = mutableListOf<Long>()
                for (j in 0 until resetTimesArray.length()) {
                    resetTimes.add(resetTimesArray.getLong(j))
                }

                streaks.add(
                    Streak(
                        id = jsonObject.getString("id"),
                        name = jsonObject.getString("name"),
                        startTime = jsonObject.getLong("startTime"),
                        resetTimes = resetTimes
                    )
                )
            }
        }

        streaks
    } catch (e: Exception) {
        // Return empty list on error
        emptyList()
    }
}
@Composable
fun StreakCounterValue(value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Text(
            text = value,
            color = Color.White,
            fontSize = 40.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = label,
            color = Color(0xFFB0BEC5),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

// Function to load motivational messages
fun loadMotivationalMessages(context: Context): List<String> {
    return try {
        val inputStream = context.assets.open("motivations.json")
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        val jsonArray = JSONArray(jsonString)
        val messages = mutableListOf<String>()
        for (i in 0 until jsonArray.length()) {
            messages.add(jsonArray.getString(i))
        }
        messages
    } catch (e: Exception) {
        // Fallback motivational messages if file not found
        listOf(
            "Stay strong! Your future self will thank you.",
            "Every day you resist is a victory.",
            "Progress is progress, no matter how small.",
            "You are growing stronger with each passing day.",
            "Remember why you started this journey.",
            "Focus on becoming the best version of yourself.",
            "Your streak is a testament to your willpower.",
            "One day at a time. You've got this!",
            "The longer you go, the easier it becomes.",
            "Your determination is inspiring."
        )
    }
}

// Data persistence functions
fun saveStreaks(context: Context, streaks: List<Streak>) {
    try {
        val jsonArray = JSONArray()
        streaks.forEach { streak ->
            val jsonObject = JSONObject().apply {
                put("id", streak.id)
                put("name", streak.name)
                put("startTime", streak.startTime)

                // Save reset times
                val resetArray = JSONArray()
                streak.resetTimes.forEach { resetArray.put(it) }
                put("resetTimes", resetArray)
            }
            jsonArray.put(jsonObject)
        }

        val file = File(context.filesDir, "streaks.json")
        file.writeText(jsonArray.toString())
    } catch (e: Exception) {
        // Handle error
    }
}

fun loadStreaks(context: Context): List<Streak> {
    try {
        val file = File(context.filesDir, "streaks.json")
        if (!file.exists()) return emptyList()

        val jsonString = file.readText()
        val jsonArray = JSONArray(jsonString)
        val streaks = mutableListOf<Streak>()

        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            val resetTimesArray = jsonObject.getJSONArray("resetTimes")
            val resetTimes = mutableListOf<Long>()

            for (j in 0 until resetTimesArray.length()) {
                resetTimes.add(resetTimesArray.getLong(j))
            }

            streaks.add(
                Streak(
                    id = jsonObject.getString("id"),
                    name = jsonObject.getString("name"),
                    startTime = jsonObject.getLong("startTime"),
                    resetTimes = resetTimes
                )
            )
        }

        return streaks
    } catch (e: Exception) {
        // Return empty list on error
        return emptyList()
    }
}