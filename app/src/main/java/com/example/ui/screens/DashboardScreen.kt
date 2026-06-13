package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.managers.SocketManager
import com.example.ui.theme.*
import com.example.ui.components.ScreenTimePanel
import com.example.ui.components.WebSafetyPanel
import com.example.ui.components.SocialMonitorPanel
import com.example.ui.components.LocationRadarPanel
import com.example.ui.components.AdvancedSentinelPanel
import org.json.JSONObject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    roomCode: String,
    onNavigateToLiveStream: () -> Unit,
    onNavigateToFiles: () -> Unit,
    onNavigateToAppBlocker: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var isChildConnected by remember { mutableStateOf(false) }
    var isPhoneLocked by remember { mutableStateOf(false) }
    var showLockDialog by remember { mutableStateOf(false) }
    var showSayDialog by remember { mutableStateOf(false) }

    // Tab state: 0 = Control Hub, 1 = Schedules, 2 = Web Safety, 3 = Social Scans, 4 = Location Radar
    var activeTab by remember { mutableStateOf(0) }

    // Screen Time States
    var dailyAllotmentHours by remember { mutableStateOf(3.0f) }
    var bedtimeStart by remember { mutableStateOf("21:30") }
    var bedtimeEnd by remember { mutableStateOf("06:00") }
    var isBedtimeEnabled by remember { mutableStateOf(true) }
    var isSchoolTimerEnabled by remember { mutableStateOf(true) }
    val appScrollLimits = remember { mutableStateMapOf("TikTok" to 45, "Instagram" to 30, "YouTube" to 60) }

    // Content & Web Safety States
    var blockAdultContent by remember { mutableStateOf(true) }
    var blockViolenceWeapons by remember { mutableStateOf(true) }
    var blockGamblingSites by remember { mutableStateOf(true) }
    var isSafeSearchEnabled by remember { mutableStateOf(true) }
    var isTextScrabbleScanning by remember { mutableStateOf(true) }
    var blockAlternativeBrowsers by remember { mutableStateOf(true) }
    val blacklistDomains = remember { mutableStateListOf("cheatday.com", "cheatmathhomework.net") }
    val whitelistDomains = remember { mutableStateListOf("wikipedia.org", "google.com") }
    val customScrubWords = remember { mutableStateListOf("idiot", "dumbass", "hate", "fight") }

    // Communications & Social Monitoring States
    var enableAIBullyScanner by remember { mutableStateOf(true) }
    var enableGameChatScanner by remember { mutableStateOf(true) }
    var enableMentalAlerts by remember { mutableStateOf(true) }
    var enableNSFWImageBlocker by remember { mutableStateOf(true) }
    val contactWatchlist = remember { mutableStateListOf("+1-404-555-0158", "+1-650-555-9482") }
    val alertLogs = remember { mutableStateListOf(
        JSONObject().apply {
            put("platform", "Discord")
            put("message", "Why are you talking back? You're an idiot, keep away.")
            put("type", "AI Cyberbullying Flag")
            put("time", "5 min ago")
            put("level", "Critical")
        },
        JSONObject().apply {
            put("platform", "Roblox Chat")
            put("message", "Let's meet up at the park after class. Send me your address.")
            put("type", "Predator Contact Alert")
            put("time", "2 hrs ago")
            put("level", "Critical")
        },
        JSONObject().apply {
            put("platform", "Instagram Message")
            put("message", "I hate exams, feel so sad and just want to sleep forever.")
            put("type", "Self-Harm/Depression Warning")
            put("time", "4 hrs ago")
            put("level", "Warning")
        }
    ) }

    // Location Tracker & Security Tools States
    var schoolGeofenceRadius by remember { mutableStateOf(150f) }
    var homeGeofenceRadius by remember { mutableStateOf(100f) }
    var appInstallApprovalPin by remember { mutableStateOf("1948") }
    var isUninstallProtectionEnabled by remember { mutableStateOf(true) }

    // Socket connection check & listeners
    LaunchedEffect(roomCode) {
        SocketManager.connect()
        SocketManager.emitJoinRoom(roomCode, "parent")
        
        launch {
            SocketManager.joinedFlow.collect { event ->
                if (event.roomCode == roomCode) {
                    isChildConnected = true
                }
            }
        }
        
        launch {
            SocketManager.startCallFlow.collect {
                isChildConnected = true
            }
        }

        launch {
            SocketManager.peerDisconnectedFlow.collect { role ->
                if (role == "child") {
                    isChildConnected = false
                }
            }
        }

        // Keep polling socket state occasionally
        while (true) {
            isChildConnected = SocketManager.isConnected()
            delay(3000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "GuardianEye Hub",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isChildConnected) EmeraldGreen.copy(alpha = 0.15f) else CoralRed.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isChildConnected) EmeraldGreen else CoralRed)
                            )
                            Text(
                                text = if (isChildConnected) "Connected" else "Offline",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isChildConnected) EmeraldGreen else CoralRed
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyDark)
            )
        },
        containerColor = NavyDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Room details card
            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = BorderStroke(1.dp, NavySurface2),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PAIRED ROOM CODE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = roomCode,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = ElectricBlue
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "SECURITY LEVEL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Armed & Protected",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreen
                        )
                    }
                }
            }

            // Material 3 Custom Bottom-Pill/Top-Segmented Categorized Navigation TabRow
            ScrollableTabRow(
                selectedTabIndex = activeTab,
                containerColor = NavyDark,
                contentColor = ElectricBlue,
                edgePadding = 0.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                        color = ElectricBlue
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("Main Controls", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    selectedContentColor = ElectricBlue,
                    unselectedContentColor = TextSecondary
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("Screen Time", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.HourglassBottom, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    selectedContentColor = ElectricBlue,
                    unselectedContentColor = TextSecondary
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    text = { Text("Web Safety", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    selectedContentColor = ElectricBlue,
                    unselectedContentColor = TextSecondary
                )
                Tab(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    text = { Text("Social Scans", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Forum, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    selectedContentColor = ElectricBlue,
                    unselectedContentColor = TextSecondary
                )
                Tab(
                    selected = activeTab == 4,
                    onClick = { activeTab = 4 },
                    text = { Text("Radar & Audit", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    selectedContentColor = ElectricBlue,
                    unselectedContentColor = TextSecondary
                )
                Tab(
                    selected = activeTab == 5,
                    onClick = { activeTab = 5 },
                    text = { Text("Sentinel Ops", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.ToggleOn, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    selectedContentColor = ElectricBlue,
                    unselectedContentColor = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Tab Content Router
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (activeTab) {
                    0 -> {
                        // Main Control Dashboard Grid
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Card 1: LIVE STREAMING
                            item {
                                ControlCard(
                                    title = "Live Screen",
                                    description = "Simulate taps & gestures",
                                    icon = Icons.Default.Monitor,
                                    tintColor = ElectricBlue,
                                    isActive = isChildConnected,
                                    testTag = "dashboard_live_screen_card",
                                    onClick = onNavigateToLiveStream
                                )
                            }

                            // Card 2: LOCK DEVICE
                            item {
                                ControlCard(
                                    title = "Block Phone",
                                    description = if (isPhoneLocked) "Locked (Tap to free)" else "Lock screen blocks inputs",
                                    icon = if (isPhoneLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                    tintColor = if (isPhoneLocked) EmeraldGreen else CoralRed,
                                    isActive = isPhoneLocked,
                                    statusText = if (isPhoneLocked) "Locked" else "Ready",
                                    testTag = "dashboard_lock_phone_card",
                                    onClick = {
                                        if (isPhoneLocked) {
                                            // Unlock screen
                                            SocketManager.emitLockScreen(false, "")
                                            isPhoneLocked = false
                                            Toast.makeText(context, "Child device unlocked successfully", Toast.LENGTH_SHORT).show()
                                        } else {
                                            showLockDialog = true
                                        }
                                    }
                                )
                            }

                            // Card 3: FILE EXPLORER
                            item {
                                ControlCard(
                                    title = "File Explorer",
                                    description = "Browse snaps, snaps & docs",
                                    icon = Icons.Default.Folder,
                                    tintColor = VividPurple,
                                    isActive = true,
                                    testTag = "dashboard_file_explorer_card",
                                    onClick = onNavigateToFiles
                                )
                            }

                            // Card 4: NOTIFICATIONS
                            item {
                                ControlCard(
                                    title = "Notifications",
                                    description = "Interweave app notifications",
                                    icon = Icons.Default.Notifications,
                                    tintColor = CoralRed,
                                    isActive = true,
                                    testTag = "dashboard_notifications_card",
                                    onClick = onNavigateToNotifications
                                )
                            }

                            // Card 5: APP BLOCKER
                            item {
                                ControlCard(
                                    title = "App Blocker",
                                    description = "Exempt or blacklist packages",
                                    icon = Icons.Default.AppBlocking,
                                    tintColor = EmeraldGreen,
                                    isActive = true,
                                    testTag = "dashboard_app_block_card",
                                    onClick = onNavigateToAppBlocker
                                )
                            }

                            // Card 6: SAY VOICE / TTS
                            item {
                                ControlCard(
                                    title = "Say / Chat",
                                    description = "Speak or play recorded voice",
                                    icon = Icons.AutoMirrored.Filled.Chat,
                                    tintColor = ElectricBlue,
                                    isActive = true,
                                    testTag = "dashboard_say_card",
                                    onClick = { showSayDialog = true }
                                )
                            }
                        }
                    }
                    1 -> {
                        ScreenTimePanel(
                            allotment = dailyAllotmentHours,
                            onAllotmentChange = { dailyAllotmentHours = it },
                            bedtimeStart = bedtimeStart,
                            onBedtimeStartChange = { bedtimeStart = it },
                            bedtimeEnd = bedtimeEnd,
                            onBedtimeEndChange = { bedtimeEnd = it },
                            isBedtimeEnabled = isBedtimeEnabled,
                            onBedtimeToggle = { isBedtimeEnabled = it },
                            isSchoolEnabled = isSchoolTimerEnabled,
                            onSchoolToggle = { isSchoolTimerEnabled = it },
                            appLimits = appScrollLimits,
                            onAppLimitChange = { appName, mins -> appScrollLimits[appName] = mins }
                        )
                    }
                    2 -> {
                        WebSafetyPanel(
                            blockAdult = blockAdultContent,
                            onBlockAdultChange = { blockAdultContent = it },
                            blockViolence = blockViolenceWeapons,
                            onBlockViolenceChange = { blockViolenceWeapons = it },
                            blockGambling = blockGamblingSites,
                            onBlockGamblingChange = { blockGamblingSites = it },
                            isSafeSearch = isSafeSearchEnabled,
                            onSafeSearchToggle = { isSafeSearchEnabled = it },
                            isTextScrubbing = isTextScrabbleScanning,
                            onTextScrubbingToggle = { isTextScrabbleScanning = it },
                            blockAlternativeBrowsers = blockAlternativeBrowsers,
                            onAlternativeBrowsersToggle = { blockAlternativeBrowsers = it },
                            blacklist = blacklistDomains,
                            onAddBlacklist = { blacklistDomains.add(it) },
                            onRemoveBlacklist = { blacklistDomains.remove(it) },
                            whitelist = whitelistDomains,
                            onAddWhitelist = { whitelistDomains.add(it) },
                            onRemoveWhitelist = { whitelistDomains.remove(it) },
                            scrubWords = customScrubWords,
                            onAddScrubWord = { customScrubWords.add(it) },
                            onRemoveScrubWord = { customScrubWords.remove(it) }
                        )
                    }
                    3 -> {
                        SocialMonitorPanel(
                            enableAI = enableAIBullyScanner,
                            onAIToggle = { enableAIBullyScanner = it },
                            enableGame = enableGameChatScanner,
                            onGameToggle = { enableGameChatScanner = it },
                            enableMental = enableMentalAlerts,
                            onMentalToggle = { enableMentalAlerts = it },
                            enableNSFW = enableNSFWImageBlocker,
                            onNSFWToggle = { enableNSFWImageBlocker = it },
                            watchlist = contactWatchlist,
                            onAddWatchlist = { contactWatchlist.add(it) },
                            onRemoveWatchlist = { contactWatchlist.remove(it) },
                            alerts = alertLogs,
                            onRemoveAlert = { alertLogs.remove(it) }
                        )
                    }
                    4 -> {
                        LocationRadarPanel(
                            schoolRadius = schoolGeofenceRadius,
                            onSchoolRadiusChange = { schoolGeofenceRadius = it },
                            homeRadius = homeGeofenceRadius,
                            onHomeRadiusChange = { homeGeofenceRadius = it },
                            pin = appInstallApprovalPin,
                            onPinChange = { appInstallApprovalPin = it },
                            isUninstallLocked = isUninstallProtectionEnabled,
                            onUninstallLockedChange = { isUninstallProtectionEnabled = it }
                        )
                    }
                    5 -> {
                        AdvancedSentinelPanel()
                    }
                }
            }
        }
    }

    // Modal dialog for Option A / B of LOCK SCREEN
    if (showLockDialog) {
        LockScreenDialog(
            onDismiss = { showLockDialog = false },
            onConfirmLock = { message ->
                isPhoneLocked = true
                SocketManager.emitLockScreen(true, message)
                showLockDialog = false
                Toast.makeText(context, "Lock overlay dispatched to child device", Toast.LENGTH_LONG).show()
            }
        )
    }

    // Modal dialog for Voice message/TTS options
    if (showSayDialog) {
        SayControlDialog(
            onDismiss = { showSayDialog = false },
            onSpeakText = { text ->
                SocketManager.emitSpeakText(text)
                Toast.makeText(context, "Speak instruction: '$text' sent", Toast.LENGTH_SHORT).show()
            },
            onPlayAudioBase64 = { base64Audio ->
                SocketManager.emitPlayAudio(base64Audio)
                Toast.makeText(context, "Voice message sent successfully", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun ControlCard(
    title: String,
    description: String,
    icon: ImageVector,
    tintColor: Color,
    isActive: Boolean,
    statusText: String? = null,
    testTag: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "card_scale"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        border = BorderStroke(1.dp, if (isActive) tintColor else NavySurface2),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .scale(animatedScale)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onClick
            )
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(tintColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tintColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // Active/Inactive status badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isActive) tintColor.copy(alpha = 0.15f) else NavySurface2)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = statusText ?: if (isActive) "Active" else "Ready",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) tintColor else TextSecondary
                    )
                }
            }

            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
fun LockScreenDialog(
    onDismiss: () -> Unit,
    onConfirmLock: (String) -> Unit
) {
    var withMessage by remember { mutableStateOf(true) }
    var messageText by remember { mutableStateOf("GuardianEye: It is time to step away from your device.") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Apply Screen Lock Block",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "A lock request will block touch interactions and draw a black overlay screen dynamically on your child's phone.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(
                        selected = withMessage,
                        onClick = { withMessage = true },
                        label = { Text("Display Message", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricBlue.copy(alpha = 0.2f),
                            selectedLabelColor = ElectricBlue,
                            containerColor = NavySurface,
                            labelColor = TextSecondary
                        )
                    )
                    
                    FilterChip(
                        selected = !withMessage,
                        onClick = { withMessage = false },
                        label = { Text("Just Blackout", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CoralRed.copy(alpha = 0.2f),
                            selectedLabelColor = CoralRed,
                            containerColor = NavySurface,
                            labelColor = TextSecondary
                        )
                    )
                }
                
                if (withMessage) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        label = { Text("Message to kid", color = ElectricBlue) },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = NavySurface,
                            unfocusedContainerColor = NavySurface,
                            focusedIndicatorColor = ElectricBlue,
                            unfocusedIndicatorColor = NavySurface2
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmLock(if (withMessage) messageText else "") },
                colors = ButtonDefaults.buttonColors(containerColor = CoralRed)
            ) {
                Text("Block Now", fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = NavyDark
    )
}

@Composable
fun SayControlDialog(
    onDismiss: () -> Unit,
    onSpeakText: (String) -> Unit,
    onPlayAudioBase64: (String) -> Unit
) {
    val context = LocalContext.current
    var isRecordingTab by remember { mutableStateOf(false) }
    
    // For TTS Option
    var typedWords by remember { mutableStateOf("Dinner is ready! Please join the family.") }
    
    // For Recording Option
    val coroutineScope = rememberCoroutineScope()
    var isRecording by remember { mutableStateOf(false) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var audioFile by remember { mutableStateOf<File?>(null) }
    var recordingDurationSeconds by remember { mutableStateOf(0) }
    
    // Permission launcher for audio recording
    val recordPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (!granted) {
                Toast.makeText(context, "Microphone access is mandatory to record ambient voice.", Toast.LENGTH_LONG).show()
            }
        }
    )

    fun startRecording() {
        try {
            recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            
            val cacheDir = context.cacheDir
            audioFile = File.createTempFile("voice_record_", ".mp3", cacheDir)
            
            // Modern MediaRecorder initialization
            @Suppress("DEPRECATION")
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                MediaRecorder()
            }
            
            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile?.absolutePath)
                prepare()
                start()
            }
            
            mediaRecorder = recorder
            isRecording = true
            recordingDurationSeconds = 0
            
            // Staggered timer
            coroutineScope.launch {
                while (isRecording) {
                    delay(1000)
                    recordingDurationSeconds++
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error starting voice recording: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopAndSendRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            
            val file = audioFile
            if (file != null && file.exists()) {
                val bytes = file.readBytes()
                val base64String = Base64.encodeToString(bytes, Base64.DEFAULT)
                onPlayAudioBase64(base64String)
                file.delete()
            }
            onDismiss()
        } catch (e: Exception) {
            Toast.makeText(context, "Error saving recording: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Voice Broadcasting Portal",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Tab select
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(
                        selected = !isRecordingTab,
                        onClick = { isRecordingTab = false },
                        label = { Text("Text-to-Speech (TTS)", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricBlue.copy(alpha = 0.2f),
                            selectedLabelColor = ElectricBlue,
                            containerColor = NavySurface,
                            labelColor = TextSecondary
                        )
                    )
                    
                    FilterChip(
                        selected = isRecordingTab,
                        onClick = { isRecordingTab = true },
                        label = { Text("Voice Recorder", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VividPurple.copy(alpha = 0.2f),
                            selectedLabelColor = VividPurple,
                            containerColor = NavySurface,
                            labelColor = TextSecondary
                        )
                    )
                }

                if (!isRecordingTab) {
                    // TTS block
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Type messages in English to be spoken aloud on the child's device at 100% volume.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        OutlinedTextField(
                            value = typedWords,
                            onValueChange = { typedWords = it },
                            label = { Text("Message to Say", color = ElectricBlue) },
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = NavySurface,
                                unfocusedContainerColor = NavySurface,
                                focusedIndicatorColor = ElectricBlue,
                                unfocusedIndicatorColor = NavySurface2
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    // Recording block
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Text(
                            text = "Record dynamic voice commands. It is played automatically on maximum volume upon receipt.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        // Recording indicators
                        if (isRecording) {
                            Text(
                                text = "RECORDING NOW...",
                                color = CoralRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format("%02d:%02d", recordingDurationSeconds / 60, recordingDurationSeconds % 60),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                        } else {
                            Text(
                                text = "Press microphone to start recording",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        FloatingActionButton(
                            onClick = {
                                if (isRecording) stopAndSendRecording() else startRecording()
                            },
                            containerColor = if (isRecording) CoralRed else VividPurple,
                            contentColor = TextPrimary,
                            shape = RoundedCornerShape(28.dp),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = "Mic",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isRecordingTab) {
                Button(
                    onClick = {
                        if (typedWords.trim().isNotEmpty()) {
                            onSpeakText(typedWords)
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                ) {
                    Text("Speak Out", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (!isRecording) {
                TextButton(onClick = onDismiss) {
                    Text("Close", color = TextSecondary)
                }
            }
        },
        containerColor = NavyDark
    )
}
