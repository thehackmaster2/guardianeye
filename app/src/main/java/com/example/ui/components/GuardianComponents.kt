package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import androidx.compose.foundation.LocalIndication

/**
 * Animated Logo using Compose Canvas to draw a premium custom shield with a glowing cyan eye in the center.
 * Features a spring entry scale animation and a breathing glow.
 */
@Composable
fun AnimatedLogo(
    modifier: Modifier = Modifier,
    size: Float = 120f
) {
    var scaleTarget by remember { mutableStateOf(0f) }
    
    LaunchedEffect(Unit) {
        scaleTarget = 1f
    }
    
    val animatedScale by animateFloatAsState(
        targetValue = scaleTarget,
        animationSpec = spring(
            dampingRatio = 0.58f,
            stiffness = Spring.StiffnessLow
        )
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_glow")
    val alphaGlow by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    Box(
        modifier = modifier
            .size(size.dp)
            .scale(animatedScale),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.dp.toPx()
            val height = size.dp.toPx()
            val padding = width * 0.1f

            // Left and Right control points for the shield curves
            val shieldPath = Path().apply {
                moveTo(width / 2f, padding)
                // Top curve right
                lineTo(width - padding, padding + height * 0.15f)
                // Side wall right curves gracefully to bottom point
                quadraticBezierTo(
                    width - padding, height * 0.65f,
                    width / 2f, height - padding
                )
                // Side wall left curves gracefully from bottom point
                quadraticBezierTo(
                    padding, height * 0.65f,
                    padding, padding + height * 0.15f
                )
                close()
            }

            // Draw shield outer outline with ambient glow
            drawPath(
                path = shieldPath,
                color = ElectricBlue.copy(alpha = alphaGlow * 0.3f),
                style = Stroke(width = width * 0.15f, cap = StrokeCap.Round)
            )

            drawPath(
                path = shieldPath,
                brush = Brush.linearGradient(
                    colors = listOf(ElectricBlue, VividPurple)
                ),
                style = Stroke(width = width * 0.05f, cap = StrokeCap.Round)
            )

            // Draw eye outline path
            val eyeWidth = width * 0.45f
            val eyeHeight = height * 0.25f
            val centerX = width / 2f
            val centerY = height * 0.45f

            val eyePath = Path().apply {
                moveTo(centerX - eyeWidth / 2f, centerY)
                // Top arc
                quadraticBezierTo(centerX, centerY - eyeHeight, centerX + eyeWidth / 2f, centerY)
                // Bottom arc
                quadraticBezierTo(centerX, centerY + eyeHeight, centerX - eyeWidth / 2f, centerY)
                close()
            }

            drawPath(
                path = eyePath,
                color = ElectricBlue.copy(alpha = 0.9f),
                style = Stroke(width = width * 0.03f, cap = StrokeCap.Round)
            )

            // Draw pupil/iris glowing circle inside the eye
            drawCircle(
                color = ElectricBlue.copy(alpha = alphaGlow),
                radius = width * 0.11f,
                center = androidx.compose.ui.geometry.Offset(centerX, centerY)
            )

            // Pupil inner dot
            drawCircle(
                color = Color.White,
                radius = width * 0.04f,
                center = androidx.compose.ui.geometry.Offset(centerX, centerY)
            )
        }
    }
}

/**
 * Premium Gradient Button with scale down press animation and spring release.
 */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    gradient: Brush = PrimaryGradient,
    testTag: String = ""
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "press_scale"
    )

    Button(
        onClick = { if (!isLoading) onClick() },
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(),
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .background(gradient)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.5.dp
                )
            } else {
                Text(
                    text = text,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.25.sp
                )
            }
        }
    }
}

/**
 * Dark Styled Text Field featuring customizable icons, passwords support, clear error display and border state animations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuardianTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    testTag: String = ""
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextSecondary) },
        leadingIcon = { Icon(leadingIcon, contentDescription = null, tint = ElectricBlue) },
        trailingIcon = trailingIcon,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ElectricBlue,
            unfocusedBorderColor = NavySurface2,
            focusedLabelColor = ElectricBlue,
            unfocusedLabelColor = TextSecondary,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedContainerColor = NavySurface,
            unfocusedContainerColor = NavySurface,
            cursorColor = ElectricBlue
        ),
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag)
    )
}

/**
 * Premium Role Card selection view with custom gradients, subtle state changes and slide entrance features.
 */
