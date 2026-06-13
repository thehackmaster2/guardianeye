package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.managers.SupabaseManager
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onNavigateToRole: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Slide up animation state for the card contents
    var cardVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        cardVisible = true
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
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // Animated Custom Logo
            AnimatedLogo(
                modifier = Modifier
                    .size(130.dp)
                    .padding(bottom = 16.dp)
            )

            Text(
                text = "GuardianEye",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 1.5.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Protect what matters most",
                fontSize = 15.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
            )

            // Dynamic entry animated container
            AnimatedVisibility(
                visible = cardVisible,
                enter = slideInVertically(
                    initialOffsetY = { 300 },
                    animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow)
                ) + fadeIn(animationSpec = tween(500)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = NavySurface
                    ),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.dp, NavySurface2),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Authentication Portal",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(bottom = 20.dp)
                        )

                        // Email Field
                        GuardianTextField(
                            value = email,
                            onValueChange = { 
                                email = it 
                                errorMessage = null
                            },
                            label = "Parent Email Address",
                            leadingIcon = Icons.Default.Email,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            testTag = "email_input",
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Password Field
                        GuardianTextField(
                            value = password,
                            onValueChange = { 
                                password = it 
                                errorMessage = null
                            },
                            label = "Secure Password",
                            leadingIcon = Icons.Default.Lock,
                            isPassword = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle password visibility",
                                        tint = TextSecondary
                                    )
                                }
                            },
                            testTag = "password_input",
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Error message animation
                        AnimatedVisibility(
                            visible = errorMessage != null,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            errorMessage?.let { msg ->
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = CoralRed.copy(alpha = 0.1f)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, CoralRed.copy(alpha = 0.3f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 20.dp)
                                ) {
                                    Text(
                                        text = msg,
                                        color = CoralRed,
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 10.dp)
                                    )
                                }
                            }
                        }

                        // Gradient Sign In Button
                        GradientButton(
                            text = "SIGN IN",
                            onClick = {
                                if (email.isBlank() || password.isBlank()) {
                                    errorMessage = "Please enter both email and password"
                                    return@GradientButton
                                }
                                isLoading = true
                                errorMessage = null
                                coroutineScope.launch {
                                    val success = SupabaseManager.signInWithEmail(email.trim(), password)
                                    isLoading = false
                                    if (success) {
                                        Toast.makeText(context, "Sign-in successful!", Toast.LENGTH_SHORT).show()
                                        onNavigateToRole()
                                    } else {
                                        errorMessage = "Invalid login credentials. Please check or register."
                                    }
                                }
                            },
                            isLoading = isLoading,
                            testTag = "login_button",
                            modifier = Modifier.padding(bottom = 14.dp)
                        )

                        // Elegant Translucent Register Button
                        OutlinedButton(
                            onClick = {
                                if (email.isBlank() || password.isBlank()) {
                                    errorMessage = "Please fill in all credentials to register"
                                    return@OutlinedButton
                                }
                                isLoading = true
                                errorMessage = null
                                coroutineScope.launch {
                                    val success = SupabaseManager.signUpWithEmail(email.trim(), password)
                                    isLoading = false
                                    if (success) {
                                        Toast.makeText(context, "Registration welcomed!", Toast.LENGTH_LONG).show()
                                        onNavigateToRole()
                                    } else {
                                        errorMessage = "Registration failed. Try a different email."
                                    }
                                }
                            },
                            border = BorderStroke(1.dp, NavySurface2),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = NavySurface2.copy(alpha = 0.3f),
                                contentColor = ElectricBlue
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("register_button")
                        ) {
                            Text(
                                "CREATE ACCOUNT",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

