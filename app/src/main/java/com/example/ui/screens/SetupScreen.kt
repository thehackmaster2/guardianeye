package com.example.ui.screens

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.admin.GuardianDeviceAdminReceiver
import com.example.services.RemoteControlAccessibilityService
import com.example.ui.components.*
import com.example.ui.theme.*
import com.google.accompanist.permissions.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SetupScreen(
    onSetupComplete: () -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableStateOf(1) }
    val totalSteps = 6

    // Permissions state for Step 2
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.READ_PHONE_STATE
        )
    )

    // State checking helpers
    var isAccessibilityActive by remember { mutableStateOf(false) }
    var isDeviceAdminActive by remember { mutableStateOf(false) }
    var isBatteryExclusionActive by remember { mutableStateOf(false) }

    // Periodically update permissions & systems configuration state
    LaunchedEffect(Unit) {
        while (true) {
            isAccessibilityActive = isAccessibilityEnabled(context)
            isDeviceAdminActive = isDeviceAdminEnabled(context)
            isBatteryExclusionActive = isBatteryIgnoringEnabled(context)
            delay(1000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyDark)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: logo and progress tracker
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Shield Logo",
                        tint = ElectricBlue,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GuardianEye",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Modern visual steps progress indicator
                Row(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..totalSteps) {
                        val isCompleted = i < currentStep
                        val isActive = i == currentStep
                        val barColor = when {
                            isCompleted -> EmeraldGreen
                            isActive -> ElectricBlue
                            else -> NavySurface2
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(barColor)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Step $currentStep of $totalSteps",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )
            }

            // Core wizard visual slides container
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { width -> width / 2 } + fadeIn(animationSpec = tween(300))).togetherWith(
                                slideOutHorizontally { width -> -width / 2 } + fadeOut(animationSpec = tween(300))
                            )
                        } else {
                            (slideInHorizontally { width -> -width / 2 } + fadeIn(animationSpec = tween(300))).togetherWith(
                                slideOutHorizontally { width -> width / 2 } + fadeOut(animationSpec = tween(300))
                            )
                        }
                    },
                    label = "setup_step_transition"
                ) { targetStep ->
                    when (targetStep) {
                        1 -> WelcomeStep()
                        2 -> PermissionsStep(permissionsState)
                        3 -> AccessibilityStep(isAccessibilityActive, context)
                        4 -> DeviceAdminStep(isDeviceAdminActive, context)
                        5 -> BatteryStep(isBatteryExclusionActive, context)
                        6 -> FinishedStep()
                    }
                }
            }

            // Footer / Standard Stepper Navigation Actions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (currentStep == totalSteps) {
                    // Step 6: Complete Registration callback
                    GradientButton(
                        text = "FINISH SETUP",
                        onClick = {
                            val prefs = context.getSharedPreferences("guardian_pref", Context.MODE_PRIVATE)
                            prefs.edit().putBoolean("setup_complete", true).apply()
                            onSetupComplete()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("finish_setup_button")
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentStep > 1) {
                            OutlinedButton(
                                onClick = { currentStep-- },
                                border = BorderStroke(1.dp, NavySurface2),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = TextPrimary,
                                    containerColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp)
                                    .testTag("prev_step_button")
                            ) {
                                Text("Back", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Determine if we can proceed to next step
                        val isAllowedToGoNext = when (currentStep) {
                            1 -> true
                            2 -> permissionsState.allPermissionsGranted
                            3 -> isAccessibilityActive
                            4 -> isDeviceAdminActive
                            5 -> isBatteryExclusionActive
                            else -> true
                        }

                        GradientButton(
                            text = "CONTINUE",
                            onClick = {
                                if (isAllowedToGoNext) {
                                    currentStep++
                                } else {
                                    // Trigger custom request alerts to guide user interaction
                                    when (currentStep) {
                                        2 -> permissionsState.launchMultiplePermissionRequest()
                                        3 -> Toast.makeText(context, "Please click 'Open Accessibility Settings' to enable Remote control.", Toast.LENGTH_SHORT).show()
                                        4 -> Toast.makeText(context, "Please click 'Enable Protection' to associate Device administrator.", Toast.LENGTH_SHORT).show()
                                        5 -> Toast.makeText(context, "Please click 'Configure Battery' to ignore battery savings.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            gradient = if (isAllowedToGoNext) PrimaryGradient else Brush.linearGradient(listOf(NavySurface2, NavySurface2)),
                            modifier = Modifier
                                .weight(if (currentStep > 1) 1f else 1.5f)
                                .testTag("next_step_button")
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WelcomeStep() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        AnimatedLogo(size = 140f)

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Parent Setup Required",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "This device will be managed and monitored securely by a parent or guardian. Please proceed with setting up the necessary consent layers.",
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionsStep(permissionsState: MultiplePermissionsState) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(NavySurface)
                .padding(18.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                tint = ElectricBlue,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Core Permissions Required",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "GuardianEye requests access to camera, microphone, and telephone states to stream live captures seamlessly to parent.",
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = NavySurface),
            border = BorderStroke(1.dp, NavySurface2),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PermissionItem(
                    title = "Camera Capture Access",
                    description = "Required to broadcast real-time video feed",
                    icon = Icons.Default.PhotoCamera,
                    isGranted = permissionsState.permissions.any { it.permission == android.Manifest.permission.CAMERA && it.status.isGranted }
                )
                Divider(color = NavySurface2)
                PermissionItem(
                    title = "Microphone Captures",
                    description = "Provides ambient audio environment safety checks",
                    icon = Icons.Default.Mic,
                    isGranted = permissionsState.permissions.any { it.permission == android.Manifest.permission.RECORD_AUDIO && it.status.isGranted }
                )
                Divider(color = NavySurface2)
                PermissionItem(
                    title = "Phone & Network Status",
                    description = "Identifies device telemetry parameters safely",
                    icon = Icons.Default.PhonelinkRing,
                    isGranted = permissionsState.permissions.any { it.permission == android.Manifest.permission.READ_PHONE_STATE && it.status.isGranted }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (!permissionsState.allPermissionsGranted) {
            GradientButton(
                text = "GRANT PERMISSIONS",
                onClick = { permissionsState.launchMultiplePermissionRequest() },
                modifier = Modifier.fillMaxWidth().testTag("grant_perms_button")
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = EmeraldGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "All Core Permissions Granted",
                    fontSize = 14.sp,
                    color = EmeraldGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun AccessibilityStep(isActive: Boolean, context: Context) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(NavySurface)
                .padding(18.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Accessibility,
                contentDescription = null,
                tint = VividPurple,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Remote Accessibility Service",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "This service enables parents to transmit simulated tap events on the screen coordinates dynamically. It also detects unauthorized tampering of GuardianEye settings.",
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = NavySurface),
            border = BorderStroke(1.dp, NavySurface2),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Simulate Clicks & Taps",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Needed for E2E secure screen control",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    if (isActive) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Active",
                            tint = EmeraldGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Button(
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NavySurface2),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Configure", fontSize = 12.sp, color = ElectricBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (!isActive) {
            GradientButton(
                text = "OPEN ACCESSIBILITY SETTINGS",
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                },
                gradient = Brush.linearGradient(listOf(VividPurple, ElectricBlue)),
                modifier = Modifier.fillMaxWidth().testTag("accessibility_open_button")
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Active",
                    tint = EmeraldGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Accessibility Protection Active",
                    fontSize = 14.sp,
                    color = EmeraldGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun DeviceAdminStep(isActive: Boolean, context: Context) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(NavySurface)
                .padding(18.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AdminPanelSettings,
                contentDescription = null,
                tint = EmeraldGreen,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Device Administrator Protection",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "This device administrator configuration prevents children or unauthorized actors from uninstalling the GuardianEye monitoring application without parent consent.",
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = NavySurface),
            border = BorderStroke(1.dp, NavySurface2),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Uninstall Protection",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Blocks unauthorized package removals",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    if (isActive) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Active",
                            tint = EmeraldGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Button(
                            onClick = {
                                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, ComponentName(context, GuardianDeviceAdminReceiver::class.java))
                                    putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Required to prevent unauthorized uninstall of GuardianEye.")
                                }
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NavySurface2),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Activate", fontSize = 12.sp, color = EmeraldGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (!isActive) {
            GradientButton(
                text = "ENABLE UNINSTALL PROTECTION",
                onClick = {
                    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, ComponentName(context, GuardianDeviceAdminReceiver::class.java))
                        putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Required to prevent unauthorized uninstall of GuardianEye.")
                    }
                    context.startActivity(intent)
                },
                gradient = Brush.linearGradient(listOf(EmeraldGreen, ElectricBlue)),
                modifier = Modifier.fillMaxWidth().testTag("admin_enable_button")
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Active",
                    tint = EmeraldGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Uninstall Protection Enabled",
                    fontSize = 14.sp,
                    color = EmeraldGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun BatteryStep(isActive: Boolean, context: Context) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(NavySurface)
                .padding(18.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.BatteryChargingFull,
                contentDescription = null,
                tint = ElectricBlue,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Background Execution Excursion",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Android systems aggressively kill background processing apps. GuardianEye requires battery optimization exemption to remain active 24/7.",
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = NavySurface),
            border = BorderStroke(1.dp, NavySurface2),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ignore Battery Savings",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Ensures background broadcasting stability",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    if (isActive) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Active",
                            tint = EmeraldGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Button(
                            onClick = {
                                launchBatterySettings(context)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NavySurface2),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Exempt", fontSize = 12.sp, color = ElectricBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (!isActive) {
            GradientButton(
                text = "REQUEST BATTERY PRESERVATION EXEMPT",
                onClick = {
                    launchBatterySettings(context)
                },
                modifier = Modifier.fillMaxWidth().testTag("battery_exempt_button")
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Active",
                    tint = EmeraldGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Battery Optimization Exempt",
                    fontSize = 14.sp,
                    color = EmeraldGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun launchBatterySettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            context.startActivity(intent)
        } catch (ex: Exception) {
            Toast.makeText(context, "Please disable battery optimization for GuardianEye manually", Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
fun FinishedStep() {
    val infiniteTransition = rememberInfiniteTransition(label = "shield_pulse")
    val animatedScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shield_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .scale(animatedScale)
                .clip(CircleShape)
                .background(EmeraldGreen.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(EmeraldGreen.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = EmeraldGreen,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "GuardianEye config matches!",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Device protection layers are successfully configured. GuardianEye is now fully armed in the background to guarantee active connection portals.",
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

@Composable
fun PermissionItem(
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isGranted) EmeraldGreen else ElectricBlue,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimary
                )
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }
        Icon(
            imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isGranted) EmeraldGreen else TextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

private fun isAccessibilityEnabled(context: Context): Boolean {
    if (RemoteControlAccessibilityService.isActive()) return true
    val service = "${context.packageName}/com.example.services.RemoteControlAccessibilityService"
    val serviceAlt = "${context.packageName}/.services.RemoteControlAccessibilityService"
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabled.contains(service) || enabled.contains(serviceAlt) || enabled.contains("RemoteControlAccessibilityService")
}

private fun isDeviceAdminEnabled(context: Context): Boolean {
    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    val adminComponent = ComponentName(context, GuardianDeviceAdminReceiver::class.java)
    return dpm.isAdminActive(adminComponent)
}

private fun isBatteryIgnoringEnabled(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}
