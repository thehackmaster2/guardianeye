package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.managers.SocketManager
import com.example.managers.WebRTCManager
import com.example.services.ChildForegroundService
import com.example.services.RemoteControlAccessibilityService
import com.example.ui.components.*
import com.example.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.webrtc.*

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StreamScreen(
    roomCode: String,
    role: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    // Screen Dimensions for scaling click coordinates
    val displayMetrics: DisplayMetrics = context.resources.displayMetrics
    val screenWidth = displayMetrics.widthPixels.toFloat()
    val screenHeight = displayMetrics.heightPixels.toFloat()

    if (role == "parent") {
        ParentStreamView(context, roomCode, screenWidth, screenHeight, onNavigateBack)
    } else {
        ChildSetupAndStreamView(context, roomCode, onNavigateBack)
    }
}

// PARENT LAYOUT

@Composable
fun ParentStreamView(
    context: Context,
    roomCode: String,
    screenWidth: Float,
    screenHeight: Float,
    onNavigateBack: () -> Unit
) {
    var webRtcManager by remember { mutableStateOf<WebRTCManager?>(null) }
    var screenRenderer by remember { mutableStateOf<SurfaceViewRenderer?>(null) }
    var cameraRenderer by remember { mutableStateOf<SurfaceViewRenderer?>(null) }

    var screenTrack by remember { mutableStateOf<VideoTrack?>(null) }
    var cameraTrack by remember { mutableStateOf<VideoTrack?>(null) }
    var isMuted by remember { mutableStateOf(false) }
    var isConnected by remember { mutableStateOf(false) }

    var renderWidth by remember { mutableStateOf(1) }
    var renderHeight by remember { mutableStateOf(1) }

    // Swipe capture offsets
    var dragStartX by remember { mutableStateOf(0f) }
    var dragStartY by remember { mutableStateOf(0f) }
    var dragEndX by remember { mutableStateOf(0f) }
    var dragEndY by remember { mutableStateOf(0f) }
    var dragStartTime by remember { mutableStateOf(0L) }

    // Live session timer state
    var sessionSeconds by remember { mutableStateOf(0) }
    
    // Increment timer when connected
    LaunchedEffect(isConnected) {
        if (isConnected) {
            while (true) {
                delay(1000)
                sessionSeconds++
            }
        } else {
            sessionSeconds = 0
        }
    }

    // Format timer to MM:SS or HH:MM:SS
    val timerText = remember(sessionSeconds) {
        val mins = (sessionSeconds / 60) % 60
        val secs = sessionSeconds % 60
        val hrs = sessionSeconds / 3600
        if (hrs > 0) {
            String.format("%02d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format("%02d:%02d", mins, secs)
        }
    }

    // Initialize WebRTC parent session
    LaunchedEffect(roomCode) {
        SocketManager.connect()
        SocketManager.emitJoinRoom(roomCode, "parent")

        webRtcManager = WebRTCManager(
            context = context,
            peerId = "child", // Calling Child
            isParent = true,
            onIceCandidateReady = { _, candidate ->
                SocketManager.emitIceCandidate("child", candidate)
            },
            onLocalSdpReady = { _, sdp ->
                SocketManager.emitOffer("child", sdp.description)
            },
            onRemoteTrackAdded = { stream ->
                Log.d("ParentStream", "onRemoteTrackAdded: videoTrackSize=${stream.videoTracks.size}")
                if (stream.videoTracks.isNotEmpty()) {
                    isConnected = true
                    screenTrack = stream.videoTracks[0]
                    if (stream.videoTracks.size > 1) {
                        cameraTrack = stream.videoTracks[1]
                    }
                }
            }
        )

        // Listen for remote events (ICE candidates, Answers)
        CoroutineScope(Dispatchers.Main).launch {
            SocketManager.answerFlow.collect { event ->
                Log.d("ParentStream", "Received WebRTC answer from child. Applying...")
                webRtcManager?.handleRemoteSdp("answer", event.sdp)
            }
        }

        CoroutineScope(Dispatchers.Main).launch {
            SocketManager.iceCandidateFlow.collect { event ->
                try {
                    val mid = event.candidate.getString("sdpMid")
                    val index = event.candidate.getInt("sdpMLineIndex")
                    val sdp = event.candidate.getString("candidate")
                    webRtcManager?.handleRemoteIceCandidate(mid, index, sdp)
                } catch (e: Exception) {
                    Log.e("ParentStream", "Failed to apply remote candidate: ${e.message}")
                }
            }
        }

        CoroutineScope(Dispatchers.Main).launch {
            SocketManager.peerDisconnectedFlow.collect { role ->
                if (role == "child") {
                    Toast.makeText(context, "Child device disconnected", Toast.LENGTH_LONG).show()
                    isConnected = false
                    screenTrack = null
                    cameraTrack = null
                }
            }
        }
    }

    // Attach screen video track to renderer
    LaunchedEffect(screenTrack, screenRenderer) {
        val st = screenTrack
        val sr = screenRenderer
        if (st != null && sr != null) {
            Log.d("ParentStream", "Attaching Remote Screen track to SurfaceView")
            st.addSink(sr)
        }
    }

    // Attach camera video track to renderer
    LaunchedEffect(cameraTrack, cameraRenderer) {
        val ct = cameraTrack
        val cr = cameraRenderer
        if (ct != null && cr != null) {
            Log.d("ParentStream", "Attaching Remote Camera track to SurfaceView")
            ct.addSink(cr)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyDark)
    ) {
        if (!isConnected) {
            // Premium custom unconnected loader UI
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(contentAlignment = Alignment.Center) {
                    // Outer pulsing ring
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse_outer")
                    val sizeScale by infiniteTransition.animateFloat(
                        initialValue = 0.9f,
                        targetValue = 1.25f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "ring_scale"
                    )
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .scale(sizeScale)
                            .clip(CircleShape)
                            .background(ElectricBlue.copy(alpha = 0.12f))
                    )

                    CircularProgressIndicator(
                        color = ElectricBlue,
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(72.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    "Establishing Live Portal...",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "Waiting for authorization from target child screen.\nActive Room ID: $roomCode",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        } else {
            // Live monitor Touch Interface container
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coordinates ->
                        renderWidth = coordinates.size.width
                        renderHeight = coordinates.size.height
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { offset ->
                                val xPercentage = offset.x / renderWidth
                                val yPercentage = offset.y / renderHeight
                                Log.d("ParentStream", "Local click detected: x=$xPercentage, y=$yPercentage")
                                SocketManager.emitRemoteControl("tap", xPercentage, yPercentage)
                            },
                            onLongPress = { offset ->
                                val xPercentage = offset.x / renderWidth
                                val yPercentage = offset.y / renderHeight
                                Log.d("ParentStream", "Local longpress detected: x=$xPercentage, y=$yPercentage")
                                SocketManager.emitRemoteControl("longpress", xPercentage, yPercentage)
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                dragStartX = offset.x / renderWidth
                                dragStartY = offset.y / renderHeight
                                dragStartTime = System.currentTimeMillis()
                            },
                            onDragEnd = {
                                val elapsed = System.currentTimeMillis() - dragStartTime
                                Log.d("ParentStream", "Local drag swipe detected: ($dragStartX, $dragStartY) -> ($dragEndX, $dragEndY), duration=${elapsed}ms")
                                SocketManager.emitRemoteControlSwipe(dragStartX, dragStartY, dragEndX, dragEndY, maxOf(100L, elapsed))
                            },
                            onDragCancel = {
                                // no-op
                            },
                            onDrag = { change, dragAmount ->
                                dragEndX = change.position.x / renderWidth
                                dragEndY = change.position.y / renderHeight
                            }
                        )
                    }
            ) {
                // Screen Mirror Render Surface
                AndroidView(
                    factory = { ctx ->
                        SurfaceViewRenderer(ctx).apply {
                            init(WebRTCManager.eglBase.eglBaseContext, null)
                            setEnableHardwareScaler(true)
                            setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
                            screenRenderer = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Picture-in-Picture Front Camera Card (Floating overlay)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 96.dp, end = 20.dp)
                        .size(width = 110.dp, height = 150.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(NavySurface.copy(alpha = 0.7f))
                        .border(1.5.dp, ElectricBlue.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                ) {
                    if (cameraTrack != null) {
                        AndroidView(
                            factory = { ctx ->
                                SurfaceViewRenderer(ctx).apply {
                                    init(WebRTCManager.eglBase.eglBaseContext, null)
                                    setEnableHardwareScaler(true)
                                    setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                                    cameraRenderer = this
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Icon fallback
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = TextSecondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Camera Inactive", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        // Parent Top HUD Title Information Info
        StatusBar(
            title = "Guardian Monitor",
            roomCode = roomCode,
            isConnected = isConnected,
            timerText = if (isConnected) "LIVE  $timerText" else "",
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Parent floating control strip at bottom
        ControlBar(
            isAudioMuted = isMuted,
            onAudioMuteToggle = {
                isMuted = !isMuted
                webRtcManager?.peerConnection?.let { pc ->
                    for (receiver in pc.receivers) {
                        val track = receiver.track()
                        if (track is AudioTrack) {
                            track.setEnabled(!isMuted)
                        }
                    }
                }
                Toast.makeText(context, if (isMuted) "Audio Muted" else "Audio Unmuted", Toast.LENGTH_SHORT).show()
            },
            onDisconnectClick = {
                webRtcManager?.close()
                SocketManager.disconnect()
                onNavigateBack()
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

// CHILD LAYOUT

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ChildSetupAndStreamView(
    context: Context,
    roomCode: String,
    onNavigateBack: () -> Unit
) {
    var isBroadcasting by remember { mutableStateOf(false) }

    // Media projection projection launcher setup
    val mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    val projectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val startIntent = Intent(context, ChildForegroundService::class.java).apply {
                action = ChildForegroundService.ACTION_START
                putExtra(ChildForegroundService.EXTRA_PROJECTION_INTENT, result.data)
                putExtra(ChildForegroundService.EXTRA_ROOM_CODE, roomCode)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(startIntent)
            } else {
                context.startService(startIntent)
            }
            isBroadcasting = true
            Toast.makeText(context, "Real-time background broadcast secure!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Screen share permission was cancelled", Toast.LENGTH_LONG).show()
        }
    }

    // Required System Overlay check
    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        val permitted = Settings.canDrawOverlays(context)
        Toast.makeText(context, if (permitted) "Overlay verified" else "Overlay denied", Toast.LENGTH_SHORT).show()
    }

    // Multiple Camera and Micro Runtime Permissions setup
    val parentPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyDark)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Elegant pulsing sensor icon header
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(NavySurface2)
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isBroadcasting) Icons.Default.Sensors else Icons.Default.Dashboard,
                    contentDescription = null,
                    tint = if (isBroadcasting) EmeraldGreen else ElectricBlue,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isBroadcasting) "LIVE BROADCAST SECURE" else "PAIRING COMPLETED",
                fontSize = 24.sp,
                color = TextPrimary,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                letterSpacing = 0.5.sp
            )

            Text(
                text = "Secure Portal Connection Room: $roomCode",
                fontSize = 13.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(26.dp),
                border = BorderStroke(1.dp, NavySurface2),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isBroadcasting) {
                        Text(
                            text = "This device screen size, camera viewpoints, and microphones inputs are fully encrypted and streaming content-safely to parent monitor.",
                            fontSize = 14.sp,
                            color = TextPrimary.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        // Stop Stream Action Gradient Button (Coral Red Glow)
                        GradientButton(
                            text = "DEACTIVATE BROADCAST",
                            onClick = {
                                val stopIntent = Intent(context, ChildForegroundService::class.java).apply {
                                    action = ChildForegroundService.ACTION_STOP
                                }
                                context.stopService(stopIntent)
                                isBroadcasting = false
                                Toast.makeText(context, "Streaming safely deactivated", Toast.LENGTH_SHORT).show()
                            },
                            gradient = Brush.linearGradient(listOf(CoralRed, Color(0xFFF43F5E))),
                            testTag = "stop_stream_button",
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = "Authorize required security policies below to configure automated screen streams and clicks successfully.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        // Runtime Custom Camera/Mic Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("A/V Portals", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                                Text("Required for camera/mic feeds", color = TextSecondary, fontSize = 11.sp)
                            }
                            if (parentPermissions.allPermissionsGranted) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(24.dp))
                            } else {
                                Button(
                                    onClick = { parentPermissions.launchMultiplePermissionRequest() },
                                    colors = ButtonDefaults.buttonColors(containerColor = NavySurface2),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Grant", fontSize = 12.sp, color = ElectricBlue, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        HorizontalDivider(color = NavySurface2, modifier = Modifier.padding(vertical = 12.dp))

                        // Overlay Custom Permission Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Screen Overlay", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                                Text("Coordinates display sync layer", color = TextSecondary, fontSize = 11.sp)
                            }
                            if (Settings.canDrawOverlays(context)) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(24.dp))
                            } else {
                                Button(
                                    onClick = {
                                        val intent = Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                        overlayPermissionLauncher.launch(intent)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NavySurface2),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Configure", fontSize = 12.sp, color = ElectricBlue, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        HorizontalDivider(color = NavySurface2, modifier = Modifier.padding(vertical = 12.dp))

                        // Accessibility Custom Service Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Remote Accessibility", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                                Text("Provides simulated tap events on screen", color = TextSecondary, fontSize = 11.sp)
                            }
                            if (RemoteControlAccessibilityService.isActive()) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(24.dp))
                            } else {
                                Button(
                                    onClick = {
                                        Toast.makeText(context, "Locate GuardianEye Remote Service in settings and enable.", Toast.LENGTH_LONG).show()
                                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                        context.startActivity(intent)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NavySurface2),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Activate", fontSize = 12.sp, color = ElectricBlue, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // START BROADCAST CTA BUTTON (Requires projection screen)
                        GradientButton(
                            text = "START SECURED BROADCAST",
                            onClick = {
                                if (!parentPermissions.allPermissionsGranted) {
                                    Toast.makeText(context, "Please click grant on A/V permissions.", Toast.LENGTH_SHORT).show()
                                    return@GradientButton
                                }
                                if (!Settings.canDrawOverlays(context)) {
                                    Toast.makeText(context, "Please enable Screen Overlay permission.", Toast.LENGTH_SHORT).show()
                                    return@GradientButton
                                }
                                if (!RemoteControlAccessibilityService.isActive()) {
                                    Toast.makeText(context, "Please enable GuardianEye Accessibility Service.", Toast.LENGTH_LONG).show()
                                    return@GradientButton
                                }

                                // Launch projection prompt
                                Log.d("ChildStream", "Launching System Screen-Capture MediaProjection prompt")
                                val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
                                projectionLauncher.launch(captureIntent)
                            },
                            testTag = "start_broadcast_button",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Stop service completely / Disconnect Button
            TextButton(
                onClick = {
                    val stopIntent = Intent(context, ChildForegroundService::class.java).apply {
                        action = ChildForegroundService.ACTION_STOP
                    }
                    context.stopService(stopIntent)
                    SocketManager.disconnect()
                    onNavigateBack()
                }
            ) {
                Text(
                    "DISCONNECT SECURE TUNNEL",
                    color = CoralRed.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.75.sp
                )
            }
        }
    }
}

