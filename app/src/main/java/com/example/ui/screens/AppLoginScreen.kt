package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.AppFrostedGlassBackground
import com.example.ui.components.GlassCardSurface
import com.example.ui.components.GlossyInnerMetricTile
import com.example.ui.components.NetaiBrandLogoBadge
import com.example.ui.components.hisab3DButtonSurface
import com.example.ui.components.hisab3DFieldDepth
import com.example.ui.components.hisabFormFieldColors
import com.example.ui.theme.BrandHeroStart
import com.example.ui.theme.DisplayGlassFont
import com.example.ui.theme.ExpenseCrimson
import com.example.ui.theme.ExpenseSurface
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonCyan

@Composable
fun AppLoginScreen(
    onLoginAttempt: (loginId: String, password: String) -> String?,
    initialLoginId: String = "",
    modifier: Modifier = Modifier
) {
    var loginId by remember(initialLoginId) { mutableStateOf(initialLoginId) }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun submitLogin() {
        if (loginId.isBlank() || password.isEmpty()) {
            errorMessage = "Please enter your Login ID and password."
            return
        }
        errorMessage = onLoginAttempt(loginId, password)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AppFrostedGlassBackground()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GlassCardSurface(
                accentColor = NeonCyan,
                secondaryGlow = NeonBlue,
                modifier = Modifier
                    .widthIn(max = 460.dp)
                    .testTag("app_login_card")
            ) {
                NetaiBrandLogoBadge(
                    size = 76.dp,
                    glowColor = NeonCyan,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .testTag("app_login_logo")
                )

                Spacer(modifier = Modifier.height(10.dp))

                IridescentGlassDepotTitle(
                    title = stringResource(R.string.login_depot_title),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = loginId,
                    onValueChange = {
                        loginId = it
                        errorMessage = null
                    },
                    label = { Text("Login ID / Member Code") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = hisabFormFieldColors(accentColor = NeonCyan),
                    modifier = Modifier
                        .fillMaxWidth()
                        .hisab3DFieldDepth(accentColor = NeonCyan)
                        .testTag("app_login_id_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    label = { Text("Password") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { submitLogin() }),
                    shape = RoundedCornerShape(12.dp),
                    colors = hisabFormFieldColors(accentColor = NeonCyan),
                    modifier = Modifier
                        .fillMaxWidth()
                        .hisab3DFieldDepth(accentColor = NeonCyan)
                        .testTag("app_login_password_input")
                )

                if (!errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    GlossyInnerMetricTile(
                        accentColor = ExpenseCrimson,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ExpenseCrimson,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { submitLogin() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .hisab3DButtonSurface(accentColor = BrandHeroStart, cornerRadius = 14.dp)
                        .testTag("app_login_submit_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandHeroStart,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Login,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sign In",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun IridescentGlassDepotTitle(
    title: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rgb_depot_title")
    val huePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rgb_hue_phase"
    )
    val shimmerPulse by infiniteTransition.animateFloat(
        initialValue = 0.78f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rgb_shimmer_pulse"
    )

    // Bright, luminous pastel-crystal tones so every letter remains razor-sharp and unmistakable
    val crispFillColors = listOf(
        Color.hsv((huePhase + 0f) % 360f, 0.28f, 1f, 1f),
        Color(0xFFFFFFFF),
        Color.hsv((huePhase + 115f) % 360f, 0.26f, 1f, 1f),
        Color(0xFFF6F9FF),
        Color.hsv((huePhase + 230f) % 360f, 0.30f, 1f, 1f)
    )
    val borderColors = listOf(
        Color.hsv((huePhase + 20f) % 360f, 0.55f, 1f, 0.65f),
        Color(0x99FFFFFF),
        Color.hsv((huePhase + 190f) % 360f, 0.55f, 1f, 0.65f)
    )
    val haloColor = Color.hsv((huePhase + 40f) % 360f, 0.60f, 1f, 0.16f * shimmerPulse)
    val textFillBrush = Brush.horizontalGradient(colors = crispFillColors)
    val cardBorderBrush = Brush.horizontalGradient(colors = borderColors)

    BoxWithConstraints(
        modifier = modifier.testTag("login_depot_title"),
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current
        val fontSizeSp = remember(maxWidth) {
            when {
                maxWidth < 280.dp -> 21.sp
                maxWidth < 340.dp -> 25.sp
                else -> 28.sp
            }
        }
        val crispOutlinePx = with(density) { 1.4.dp.toPx() }

        val baseStyle = TextStyle(
            fontFamily = DisplayGlassFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = fontSizeSp,
            lineHeight = (fontSizeSp.value * 1.15f).sp,
            letterSpacing = 1.6.sp,
            textAlign = TextAlign.Center
        )

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF091224).copy(alpha = 0.82f),
            border = BorderStroke(1.2.dp, cardBorderBrush),
            shadowElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(haloColor, Color.Transparent),
                                center = Offset(size.width * 0.5f, size.height * 0.5f),
                                radius = size.width * 0.52f
                            ),
                            center = Offset(size.width * 0.5f, size.height * 0.5f),
                            radius = size.width * 0.52f
                        )
                    }
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                // Crisp dark edge outline behind the bright fill for clean separation
                Text(
                    text = title,
                    maxLines = 1,
                    style = baseStyle.copy(
                        color = Color(0xE605020E),
                        drawStyle = Stroke(
                            width = crispOutlinePx,
                            join = StrokeJoin.Round
                        )
                    ),
                    modifier = Modifier
                        .offset(y = 1.5.dp)
                        .clearAndSetSemantics { }
                )

                // Sharp, luminous crystal fill (accessible text node)
                Text(
                    text = title,
                    maxLines = 1,
                    style = baseStyle.copy(
                        brush = textFillBrush,
                        shadow = Shadow(
                            color = Color(0xCC060210),
                            offset = Offset(0f, 2.5f),
                            blurRadius = 4f
                        )
                    )
                )
            }
        }
    }
}
