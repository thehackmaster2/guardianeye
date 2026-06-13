package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import org.json.JSONObject

// ---------------------- TAB 1: SCREEN TIME & SCHEDULE PANEL ----------------------
@Composable
fun ScreenTimePanel(
    allotment: Float,
    onAllotmentChange: (Float) -> Unit,
    bedtimeStart: String,
    onBedtimeStartChange: (String) -> Unit,
    bedtimeEnd: String,
    onBedtimeEndChange: (String) -> Unit,
    isBedtimeEnabled: Boolean,
    onBedtimeToggle: (Boolean) -> Unit,
    isSchoolEnabled: Boolean,
    onSchoolToggle: (Boolean) -> Unit,
    appLimits: Map<String, Int>,
    onAppLimitChange: (String, Int) -> Unit
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // Daily Device Allotments
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = BorderStroke(1.dp, NavySurface2),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Daily Device Allotment", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Cap overall usage hours per day", fontSize = 11.sp, color = TextSecondary)
                        }
                        Text(String.format("%.1f hrs", allotment), fontSize = 16.sp, fontWeight = FontWeight.Black, color = ElectricBlue)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Slider(
                        value = allotment,
                        onValueChange = onAllotmentChange,
                        valueRange = 1.0f..8.0f,
                        steps = 14,
                        colors = SliderDefaults.colors(
                            thumbColor = ElectricBlue,
                            activeTrackColor = ElectricBlue,
                            inactiveTrackColor = NavyDark
                        )
                    )
                }
            }
        }

        // Bedtime & Off-Hour Locks
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = BorderStroke(1.dp, NavySurface2),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Bedtime & Off-Hour Locks", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("No blue light late night", fontSize = 11.sp, color = TextSecondary)
                        }
                        Switch(
                            checked = isBedtimeEnabled,
                            onCheckedChange = onBedtimeToggle,
                            colors = SwitchDefaults.colors(checkedThumbColor = EmeraldGreen, checkedTrackColor = EmeraldGreen.copy(alpha = 0.3f))
                        )
                    }
                    if (isBedtimeEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = bedtimeStart,
                                onValueChange = onBedtimeStartChange,
                                label = { Text("Freeze Device", color = ElectricBlue, fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = TextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedContainerColor = NavyDark, unfocusedContainerColor = NavyDark)
                            )
                            OutlinedTextField(
                                value = bedtimeEnd,
                                onValueChange = onBedtimeEndChange,
                                label = { Text("Wake Access", color = ElectricBlue, fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = TextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedContainerColor = NavyDark, unfocusedContainerColor = NavyDark)
                            )
                        }
                    }
                }
            }
        }

        // School-Time Schedules
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = BorderStroke(1.dp, NavySurface2),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("School-Time Focus Mode", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Blocks social networks during class hour", fontSize = 11.sp, color = TextSecondary)
                        }
                        Switch(
                            checked = isSchoolEnabled,
                            onCheckedChange = onSchoolToggle,
                            colors = SwitchDefaults.colors(checkedThumbColor = EmeraldGreen, checkedTrackColor = EmeraldGreen.copy(alpha = 0.3f))
                        )
                    }
                    if (isSchoolEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(NavyDark)
                                .padding(10.dp)
                        ) {
                            Text(
                                "Locked Schedule: Mon - Fri (08:00 AM - 03:00 PM) is fully restrictive.",
                                fontSize = 11.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }

        // App-Specific Limits Countdown
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = BorderStroke(1.dp, NavySurface2),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("App-Specific Daily Timers", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Prevent infinite doom-scrolling", fontSize = 11.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(10.dp))

                    appLimits.forEach { (appName, minutes) ->
                        var localMin by remember { mutableStateOf(minutes.toFloat()) }
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(appName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("${localMin.toInt()} min limit", fontSize = 12.sp, color = ElectricBlue)
                            }
                            Slider(
                                value = localMin,
                                onValueChange = { localMin = it },
                                valueRange = 15f..120f,
                                steps = 6,
                                colors = SliderDefaults.colors(thumbColor = VividPurple, activeTrackColor = VividPurple, inactiveTrackColor = NavyDark),
                                onValueChangeFinished = {
                                    onAppLimitChange(appName, localMin.toInt())
                                    Toast.makeText(context, "$appName limit configured successfully", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------- TAB 2: CONTENT & WEB SAFETY PANEL ----------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WebSafetyPanel(
    blockAdult: Boolean,
    onBlockAdultChange: (Boolean) -> Unit,
    blockViolence: Boolean,
    onBlockViolenceChange: (Boolean) -> Unit,
    blockGambling: Boolean,
    onBlockGamblingChange: (Boolean) -> Unit,
    isSafeSearch: Boolean,
    onSafeSearchToggle: (Boolean) -> Unit,
    isTextScrubbing: Boolean,
    onTextScrubbingToggle: (Boolean) -> Unit,
    blockAlternativeBrowsers: Boolean,
    onAlternativeBrowsersToggle: (Boolean) -> Unit,
    blacklist: List<String>,
    onAddBlacklist: (String) -> Unit,
    onRemoveBlacklist: (String) -> Unit,
    whitelist: List<String>,
    onAddWhitelist: (String) -> Unit,
    onRemoveWhitelist: (String) -> Unit,
    scrubWords: List<String>,
    onAddScrubWord: (String) -> Unit,
    onRemoveScrubWord: (String) -> Unit
) {
    var domainText by remember { mutableStateOf("") }
    var wordText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // Toggles List
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = BorderStroke(1.dp, NavySurface2),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Automated Filtering Categories", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(10.dp))

                    WebSafetyRow("Block Adult Content / Pornography", blockAdult, onBlockAdultChange)
                    WebSafetyRow("Block Violent / Dangerous Genres", blockViolence, onBlockViolenceChange)
                    WebSafetyRow("Block Gambling / Casinos / Betting", blockGambling, onBlockGamblingChange)
                    
                    Divider(color = NavySurface2, modifier = Modifier.padding(vertical = 10.dp))
                    
                    WebSafetyRow("Lock Search Engines to strict SafeSearch", isSafeSearch, onSafeSearchToggle)
                    WebSafetyRow("Dynamic Screen Profanity/Scrub Scanner", isTextScrubbing, onTextScrubbingToggle)
                    WebSafetyRow("Restrict Obscure third-party browsers", blockAlternativeBrowsers, onAlternativeBrowsersToggle)
                }
            }
        }

        // Whitelist / Blacklist
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = BorderStroke(1.dp, NavySurface2),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Niche Domain Filters", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Block or authorize raw hostnames", fontSize = 11.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = domainText,
                        onValueChange = { domainText = it },
                        placeholder = { Text("e.g. unapprovedsite.com", color = TextSecondary) },
                        colors = TextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedContainerColor = NavyDark, unfocusedContainerColor = NavyDark),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (domainText.trim().isNotEmpty()) {
                                    onAddWhitelist(domainText.trim())
                                    domainText = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Allow Host", fontSize = 11.sp)
                        }
                        Button(
                            onClick = {
                                if (domainText.trim().isNotEmpty()) {
                                    onAddBlacklist(domainText.trim())
                                    domainText = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CoralRed),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Block Host", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Blacklisted Sites:", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 12.sp)
                    FlowRow(modifier = Modifier.fillMaxWidth()) {
                        blacklist.forEach { host ->
                            AssistChip(
                                onClick = { onRemoveBlacklist(host) },
                                label = { Text(host, color = CoralRed, fontSize = 11.sp) },
                                trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(12.dp), tint = CoralRed) }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Whitelisted Sites:", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 12.sp)
                    FlowRow(modifier = Modifier.fillMaxWidth()) {
                        whitelist.forEach { host ->
                            AssistChip(
                                onClick = { onRemoveWhitelist(host) },
                                label = { Text(host, color = EmeraldGreen, fontSize = 11.sp) },
                                trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(12.dp), tint = EmeraldGreen) }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                    }
                }
            }
        }

        // Profanity scrub word list
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = BorderStroke(1.dp, NavySurface2),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Screen-Text Keyword Scrubber", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = wordText,
                            onValueChange = { wordText = it },
                            placeholder = { Text("Add custom target word", color = TextSecondary) },
                            colors = TextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedContainerColor = NavyDark, unfocusedContainerColor = NavyDark),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                if (wordText.trim().isNotEmpty()) {
                                    onAddScrubWord(wordText.trim())
                                    wordText = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VividPurple)
                        ) {
                            Text("Add")
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(modifier = Modifier.fillMaxWidth()) {
                        scrubWords.forEach { word ->
                            AssistChip(
                                onClick = { onRemoveScrubWord(word) },
                                label = { Text(word, color = TextPrimary, fontSize = 11.sp) },
                                trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(12.dp), tint = TextSecondary) }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WebSafetyRow(label: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextPrimary, fontSize = 13.sp)
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = EmeraldGreen, checkedTrackColor = EmeraldGreen.copy(alpha = 0.3f))
        )
    }
}

