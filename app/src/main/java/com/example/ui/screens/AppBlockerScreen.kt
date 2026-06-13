package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.managers.SocketManager
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBlockerScreen(
    roomCode: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var isFetching by remember { mutableStateOf(false) }
    var installedApps by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var blockedPackages by remember { mutableStateOf<Set<String>>(emptySet()) }
    var searchQuery by remember { mutableStateOf("") }

    // Connect and setup subscribers
    LaunchedEffect(roomCode) {
        isFetching = true
        SocketManager.connect()
        SocketManager.emitRequestApps()
        
        launch {
            SocketManager.appListFlow.collect { appList ->
                val list = mutableListOf<JSONObject>()
                for (i in 0 until appList.length()) {
                    list.add(appList.getJSONObject(i))
                }
                installedApps = list.sortedBy { it.optString("name", "").lowercase() }
                isFetching = false
            }
        }
        
        // Timeout
        delay(6000)
        if (isFetching) {
            isFetching = false
        }
    }

    val filteredApps = remember(installedApps, searchQuery) {
        if (searchQuery.trim().isEmpty()) {
            installedApps
        } else {
            installedApps.filter {
                it.optString("name", "").contains(searchQuery, ignoreCase = true) ||
                it.optString("package", "").contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AppBlocking,
                            contentDescription = null,
                            tint = EmeraldGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Blistered App Blocker",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            isFetching = true
                            SocketManager.emitRequestApps()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync",
                            tint = ElectricBlue
                        )
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
            Text(
                text = "Control what apps your child is allowed to open. Blocked apps will automatically redirect back to the home screen.",
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Search filter
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search package or label", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = TextSecondary) },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = NavySurface,
                    unfocusedContainerColor = NavySurface,
                    focusedIndicatorColor = ElectricBlue,
                    unfocusedIndicatorColor = NavySurface2
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            // Bulk actions Buttons row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Block Selected
                Button(
                    onClick = {
                        val array = JSONArray()
                        blockedPackages.forEach { array.put(it) }
                        SocketManager.emitBlockApps(array)
                        Toast.makeText(context, "Blocking list deployed to child", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1.3f)
                ) {
                    Text("Apply Rules", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }

                // Block All
                Button(
                    onClick = {
                        val allPkgs = installedApps.map { it.optString("package") }.filter { it.isNotEmpty() }
                        blockedPackages = allPkgs.toSet()
                        
                        val array = JSONArray()
                        allPkgs.forEach { array.put(it) }
                        SocketManager.emitBlockApps(array)
                        Toast.makeText(context, "All apps blocked", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NavySurface2),
                    border = BorderStroke(1.dp, CoralRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Block All", fontSize = 11.sp, color = TextPrimary)
                }

                // Unblock All
                Button(
                    onClick = {
                        blockedPackages = emptySet()
                        SocketManager.emitBlockApps(JSONArray())
                        Toast.makeText(context, "All apps unblocked", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NavySurface2),
                    border = BorderStroke(1.dp, EmeraldGreen),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Unblock All", fontSize = 11.sp, color = TextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main package list scroll
            if (isFetching) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = EmeraldGreen)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Retrieving installed applications list...",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            } else if (filteredApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No matching applications found.", color = TextSecondary, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredApps) { appObj ->
                        val name = appObj.optString("name", "Unknown App")
                        val pkg = appObj.optString("package", "")
                        val isBlocked = blockedPackages.contains(pkg)

                        AppBlockItem(
                            appName = name,
                            packageName = pkg,
                            isBlocked = isBlocked,
                            onToggleBlocked = {
                                blockedPackages = if (isBlocked) {
                                    blockedPackages - pkg
                                } else {
                                    blockedPackages + pkg
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppBlockItem(
    appName: String,
    packageName: String,
    isBlocked: Boolean,
    onToggleBlocked: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        border = BorderStroke(1.dp, if (isBlocked) CoralRed else NavySurface2),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleBlocked() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isBlocked) CoralRed.copy(alpha = 0.12f) else EmeraldGreen.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isBlocked) Icons.Default.Block else Icons.Default.Android,
                        contentDescription = null,
                        tint = if (isBlocked) CoralRed else EmeraldGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = appName,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = packageName,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }
            
            Switch(
                checked = isBlocked,
                onCheckedChange = { onToggleBlocked() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = TextPrimary,
                    checkedTrackColor = CoralRed,
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = NavySurface2
                )
            )
        }
    }
}
