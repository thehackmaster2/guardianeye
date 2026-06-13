package com.example.ui.screens

import android.content.Context
import android.os.Environment
import android.util.Base64
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.managers.SocketManager
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    roomCode: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var currentTab by remember { mutableStateOf("photos") } // "photos", "videos", "documents", "all"
    var fileList by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedFileForPreview by remember { mutableStateOf<JSONObject?>(null) }
    var downloadedFileBytes by remember { mutableStateOf<ByteArray?>(null) }
    var isDownloadingState by remember { mutableStateOf(false) }

    // Connect and listen for file events
    LaunchedEffect(roomCode, currentTab) {
        isLoading = true
        fileList = emptyList()
        SocketManager.connect()
        SocketManager.emitRequestFiles(currentTab)
        
        launch {
            SocketManager.fileListFlow.collect { list ->
                val ktList = mutableListOf<JSONObject>()
                for (i in 0 until list.length()) {
                    ktList.add(list.getJSONObject(i))
                }
                fileList = ktList
                isLoading = false
            }
        }

        launch {
            SocketManager.fileDataFlow.collect { event ->
                if (selectedFileForPreview?.optString("path") == event.path) {
                    try {
                        val bytes = Base64.decode(event.base64Data, Base64.DEFAULT)
                        downloadedFileBytes = bytes
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error decoding file payload: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                    isDownloadingState = false
                }
            }
        }

        // Timeout fallback
        delay(6000)
        if (isLoading) {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = VividPurple,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Browse Child Files",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("file_browser_back")
                    ) {
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
                            isLoading = true
                            SocketManager.emitRequestFiles(currentTab)
                        },
                        modifier = Modifier.testTag("file_browser_refresh")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
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
        ) {
            // Tab row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(NavySurface)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val tabs = listOf(
                    "photos" to "Photos",
                    "videos" to "Videos",
                    "documents" to "Docs",
                    "all" to "All"
                )
                
                tabs.forEach { (route, label) ->
                    val isSelected = currentTab == route
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) VividPurple else Color.Transparent)
                            .clickable { currentTab = route }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) TextPrimary else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // File items grid
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = VividPurple)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Querying child device file index...",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else if (fileList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FindInPage,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Files Encountered",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Either there are no matching files under '$currentTab' on the child device or storage authorization is pending.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(fileList) { fileObj ->
                        val path = fileObj.optString("path", "")
                        val name = fileObj.optString("name", "Unknown File")
                        val size = fileObj.optLong("size", 0L)
                        val mime = fileObj.optString("mimeType", "")
                        
                        FileGridItem(
                            name = name,
                            path = path,
                            size = size,
                            mimeType = mime,
                            onClick = {
                                selectedFileForPreview = fileObj
                                downloadedFileBytes = null
                                isDownloadingState = true
                                SocketManager.emitRequestFileData(path)
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal view block
    if (selectedFileForPreview != null) {
        val fileObj = selectedFileForPreview!!
        val name = fileObj.optString("name", "")
        val path = fileObj.optString("path", "")
        val size = fileObj.optLong("size", 0)
        
        AlertDialog(
            onDismissRequest = {
                selectedFileForPreview = null
                downloadedFileBytes = null
            },
            title = {
                Text(
                    text = "File Details preview",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(
                            imageVector = when {
                                name.endsWith(".png", true) || name.endsWith(".jpg", true) || name.endsWith(".jpeg", true) -> Icons.Default.Image
                                name.endsWith(".mp4", true) || name.endsWith(".3gp", true) -> Icons.Default.VideoFile
                                name.endsWith(".pdf", true) || name.endsWith(".txt", true) || name.endsWith(".doc", true) -> Icons.Default.Description
                                else -> Icons.Default.InsertDriveFile
                            },
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(44.dp)
                        )
                        Column {
                            Text(
                                text = name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Size: ${size / 1024} KB",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Text(
                        text = "Path: $path",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    Divider(color = NavySurface2)

                    if (isDownloadingState) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(color = ElectricBlue, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Fetching complete file bytes...", fontSize = 12.sp, color = TextSecondary)
                        }
                    } else if (downloadedFileBytes != null) {
                        Text(
                            text = "✓ File downloaded from child successfully",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreen
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val bytes = downloadedFileBytes
                        if (bytes != null) {
                            val saved = saveFileToExternalStorage(context, name, bytes)
                            if (saved) {
                                Toast.makeText(context, "Saved to Downloads", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Saved internally", Toast.LENGTH_SHORT).show()
                            }
                            selectedFileForPreview = null
                            downloadedFileBytes = null
                        } else {
                            SocketManager.emitRequestFileData(path)
                            isDownloadingState = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VividPurple),
                    enabled = !isDownloadingState
                ) {
                    Text(if (downloadedFileBytes != null) "Save File" else "Retry Pull", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    selectedFileForPreview = null
                    downloadedFileBytes = null
                }) {
                    Text("Close", color = TextSecondary)
                }
            },
            containerColor = NavyDark
        )
    }
}

@Composable
fun FileGridItem(
    name: String,
    path: String,
    size: Long,
    mimeType: String,
    onClick: () -> Unit
) {
    val fileIcon = when {
        name.endsWith(".png", true) || name.endsWith(".jpg", true) || name.endsWith(".jpeg", true) -> Icons.Default.Image
        name.endsWith(".mp4", true) || name.endsWith(".3gp", true) -> Icons.Default.VideoFile
        name.endsWith(".pdf", true) || name.endsWith(".txt", true) || name.endsWith(".doc", true) -> Icons.Default.Description
        else -> Icons.Default.InsertDriveFile
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        border = BorderStroke(1.dp, NavySurface2),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(NavyDark),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = fileIcon,
                    contentDescription = null,
                    tint = ElectricBlue,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "${size / 1024} KB",
                fontSize = 10.sp,
                color = TextSecondary
            )
        }
    }
}

private fun saveFileToExternalStorage(context: Context, filename: String, bytes: ByteArray): Boolean {
    return try {
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadDir.exists()) downloadDir.mkdirs()
        val destFile = File(downloadDir, "GuardianEye_$filename")
        FileOutputStream(destFile).use { fos ->
            fos.write(bytes)
        }
        true
    } catch (e: Exception) {
        try {
            // fallback internal
            val file = File(context.cacheDir, filename)
            FileOutputStream(file).use { fos ->
                fos.write(bytes)
            }
            true
        } catch (ex: Exception) {
            false
        }
    }
}