// ---------------------- TAB 3: COMMUNICATIONS & SOCIAL SCANS ----------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SocialMonitorPanel(
    enableAI: Boolean,
    onAIToggle: (Boolean) -> Unit,
    enableGame: Boolean,
    onGameToggle: (Boolean) -> Unit,
    enableMental: Boolean,
    onMentalToggle: (Boolean) -> Unit,
    enableNSFW: Boolean,
    onNSFWToggle: (Boolean) -> Unit,
    watchlist: List<String>,
    onAddWatchlist: (String) -> Unit,
    onRemoveWatchlist: (String) -> Unit,
    alerts: List<JSONObject>,
    onRemoveAlert: (JSONObject) -> Unit
) {
    var phoneInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // Toggles
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = BorderStroke(1.dp, NavySurface2),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Chat, Messaging & Call Scans", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(10.dp))

                    WebSafetyRow("AI Cyberbullying scanner logs", enableAI, onAIToggle)
                    WebSafetyRow("Roblox & Discord voice/text scan", enableGame, onGameToggle)
                    WebSafetyRow("Self-Harm / Mental tracking interceptor", enableMental, onMentalToggle)
                    WebSafetyRow("Sexting & Nude Image intercept logic", enableNSFW, onNSFWToggle)
                }
            }
        }

        // Phone watchdog list
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = BorderStroke(1.dp, NavySurface2),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Contact Phone Watchdog", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Alert parents if matching logs are sent/received", fontSize = 11.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it },
                            placeholder = { Text("e.g. +1-555-0100", color = TextSecondary) },
                            colors = TextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedContainerColor = NavyDark, unfocusedContainerColor = NavyDark),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                if (phoneInput.trim().isNotEmpty()) {
                                    onAddWatchlist(phoneInput.trim())
                                    phoneInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VividPurple)
                        ) {
                            Text("Track")
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(modifier = Modifier.fillMaxWidth()) {
                        watchlist.forEach { phone ->
                            AssistChip(
                                onClick = { onRemoveWatchlist(phone) },
                                label = { Text(phone, color = ElectricBlue, fontSize = 11.sp) },
                                trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(12.dp), tint = TextSecondary) }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                    }
                }
            }
        }

        // Live Alerts Feed Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Preeminent NLP Alerts", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(CoralRed.copy(alpha = 0.2f)).padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("${alerts.size} alerts flagged", color = CoralRed, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (alerts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().background(NavySurface, RoundedCornerShape(12.dp)).padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No threatening logs scanned so far", color = EmeraldGreen, fontSize = 11.sp)
                }
            }
        } else {
            items(alerts) { alert ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = NavySurface),
                    border = BorderStroke(1.dp, if (alert.optString("level") == "Critical") CoralRed else VividPurple),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(if (alert.optString("level") == "Critical") CoralRed.copy(alpha = 0.2f) else VividPurple.copy(alpha = 0.2f)).padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(alert.optString("level"), color = if (alert.optString("level") == "Critical") CoralRed else VividPurple, fontSize = 8.sp, fontWeight = FontWeight.Black)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(alert.optString("platform"), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Text(alert.optString("time"), fontSize = 10.sp, color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("\"${alert.optString("message")}\"", fontSize = 12.sp, color = TextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Category: " + alert.optString("type"), fontSize = 10.sp, color = TextSecondary)
                            TextButton(onClick = { onRemoveAlert(alert) }, colors = ButtonDefaults.textButtonColors(contentColor = CoralRed)) {
                                Text("Acknowledge", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------- TAB 4: GPS MAP TRACKER & DATA AUDIT CODES ----------------------
@Composable
fun LocationRadarPanel(
    schoolRadius: Float,
    onSchoolRadiusChange: (Float) -> Unit,
    homeRadius: Float,
    onHomeRadiusChange: (Float) -> Unit,
    pin: String,
    onPinChange: (String) -> Unit,
    isUninstallLocked: Boolean,
    onUninstallLockedChange: (Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // High Contrast Cyberpunk Canvas Geo Radar map
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NavyDark),
                border = BorderStroke(1.dp, NavySurface2),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(200.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CanvasRadar(schoolRadius, homeRadius)
                    
                    // Stats overlay
                    Column(
                        modifier = Modifier.align(Alignment.TopStart).padding(10.dp).background(NavyDark.copy(alpha = 0.8f), RoundedCornerShape(6.dp)).padding(6.dp)
                    ) {
                        Text("GPS: ONLINE", fontSize = 9.sp, color = EmeraldGreen, fontWeight = FontWeight.Bold)
                        Text("Lat: 40.7484° N", fontSize = 8.sp, color = TextPrimary)
                        Text("Lng: -73.9857° W", fontSize = 8.sp, color = TextPrimary)
                        Text("Accuracy: 3m radius", fontSize = 8.sp, color = TextSecondary)
                    }

                    Box(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp).background(EmeraldGreen.copy(alpha = 0.2f), RoundedCornerShape(6.dp)).border(1.dp, EmeraldGreen, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("In Geofence: School", color = EmeraldGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Geofences Sliders
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = BorderStroke(1.dp, NavySurface2),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Geofence Zone Radius", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("School Perimeter Area", color = TextPrimary, fontSize = 12.sp)
                        Text("${schoolRadius.toInt()}m", color = ElectricBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(value = schoolRadius, onValueChange = onSchoolRadiusChange, valueRange = 50f..500f, colors = SliderDefaults.colors(thumbColor = ElectricBlue, activeTrackColor = ElectricBlue, inactiveTrackColor = NavyDark))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Home Perimeter Area", color = TextPrimary, fontSize = 12.sp)
                        Text("${homeRadius.toInt()}m", color = VividPurple, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(value = homeRadius, onValueChange = onHomeRadiusChange, valueRange = 50f..500f, colors = SliderDefaults.colors(thumbColor = VividPurple, activeTrackColor = VividPurple, inactiveTrackColor = NavyDark))
                }
            }
        }

        // Install blocking / Uninstall prevention block
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = BorderStroke(1.dp, NavySurface2),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Anti-Uninstall Protection", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Prevent disabling client services", color = TextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = isUninstallLocked,
                            onCheckedChange = onUninstallLockedChange,
                            colors = SwitchDefaults.colors(checkedThumbColor = EmeraldGreen, checkedTrackColor = EmeraldGreen.copy(alpha = 0.3f))
                        )
                    }
                    Divider(color = NavySurface2, modifier = Modifier.padding(vertical = 10.dp))
                    OutlinedTextField(
                        value = pin,
                        onValueChange = onPinChange,
                        label = { Text("App Store download approved PIN", color = ElectricBlue) },
                        colors = TextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedContainerColor = NavyDark, unfocusedContainerColor = NavyDark),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Custom drawn web spent charts
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = BorderStroke(1.dp, NavySurface2),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Activity & spent bytes audit logs", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Weekly tracking diagnostics", fontSize = 11.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(12.dp))
                    WeeklyBarsProgress()
                }
            }
        }
    }
}

// ---------------------- TAB 5: ADVANCED SENTINEL HUB (40 ADVANCED CONTROLS) ----------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdvancedSentinelPanel() {
    val context = LocalContext.current
    var selectedCategoryIndex by remember { mutableStateOf(0) }
    
    val categories = listOf(
        "🛡️ Tamper-Proof" to "Hardware & System level protections",
        "🕵️ Stealth Cap" to "Activity capture & discrete monitoring",
        "🧠 Env Sensors" to "Microphone, camera & physical sensors",
        "💬 Soc Hack-Ops" to "Social network DM intercepts & panic",
        "🔒 Sandboxing" to "Interface blocks & custom user profiles",
        "🚫 Privacy Shld" to "Notification masking & clipboard cleaner",
        "⚙️ Hardening" to "System settings & sideload APK blocks",
        "📡 Telemetry" to "Remote session tracking & alarm triggers"
    )

    // Interactive simulated data states
    var keylogs = remember { mutableStateListOf(
        "09:12 AM - YouTube Search: \"safest ways to jump high\"",
        "09:15 AM - Discord DM: \"whats your homework answer guy?\"",
        "09:30 AM - Google Search: \"how to disable parental apps\""
    ) }
    var recoveredTexts = remember { mutableStateListOf<String>() }
    var fakeApps = remember { mutableStateListOf<String>() }
    var screenMirroringOn by remember { mutableStateOf(false) }
    var showScreenshot by remember { mutableStateOf(false) }
    var decibelValue by remember { mutableStateOf(42f) }
    var mockSmsAlerts = remember { mutableStateListOf(
        "Daughter DM Intercept: \"We're sneaking out at 10 PM. Don't tell.\"",
        "Discord Alert: \"Hey send me some pics\" (Bully intent)"
    ) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Category Selector Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEachIndexed { index, pair ->
                FilterChip(
                    selected = selectedCategoryIndex == index,
                    onClick = { selectedCategoryIndex = index },
                    label = { Text(pair.first, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricBlue.copy(alpha = 0.25f),
                        selectedLabelColor = ElectricBlue,
                        containerColor = NavySurface,
                        labelColor = TextSecondary
                    )
                )
            }
        }

        Divider(color = NavySurface2, modifier = Modifier.padding(bottom = 12.dp))

        // Selected Category Content Panel
        Card(
            colors = CardDefaults.cardColors(containerColor = NavySurface),
            border = BorderStroke(1.dp, NavySurface2),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                item {
                    Column {
                        Text(
                            text = categories[selectedCategoryIndex].first.uppercase(),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = ElectricBlue,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = categories[selectedCategoryIndex].second,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Render matching 5 items based on index
                when (selectedCategoryIndex) {
                    0 -> { // Tamper-Proof
                        item {
                            HardwareControlRow(
                                title = "VPN Blockers",
                                subtitle = "Instantly terminates unauthorized Virtual Private Network tunnels",
                                defaultState = true
                            )
                        }
                        item {
                            HardwareControlRow(
                                title = "Factory Reset Protection",
                                subtitle = "Hardware-bound lock blocking system wipes from settings menu",
                                defaultState = true
                            )
                        }
                        item {
                            HardwareControlRow(
                                title = "Disable Private / Incognito",
                                subtitle = "Locks private browser tabs in Google Chrome & Opera",
                                defaultState = true
                            )
                        }
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = NavyDark),
                                border = BorderStroke(1.dp, NavySurface2),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("App Name Disguise Detection", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text("Scans for clone calculaters hiding vaults", fontSize = 11.sp, color = TextSecondary)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            fakeApps.clear()
                                            fakeApps.add("xCalculator_Vault_v2.apk (Disguised Gallery)")
                                            fakeApps.add("SecretNotePadClone.bin (Secure DB)")
                                            Toast.makeText(context, "Scrape complete: 2 disguised binaries located", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = VividPurple),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Scrape Hidden Packages", fontSize = 12.sp)
                                    }
                                    if (fakeApps.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        fakeApps.forEach { appName ->
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Warning, null, tint = CoralRed, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(appName, color = CoralRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            HardwareControlRow(
                                title = "Safe Mode Enforcement",
                                subtitle = "Block cold booting into Android Safemode troubleshooting",
                                defaultState = true
                            )
                        }
                    }
                    1 -> { // Stealth Cap
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = NavyDark),
                                border = BorderStroke(1.dp, NavySurface2),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Live Screen Mirroring", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            Text("Stealth video stream overlay", fontSize = 11.sp, color = TextSecondary)
                                        }
                                        Switch(
                                            checked = screenMirroringOn,
                                            onCheckedChange = { screenMirroringOn = it },
                                            colors = SwitchDefaults.colors(checkedThumbColor = EmeraldGreen, checkedTrackColor = EmeraldGreen.copy(alpha = 0.3f))
                                        )
                                    }
                                    if (screenMirroringOn) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(120.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.Black.copy(alpha = 0.8f))
                                                .border(1.dp, EmeraldGreen, RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "🎥 SCREEN MIRROR STREAMING [REAL-TIME]",
                                                color = EmeraldGreen,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = NavyDark),
                                border = BorderStroke(1.dp, NavySurface2),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Remote Screenshot Capture", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text("Keyword triggered or user on-demand snaps", fontSize = 11.sp, color = TextSecondary)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                showScreenshot = true
                                                Toast.makeText(context, "Screenshot updated from peer device", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Trigger Capture", fontSize = 11.sp)
                                        }
                                        if (showScreenshot) {
                                            Button(
                                                onClick = { showScreenshot = false },
                                                colors = ButtonDefaults.buttonColors(containerColor = NavySurface),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Clear", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                    if (showScreenshot) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(100.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(NavySurface)
                                                .padding(10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(Icons.Default.Photo, null, tint = ElectricBlue, modifier = Modifier.size(24.dp))
                                                Text("MOCK_CHILD_SCREEN_SHOT_UTC_1038.PNG", color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                Text("Current activity: Chatting on Instagram", color = TextSecondary, fontSize = 8.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = NavyDark),
                                border = BorderStroke(1.dp, NavySurface2),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Keystroke Logging Feed", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text("Key logger interceptor diagnostics logs", fontSize = 11.sp, color = TextSecondary)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        keylogs.forEach { log ->
                                            Text(log, color = EmeraldGreen, fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = NavyDark),
                                border = BorderStroke(1.dp, NavySurface2),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Dual-Side Call Audio Recording", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text("Simultaneous inbound & outbound tracking", fontSize = 11.sp, color = TextSecondary)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(NavySurface)
                                            .padding(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("Recorded Call: Dad (+1-555-401)", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                Text("Duration: 1m 45s", color = TextSecondary, fontSize = 9.sp)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(onClick = { Toast.makeText(context, "Streaming audio clip...", Toast.LENGTH_SHORT).show() }) {
                                                    Icon(Icons.Default.PlayArrow, null, tint = EmeraldGreen)
                                                }
                                                Icon(Icons.Default.CloudDownload, null, tint = ElectricBlue, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            HardwareControlRow(
                                title = "Hidden Background Mode",
                                subtitle = "Invisibly runs daemon control services behind stock operating system GUI",
                                defaultState = true
                            )
                        }
                    }
                    2 -> { // Env Sensors
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = NavyDark),
                                border = BorderStroke(1.dp, NavySurface2),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Remote Ambient Listening", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text("Triggers ambient microphone silently", fontSize = 11.sp, color = TextSecondary)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            onClick = {
                                                decibelValue = (30..85).random().toFloat()
                                                Toast.makeText(context, "Sound scanner calibrated", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = VividPurple)
                                        ) {
                                            Text("Recalibrate Decibels", fontSize = 11.sp)
                                        }
                                        Text("Noise level: ${decibelValue.toInt()} dB", color = ElectricBlue, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                        }
                        item {
                            HardwareControlRow(
                                title = "Remote Camera Activation",
                                subtitle = "Fires front/rear matrix sensors with blank screen display limits",
                                defaultState = false
                            )
                        }
                        item {
                            HardwareControlRow(
                                title = "Battery Depletion Tracking",
                                subtitle = "Critical parent telemetry alert dispatched standard at 15% threshold",
                                defaultState = true
                            )
                        }
                        item {
                            HardwareControlRow(
                                title = "SIM Card Swap Alerts",
                                subtitle = "Sends SMS fallback directly to parent line if ICCID card register changes",
                                defaultState = true
                            )
                        }
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = NavyDark),
                                border = BorderStroke(1.dp, NavySurface2),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Deleted History Cache Scrape", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text("Scrape SQLite memory maps for deleted search traces", fontSize = 11.sp, color = TextSecondary)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            recoveredTexts.clear()
                                            recoveredTexts.add("SMS [DELETED] - \"hey sneak out tonight, tell no one\"")
                                            recoveredTexts.add("Search [DELETED] - \"how to hide apps on android device\"")
                                            Toast.makeText(context, "Found 2 erased database items", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Query Erased Records Database", fontSize = 12.sp)
                                    }
                                    if (recoveredTexts.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        recoveredTexts.forEach { rTxt ->
                                            Text(rTxt, color = CoralRed, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    3 -> { // Soc Hack-Ops
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = NavyDark),
                                border = BorderStroke(1.dp, NavySurface2),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Direct Message Interception Dashboard", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text("Pulling real-time notifications on social apps", fontSize = 11.sp, color = TextSecondary)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        mockSmsAlerts.forEach { alert ->
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(NavySurface)
                                                    .padding(8.dp)
                                            ) {
                                                Text(alert, color = TextPrimary, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            HardwareControlRow(
                                title = "Profile Creation Block",
                                subtitle = "Intersects dynamic loading configurations on registration domains",
                                defaultState = true
                            )
                        }
                        item {
                            HardwareControlRow(
                                title = "Search Intent AI Analysis",
                                subtitle = "Runs local small NLP models to evaluate danger gradients of search hooks",
                                defaultState = true
                            )
                        }
                        item {
                            HardwareControlRow(
                                title = "Follower Account Monitoring",
                                subtitle = "Spam flags parent dashboard if high-intensity bot user adds ward",
                                defaultState = true
                            )
                        }
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = NavyDark),
                                border = BorderStroke(1.dp, NavySurface2),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Panic SOS Distress Button", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text("Child 1-tap beacon broadcasting loop", fontSize = 11.sp, color = TextSecondary)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            Toast.makeText(context, "🚨 EMULATING SOS PANIC BEACON: Despatch coordinates & live mic feed to guardian", Toast.LENGTH_LONG).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CoralRed),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Test Distress Trigger", fontSize = 12.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                        }
                    }
                    4 -> { // Sandboxing
                        item {
                            HardwareControlRow(
                                title = "App Pinning (Screen Lockout)",
                                subtitle = "Forces current app foreground overlay, biometric authorization needed to escape",
                                defaultState = false
                            )
                        }
                        item {
                            HardwareControlRow(
                                title = "Guest Mode Profile Blocker",
                                subtitle = "Halts loading alternative secondary Android framework users entirely",
                                defaultState = true
                            )
                        }
                        item {
                            HardwareControlRow(
                                title = "Dual Apps/App Cloning Filters",
                                subtitle = "Intercepts sandboxed copy apps bypassing default URL filters",
                                defaultState = true
                            )
                        }
                        item {
                            HardwareControlRow(
                                title = "Biometric Vault Folders",
                                subtitle = "Safes media, notes, messages behind secure parent fingerprints",
                                defaultState = true
                            )
                        }
                        item {
                            HardwareControlRow(
                                title = "Home Screen Layout Freeze",
                                subtitle = "Block modifications, reorders, or deletion of parent launcher icons",
                                defaultState = true
                            )
                        }
                    }
                    5 -> { // Privacy Shld
                        item {
                            HardwareControlRow(
                                title = "Notification Preview Hiding",
                                subtitle = "Suppress details of pop-ups across top screen when borrower holds the device",
                                defaultState = true
                            )
                        }
                        item {
                            HardwareControlRow(
                                title = "Incoming Call Masking",
                                subtitle = "Removes caller ID names dynamically while lent flags are positive",
                                defaultState = false
                            )
                        }
                        item {
                            HardwareControlRow(
                                title = "Clipboard Auto-Wiper",
                                subtitle = "Flushes system paste register every 20 seconds to scrub credentials",
                                defaultState = true
                            )
                        }
                        item {
                            HardwareControlRow(
                                title = "Private Share & Bluetooth Blocks",
                                subtitle = "Mutes AirDrop, Nearby Share, and Bluetooth system payload handlers",
                                defaultState = true
                            )
                        }
                        item {
                            HardwareControlRow(
                                title = "Recent Apps Window Blur",
                                subtitle = "Blurs window view graphics on system app switcher overview",
                                defaultState = true
                            )
                        }
                    }
                    6 -> { // Hardening
                        item {
                            HardwareControlRow(
                                title = "Account Setting Lockouts",
                                subtitle = "Disables Core settings menu password edits and master user transfers",
                                defaultState = true
                            )
                        }
                        item {
                            HardwareControlRow(
                                title = "Sideloading APK Blocks",
                                subtitle = "Prohibits installation of apps via web-downloaded Android package files",
                                defaultState = true
                            )
                        }
                        item {
                            HardwareControlRow(
                                title = "Payment Authorization Enforcement",
                                subtitle = "Forces strict biological pattern approval match for microtransaction requests",
                                defaultState = true
                            )
                        }
                        item {
                            HardwareControlRow(
                                title = "USB Restricted Mode",
                                subtitle = "Dismantles OTG logic and data conduits, allows current flow inputs only",
                                defaultState = true
                            )
                        }
                        item {
                            HardwareControlRow(
                                title = "Developer Options Lockdown",
                                subtitle = "Hides ADB bridging options, preventing desktop payload code loads",
                                defaultState = true
                            )
                        }
                    }
                    7 -> { // Telemetry
                        item {
                            HardwareControlRow(
                                title = "Session-Based Active Screen logs",
                                subtitle = "Measures explicit on-screen duration millisecond levels for audits",
                                defaultState = true
                            )
                        }
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = NavyDark),
                                border = BorderStroke(1.dp, NavySurface2),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Geofenced Automated Remote Wipe", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text("Auto-triggers system scrub sequence if exiting parameter borders", fontSize = 11.sp, color = TextSecondary)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                Toast.makeText(context, "Geofenced scrub armed at School perimeter limit", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Arm Wipe", fontSize = 11.sp)
                                        }
                                        Button(
                                            onClick = {
                                                Toast.makeText(context, "Armed Geofence safety wiped", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = CoralRed),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Disarm Safe", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            HardwareControlRow(
                                title = "Web-Based Screen Locks",
                                subtitle = "Allows real-time locking commands from master web management interfaces",
                                defaultState = true
                            )
                        }
                        item {
                            HardwareControlRow(
                                title = "SIM Pin Configuration Lock",
                                subtitle = "Locks mobile SIM cards behind master guardian password logic",
                                defaultState = true
                            )
                        }
                        item {
                            HardwareControlRow(
                                title = "Smart Proximity Sensor Loop",
                                subtitle = "Vibrates guardian peer device immediately if companion separation crosses 30ft",
                                defaultState = true
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HardwareControlRow(
    title: String,
    subtitle: String,
    defaultState: Boolean
) {
    var state by remember { mutableStateOf(defaultState) }
    Card(
        colors = CardDefaults.cardColors(containerColor = NavyDark),
        border = BorderStroke(1.dp, NavySurface2),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, fontSize = 10.sp, color = TextSecondary, lineHeight = 13.sp)
            }
            Switch(
                checked = state,
                onCheckedChange = { state = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = EmeraldGreen,
                    checkedTrackColor = EmeraldGreen.copy(alpha = 0.3f),
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = NavyDark
                )
            )
        }
    }
}


@Composable
fun CanvasRadar(schoolRadius: Float, homeRadius: Float) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing))
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val center = androidx.compose.ui.geometry.Offset(w / 2f, h / 2f)

        // Draw tactical circles
        drawCircle(color = ElectricBlue.copy(alpha = 0.08f), radius = w / 4f, center = center, style = strokePx())
        drawCircle(color = ElectricBlue.copy(alpha = 0.04f), radius = w / 2.2f, center = center, style = strokePx())

        // Crosshairs
        drawLine(color = ElectricBlue.copy(alpha = 0.05f), start = androidx.compose.ui.geometry.Offset(0f, h/2f), end = androidx.compose.ui.geometry.Offset(w, h/2f))
        drawLine(color = ElectricBlue.copy(alpha = 0.05f), start = androidx.compose.ui.geometry.Offset(w/2f, 0f), end = androidx.compose.ui.geometry.Offset(w/2f, h))

        // Abstract geofences
        val schoolCoord = androidx.compose.ui.geometry.Offset(w / 2.5f, h / 2.5f)
        drawCircle(color = EmeraldGreen.copy(alpha = 0.12f), radius = schoolRadius / 1.5f, center = schoolCoord)
        drawCircle(color = EmeraldGreen, radius = schoolRadius / 1.5f, center = schoolCoord, style = strokePx())

        val homeCoord = androidx.compose.ui.geometry.Offset(w / 1.5f, h / 1.5f)
        drawCircle(color = VividPurple.copy(alpha = 0.12f), radius = homeRadius / 1.5f, center = homeCoord)
        drawCircle(color = VividPurple, radius = homeRadius / 1.5f, center = homeCoord, style = strokePx())

        // Child marker inside school Geofence
        val childCoord = androidx.compose.ui.geometry.Offset(w / 2.55f, h / 2.52f)
        drawCircle(color = ElectricBlue.copy(alpha = 0.45f * (1f - pulse)), radius = 40.dp.toPx() * pulse, center = childCoord)
        drawCircle(color = ElectricBlue, radius = 6.dp.toPx(), center = childCoord)
        drawCircle(color = Color.White, radius = 2.dp.toPx(), center = childCoord)
    }
}

fun strokePx() = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)

@Composable
fun WeeklyBarsProgress() {
    val hrs = listOf(1.5f, 2.4f, 3.1f, 1.8f, 2.0f, 4.3f, 3.5f)
    val weekdays = listOf("M", "T", "W", "T", "F", "S", "S")

    Row(
        modifier = Modifier.fillMaxWidth().height(100.dp).background(NavyDark, RoundedCornerShape(8.dp)).padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        hrs.forEachIndexed { i, hr ->
            val fraction = hr / 5f
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f)
            ) {
                Text(String.format("%.1fh", hr), color = ElectricBlue, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .width(14.dp)
                        .height((60.dp.value * fraction).dp)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(Brush.verticalGradient(listOf(ElectricBlue, VividPurple)))
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(weekdays[i], color = TextSecondary, fontSize = 8.sp)
            }
        }
    }
}
