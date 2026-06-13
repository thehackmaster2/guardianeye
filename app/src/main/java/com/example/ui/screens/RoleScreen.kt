package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.managers.SupabaseManager
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun RoleScreen(
    onNavigateToPairing: (role: String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf("parent") }

    var containerVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        containerVisible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyDark)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            // Header space
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(
                    text = "Device Authority",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Select the operational role for this device",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            // Central Selection Section
            AnimatedVisibility(
                visible = containerVisible,
                enter = fadeIn(animationSpec = tween(500)) + expandVertically(animationSpec = tween(600)),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = ElectricBlue,
                            modifier = Modifier.size(52.dp)
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Parent Role Selection Card
                            RoleCard(
                                title = "Guardian",
                                subtitle = "Monitor & remote control child device",
                                features = listOf(
                                    "Live Screen mirroring",
                                    "Ambient audio check",
                                    "Remote action clicks",
                                    "E2E secure tunnel"
                                ),
                                isSelected = selectedRole == "parent",
                                onClick = { selectedRole = "parent" },
                                accentColor = ElectricBlue,
                                gradientColors = listOf(ElectricBlue, VividPurple),
                                iconVector = Icons.Default.SupervisorAccount,
                                modifier = Modifier.weight(1f),
                                testTag = "role_parent_button"
                            )

                            // Child Role Selection Card
                            RoleCard(
                                title = "Secured Child",
                                subtitle = "Encrypts and broadcasts device streams",
                                features = listOf(
                                    "Background stream",
                                    "Overlay coordinate check",
                                    "Simulated tap inputs",
                                    "Active connection HUD"
                                ),
                                isSelected = selectedRole == "child",
                                onClick = { selectedRole = "child" },
                                accentColor = VividPurple,
                                gradientColors = listOf(VividPurple, ElectricBlue),
                                iconVector = Icons.Default.Smartphone,
                                modifier = Modifier.weight(1f),
                                testTag = "role_child_button"
                            )
                        }
                    }
                }
            }

            // Footer Button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GradientButton(
                    text = "CONTINUE AS ${selectedRole.uppercase()}",
                    onClick = {
                        selectRole(context, coroutineScope, selectedRole, onNavigateToPairing) { isLoading = it }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "continue_role_button"
                )
            }
        }
    }
}

private fun selectRole(
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope,
    role: String,
    onNavigate: (role: String) -> Unit,
    setLoadingState: (Boolean) -> Unit
) {
    setLoadingState(true)
    scope.launch {
        val uid = SupabaseManager.getCurrentUserId()
        val email = SupabaseManager.getCurrentUserEmail() ?: "user@guardianeye.com"
        
        if (uid != null) {
            val success = SupabaseManager.saveUserRole(uid, email, role)
            setLoadingState(false)
            if (success) {
                Toast.makeText(context, "Role registered: ${role.uppercase()}", Toast.LENGTH_SHORT).show()
                onNavigate(role)
            } else {
                Toast.makeText(context, "Network bypass: entering portal offline", Toast.LENGTH_SHORT).show()
                onNavigate(role)
            }
        } else {
            setLoadingState(false)
            Toast.makeText(context, "Bypassing authenticated session offline", Toast.LENGTH_SHORT).show()
            onNavigate(role)
        }
    }
}