@Composable
fun RoleCard(
    title: String,
    subtitle: String,
    features: List<String>,
    isSelected: Boolean,
    onClick: () -> Unit,
    accentColor: Color,
    gradientColors: List<Color>,
    iconVector: ImageVector,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "role_card_scale"
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) NavySurface2 else NavySurface
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            brush = if (isSelected) {
                Brush.linearGradient(gradientColors)
            } else {
                SolidColor(NavySurface2)
            }
        ),
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(24.dp))
            .clickable(interactionSource = interactionSource, indication = LocalIndication.current) {
                onClick()
            }
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Large styled background circle for role icon
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(accentColor.copy(alpha = 0.15f), Color.Transparent)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    minLines = 2,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Bullet features
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                features.forEach { feature ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = feature,
                            fontSize = 11.sp,
                            color = TextPrimary.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Single OTP Digit entry box with entry scale and modern state border highlights.
 */
@Composable
fun CodeDigitBox(
    digit: String,
    modifier: Modifier = Modifier,
    index: Int = 0
) {
    var scaleTarget by remember { mutableStateOf(0.4f) }
    LaunchedEffect(digit) {
        delay(index * 40L)
        scaleTarget = 1f
    }
    val scale by animateFloatAsState(
        targetValue = scaleTarget,
        animationSpec = spring(dampingRatio = 0.65f),
        label = "digit_scale"
    )

    Box(
        modifier = modifier
            .size(width = 50.dp, height = 66.dp)
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .background(NavySurface)
            .drawBehind {
                drawRoundRect(
                    color = if (digit.isNotEmpty()) ElectricBlue else NavySurface2,
                    style = Stroke(width = if (digit.isNotEmpty()) 2.dp.toPx() else 1.dp.toPx())
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = digit,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = if (digit.isNotEmpty()) ElectricBlue else TextSecondary,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
    }
}

/**
 * Beautiful Pulsing Dot indicative of state updates or streaming heartbeat rates.
 */
@Composable
fun PulsingDot(
    modifier: Modifier = Modifier,
    color: Color = EmeraldGreen
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_dot_alpha")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )

    Box(
        modifier = modifier
            .size(16.dp)
            .scale(1f + (1f - alphaAnim) * 0.4f),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(color.copy(alpha = alphaAnim * 0.4f))
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}

/**
 * Top floating bar showing system title and pairing/room identifier parameters.
 */
@Composable
fun StatusBar(
    title: String,
    roomCode: String,
    isConnected: Boolean,
    modifier: Modifier = Modifier,
    timerText: String = ""
) {
    val statusColor = if (isConnected) EmeraldGreen else CoralRed
    val pulseColor by rememberInfiniteTransition(label = "pulse").animateColor(
        initialValue = statusColor,
        targetValue = statusColor.copy(alpha = 0.2f),
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_color"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurface.copy(alpha = 0.85f)),
        border = BorderStroke(1.dp, NavySurface2),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(pulseColor)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Portal Room: $roomCode",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            if (timerText.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NavySurface2)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = timerText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp,
                        color = ElectricBlue
                    )
                }
            }
        }
    }
}

/**
 * Floating rounded control bar displayed at bottom of streamed viewscreen.
 */
@Composable
fun ControlBar(
    isAudioMuted: Boolean,
    onAudioMuteToggle: () -> Unit,
    onDisconnectClick: () -> Unit,
    modifier: Modifier = Modifier,
    additionalActions: @Composable (RowScope.() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurface.copy(alpha = 0.90f)),
        border = BorderStroke(1.dp, NavySurface2),
        modifier = modifier
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .wrapContentWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Audio toggle icon with active indicator
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isAudioMuted) CoralRed.copy(alpha = 0.15f) else NavySurface2)
                    .clickable { onAudioMuteToggle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isAudioMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                    contentDescription = null,
                    tint = if (isAudioMuted) CoralRed else ElectricBlue,
                    modifier = Modifier.size(22.dp)
                )
            }

            additionalActions?.invoke(this)

            // Terminate Stream Button
            Button(
                onClick = onDisconnectClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CoralRed,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                modifier = Modifier.height(44.dp)
            ) {
                Text(
                    text = "DISCONNECT",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

fun Modifier.glow(color: Color, radius: Float): Modifier = this.drawBehind {
    // Custom glowing background paint
}
