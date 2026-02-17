package com.streaming.sunne.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.streaming.sunne.ui.adaptive.*
import com.streaming.sunne.ui.theme.*
import com.streaming.sunne.viewmodel.AuthViewModel
import com.streaming.sunne.viewmodel.UiState

// ══════════════════════════════════════════════════════════════════════════════
//  LOGIN SCREEN
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val authState    by viewModel.authState.collectAsState()
    val ad            = rememberAdaptive()
    val focusManager = LocalFocusManager.current

    var username        by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(authState) {
        if (authState is UiState.Success) onLoginSuccess()
    }

    val errorMessage = (authState as? UiState.Error)?.message
    val isLoading    = authState is UiState.Loading

    if (ad.isTV) {
        // ── TV: split-screen ────────────────────────────────────────────────
        TvAuthLayout(
            brandPanel = { TvBrandPanel(subtitle = "Sign in to watch") },
            formPanel  = {
                AuthFormPanel(ad = ad) {
                    AuthFormContent(
                        title        = "Welcome back",
                        subtitle     = "Sign in to continue",
                        errorMessage = errorMessage,
                        isLoading    = isLoading,
                        primaryLabel = "Sign In",
                        ad           = ad,
                        onSubmit     = {
                            if (username.isNotBlank() && password.isNotBlank())
                                viewModel.login(username, password)
                        },
                        bottomContent = {
                            AuthSwitchRow(
                                prompt      = "New to Sunne?",
                                actionLabel = "Sign up",
                                onClick     = onNavigateToRegister
                            )
                        }
                    ) {
                        AuthField(
                            value           = username,
                            onValueChange   = { username = it },
                            label           = "Username",
                            icon            = Icons.Default.Person,
                            ad              = ad,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            )
                        )
                        Spacer(Modifier.height(if (ad.isTV) 16.dp else 12.dp))
                        AuthField(
                            value                = password,
                            onValueChange        = { password = it },
                            label                = "Password",
                            icon                 = Icons.Default.Lock,
                            ad                   = ad,
                            visualTransformation = if (passwordVisible)
                                VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                Icon(
                                    if (passwordVisible) Icons.Default.VisibilityOff
                                    else Icons.Default.Visibility,
                                    null, tint = LightGray,
                                    modifier = Modifier.size(22.dp)
                                        .clickable { passwordVisible = !passwordVisible }
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction    = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = {
                                focusManager.clearFocus()
                                if (username.isNotBlank() && password.isNotBlank())
                                    viewModel.login(username, password)
                            })
                        )
                    }
                }
            }
        )
    } else {
        // ── Phone / Tablet: full-screen ─────────────────────────────────────
        PhoneAuthScaffold(ad = ad) {
            PhoneBrandHeader(title = "SUNNE", subtitle = "Sign in to continue", ad = ad)
            Spacer(Modifier.height(24.dp))

            if (errorMessage != null) AuthErrorBox(errorMessage, ad)
            Spacer(Modifier.height(if (errorMessage != null) 12.dp else 0.dp))

            AuthField(username, { username = it }, "Username", Icons.Default.Person, ad,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )
            Spacer(Modifier.height(12.dp))
            AuthField(
                value = password, onValueChange = { password = it },
                label = "Password", icon = Icons.Default.Lock, ad = ad,
                visualTransformation = if (passwordVisible)
                    VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    Icon(
                        if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        null, tint = LightGray,
                        modifier = Modifier.size(22.dp).clickable { passwordVisible = !passwordVisible }
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    if (username.isNotBlank() && password.isNotBlank()) viewModel.login(username, password)
                })
            )
            Spacer(Modifier.height(20.dp))
            AuthSubmitButton("Sign In", isLoading, ad) {
                if (username.isNotBlank() && password.isNotBlank()) viewModel.login(username, password)
            }
            Spacer(Modifier.height(16.dp))
            AuthSwitchRow("New to Sunne?", "Sign up now", onNavigateToRegister)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  REGISTER SCREEN
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val authState    by viewModel.authState.collectAsState()
    val ad            = rememberAdaptive()
    val focusManager = LocalFocusManager.current

    var fullName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(authState) {
        if (authState is UiState.Success) onRegisterSuccess()
    }

    val errorMessage = (authState as? UiState.Error)?.message
    val isLoading    = authState is UiState.Loading

    val fields: @Composable () -> Unit = {
        AuthField(fullName, { fullName = it }, "Full Name (optional)", Icons.Default.Person, ad,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )
        Spacer(Modifier.height(if (ad.isTV) 16.dp else 12.dp))
        AuthField(username, { username = it }, "Username", Icons.Default.AccountCircle, ad,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )
        Spacer(Modifier.height(if (ad.isTV) 16.dp else 12.dp))
        AuthField(email, { email = it }, "Email", Icons.Default.Email, ad,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )
        Spacer(Modifier.height(if (ad.isTV) 16.dp else 12.dp))
        AuthField(
            value = password, onValueChange = { password = it },
            label = "Password (min 6 chars)", icon = Icons.Default.Lock, ad = ad,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                viewModel.register(username, email, password, fullName.takeIf { it.isNotBlank() })
            })
        )
    }

    if (ad.isTV) {
        TvAuthLayout(
            brandPanel = { TvBrandPanel(subtitle = "Start watching today") },
            formPanel  = {
                AuthFormPanel(ad = ad) {
                    AuthFormContent(
                        title        = "Create account",
                        subtitle     = "Join Sunne for free",
                        errorMessage = errorMessage,
                        isLoading    = isLoading,
                        primaryLabel = "Create Account",
                        ad           = ad,
                        onSubmit     = {
                            viewModel.register(username, email, password, fullName.takeIf { it.isNotBlank() })
                        },
                        bottomContent = {
                            AuthSwitchRow("Already a member?", "Sign in", onNavigateToLogin)
                        },
                        fieldContent = fields
                    )
                }
            }
        )
    } else {
        PhoneAuthScaffold(ad = ad) {
            PhoneBrandHeader("SUNNE", "Create your account", ad)
            Spacer(Modifier.height(24.dp))

            if (errorMessage != null) { AuthErrorBox(errorMessage, ad); Spacer(Modifier.height(12.dp)) }

            fields()

            Spacer(Modifier.height(20.dp))
            AuthSubmitButton("Create Account", isLoading, ad) {
                viewModel.register(username, email, password, fullName.takeIf { it.isNotBlank() })
            }
            Spacer(Modifier.height(16.dp))
            AuthSwitchRow("Already a member?", "Sign in", onNavigateToLogin)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  LAYOUT SHELLS
// ══════════════════════════════════════════════════════════════════════════════

// ── TV: left brand panel + right form panel ───────────────────────────────────
@Composable
private fun TvAuthLayout(
    brandPanel: @Composable () -> Unit,
    formPanel:  @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxSize().background(DarkBackground)
    ) {
        // Left brand column — 45% width
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.45f)
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF1A0000),
                            DarkBackground.copy(alpha = 0.95f)
                        )
                    )
                )
        ) {
            // Decorative circles
            Box(
                Modifier.size(400.dp).offset((-80).dp, (-80).dp)
                    .clip(CircleShape)
                    .background(NetflixRed.copy(alpha = 0.06f))
            )
            Box(
                Modifier.size(300.dp)
                    .align(Alignment.BottomEnd)
                    .offset(60.dp, 60.dp)
                    .clip(CircleShape)
                    .background(NetflixRed.copy(alpha = 0.04f))
            )
            brandPanel()
        }

        // Thin divider
        Box(
            Modifier.fillMaxHeight().width(1.dp)
                .background(White.copy(alpha = 0.06f))
        )

        // Right form column — 55% width
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.55f)
                .background(Color(0xFF0A0A0A))
        ) {
            formPanel()
        }
    }
}

// ── Phone: centered scrollable column ────────────────────────────────────────
@Composable
private fun PhoneAuthScaffold(
    ad: Adaptive,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
            .background(DarkBackground)
            .background(
                Brush.radialGradient(
                    colors = listOf(NetflixRed.copy(alpha = 0.07f), Color.Transparent),
                    radius = 900f
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ad.pad, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  TV BRAND PANEL
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun TvBrandPanel(subtitle: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.3f,
        targetValue   = 0.6f,
        animationSpec = infiniteRepeatable(tween(2500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "glow"
    )

    Column(
        modifier            = Modifier.fillMaxSize().padding(56.dp),
        verticalArrangement = Arrangement.Center
    ) {
        // Animated glow dot
        Box(
            modifier = Modifier
                .size(10.dp).clip(CircleShape)
                .background(NetflixRed.copy(alpha = glowAlpha))
        )
        Spacer(Modifier.height(20.dp))

        // Logo
        Text(
            "SUNNE",
            fontSize      = sp(64),
            fontWeight    = FontWeight.Black,
            color         = White,
            letterSpacing = sp(8)
        )

        // Red underline accent
        Box(
            modifier = Modifier
                .width(80.dp).height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(NetflixRed)
        )

        Spacer(Modifier.height(24.dp))

        Text(
            subtitle,
            fontSize   = sp(20),
            color      = LightGray,
            lineHeight = sp(30)
        )

        Spacer(Modifier.height(48.dp))

        // Feature bullets
        listOf(
            "✦  Stream in 4K Ultra HD",
            "✦  Download & watch offline",
            "✦  Unlimited movies & shows"
        ).forEach { feature ->
            Text(feature, fontSize = sp(15), color = LightGray.copy(alpha = 0.65f),
                modifier = Modifier.padding(bottom = 10.dp))
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  PHONE BRAND HEADER
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun PhoneBrandHeader(title: String, subtitle: String, ad: Adaptive) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            title,
            fontSize      = if (ad.isPhone) sp(44) else sp(52),
            fontWeight    = FontWeight.Black,
            color         = NetflixRed,
            letterSpacing = sp(6)
        )
        Spacer(Modifier.height(6.dp))
        Box(Modifier.width(48.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(NetflixRed.copy(0.5f)))
        Spacer(Modifier.height(10.dp))
        Text(subtitle, fontSize = sp(15), color = LightGray, textAlign = TextAlign.Center)
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  TV FORM PANEL WRAPPER
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun AuthFormPanel(ad: Adaptive, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 64.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.Center,
        content             = content
    )
}

// ── TV form content (title + fields + button + bottom link) ──────────────────
@Composable
private fun AuthFormContent(
    title:        String,
    subtitle:     String,
    errorMessage: String?,
    isLoading:    Boolean,
    primaryLabel: String,
    ad:           Adaptive,
    onSubmit:     () -> Unit,
    bottomContent: @Composable () -> Unit,
    fieldContent:  @Composable () -> Unit
) {
    // Title
    Text(title, fontSize = sp(32), fontWeight = FontWeight.Black, color = White)
    Spacer(Modifier.height(6.dp))
    Text(subtitle, fontSize = sp(16), color = LightGray)
    Spacer(Modifier.height(32.dp))

    if (errorMessage != null) {
        AuthErrorBox(errorMessage, ad)
        Spacer(Modifier.height(20.dp))
    }

    fieldContent()

    Spacer(Modifier.height(28.dp))
    AuthSubmitButton(primaryLabel, isLoading, ad, onSubmit)
    Spacer(Modifier.height(20.dp))
    bottomContent()
}

// ══════════════════════════════════════════════════════════════════════════════
//  SHARED ATOMS
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun AuthField(
    value:                String,
    onValueChange:        (String) -> Unit,
    label:                String,
    icon:                 ImageVector,
    ad:                   Adaptive,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon:         (@Composable () -> Unit)? = null,
    keyboardOptions:      KeyboardOptions = KeyboardOptions.Default,
    keyboardActions:      KeyboardActions = KeyboardActions.Default
) {
    OutlinedTextField(
        value                = value,
        onValueChange        = onValueChange,
        label                = {
            Text(label, color = LightGray, fontSize = if (ad.isTV) sp(15) else sp(13))
        },
        leadingIcon = {
            Icon(icon, null, tint = LightGray,
                modifier = Modifier.size(if (ad.isTV) 22.dp else 20.dp))
        },
        trailingIcon         = trailingIcon,
        visualTransformation = visualTransformation,
        keyboardOptions      = keyboardOptions,
        keyboardActions      = keyboardActions,
        singleLine           = true,
        modifier             = Modifier
            .fillMaxWidth()
            .then(if (ad.isTV) Modifier.height(64.dp) else Modifier),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            textColor            = White,
            backgroundColor      = Color(0xFF141414),
            focusedBorderColor   = NetflixRed,
            unfocusedBorderColor = MediumGray,
            cursorColor          = NetflixRed,
            focusedLabelColor    = NetflixRed,
            leadingIconColor     = LightGray
        ),
        shape = RoundedCornerShape(if (ad.isTV) 10.dp else 8.dp)
    )
}

@Composable
private fun AuthSubmitButton(
    label:     String,
    isLoading: Boolean,
    ad:        Adaptive,
    onClick:   () -> Unit
) {
    val height: Dp = if (ad.isTV) 58.dp else 52.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(if (ad.isTV) 10.dp else 8.dp))
            .background(if (isLoading) NetflixRed.copy(alpha = 0.5f) else NetflixRed)
            .clickable(enabled = !isLoading) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color       = White,
                modifier    = Modifier.size(26.dp),
                strokeWidth = 2.5.dp
            )
        } else {
            Text(
                label,
                fontSize   = if (ad.isTV) sp(17) else sp(16),
                fontWeight = FontWeight.Bold,
                color      = White
            )
        }
    }
}

@Composable
private fun AuthErrorBox(message: String, ad: Adaptive) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(NetflixRed.copy(alpha = 0.12f))
            .border(1.dp, NetflixRed.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Default.ErrorOutline, null, tint = NetflixRed,
            modifier = Modifier.size(18.dp))
        Text(message, fontSize = if (ad.isTV) sp(14) else sp(13), color = NetflixRed)
    }
}

@Composable
private fun AuthSwitchRow(prompt: String, actionLabel: String, onClick: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text("$prompt ", fontSize = sp(14), color = LightGray)
        Text(
            actionLabel,
            fontSize   = sp(14),
            color      = White,
            fontWeight = FontWeight.Bold,
            modifier   = Modifier.clickable { onClick() }
        )
    }
}



//package com.streaming.sunne.ui.screens
//
//import androidx.compose.animation.core.*
//import androidx.compose.foundation.background
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.foundation.text.KeyboardActions
//import androidx.compose.foundation.text.KeyboardOptions
//import androidx.compose.material.*
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.*
//import androidx.compose.material.icons.filled.Visibility
//import androidx.compose.material.icons.filled.VisibilityOff
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.focus.FocusDirection
//import androidx.compose.ui.graphics.Brush
//import androidx.compose.ui.platform.LocalFocusManager
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.input.*
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import com.streaming.sunne.ui.theme.*
//import com.streaming.sunne.viewmodel.AuthViewModel
//import com.streaming.sunne.viewmodel.UiState
//
//// ─── LoginScreen ───────────────────────────────────────────────────────────────
//
//@Composable
//fun LoginScreen(
//    viewModel: AuthViewModel,
//    onLoginSuccess: () -> Unit,
//    onNavigateToRegister: () -> Unit
//) {
//    val authState by viewModel.authState.collectAsState()
//    val focusManager = LocalFocusManager.current
//
//    var username by remember { mutableStateOf("") }
//    var password by remember { mutableStateOf("") }
//    var passwordVisible by remember { mutableStateOf(false) }
//
//    // Navigate on success
//    LaunchedEffect(authState) {
//        if (authState is UiState.Success) onLoginSuccess()
//    }
//
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(DarkBackground)
//    ) {
//        // Background gradient
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .background(
//                    Brush.radialGradient(
//                        colors = listOf(
//                            NetflixRed.copy(alpha = 0.08f),
//                            DarkBackground
//                        ),
//                        radius = 1200f
//                    )
//                )
//        )
//
//        Column(
//            modifier = Modifier
//                .align(Alignment.Center)
//                .width(420.dp)
//                .padding(32.dp),
//            horizontalAlignment = Alignment.CenterHorizontally,
//            verticalArrangement = Arrangement.spacedBy(20.dp)
//        ) {
//            // Logo
//            Text(
//                "SUNNE",
//                fontSize = 52.sp,
//                fontWeight = FontWeight.Black,
//                color = NetflixRed,
//                letterSpacing = 8.sp
//            )
//
//            Text(
//                "Sign in to continue",
//                fontSize = 16.sp,
//                color = LightGray
//            )
//
//            Spacer(Modifier.height(8.dp))
//
//            // Error message
//            if (authState is UiState.Error) {
//                Box(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .clip(RoundedCornerShape(8.dp))
//                        .background(NetflixRed.copy(alpha = 0.15f))
//                        .padding(12.dp)
//                ) {
//                    Text(
//                        (authState as UiState.Error).message,
//                        fontSize = 14.sp,
//                        color = NetflixRed
//                    )
//                }
//            }
//
//            // Username field
//            AuthTextField(
//                value = username,
//                onValueChange = { username = it },
//                label = "Username",
//                leadingIcon = Icons.Default.Person,
//                keyboardOptions = KeyboardOptions(
//                    imeAction = ImeAction.Next
//                ),
//                keyboardActions = KeyboardActions(
//                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
//                )
//            )
//
//            // Password field
//            AuthTextField(
//                value = password,
//                onValueChange = { password = it },
//                label = "Password",
//                leadingIcon = Icons.Default.Lock,
//                visualTransformation = if (passwordVisible)
//                    VisualTransformation.None else PasswordVisualTransformation(),
//                trailingIcon = {
//                    Icon(
//                        if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
//                        contentDescription = null,
//                        tint = LightGray,
//                        modifier = Modifier
//                            .size(22.dp)
//                            .clickable { passwordVisible = !passwordVisible }
//                    )
//                },
//                keyboardOptions = KeyboardOptions(
//                    keyboardType = KeyboardType.Password,
//                    imeAction = ImeAction.Done
//                ),
//                keyboardActions = KeyboardActions(
//                    onDone = {
//                        focusManager.clearFocus()
//                        if (username.isNotBlank() && password.isNotBlank()) {
//                            viewModel.login(username, password)
//                        }
//                    }
//                )
//            )
//
//            Spacer(Modifier.height(4.dp))
//
//            // Sign In button
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(52.dp)
//                    .clip(RoundedCornerShape(8.dp))
//                    .background(
//                        if (authState is UiState.Loading) NetflixRed.copy(alpha = 0.5f)
//                        else NetflixRed
//                    )
//                    .clickable(enabled = authState !is UiState.Loading) {
//                        if (username.isNotBlank() && password.isNotBlank()) {
//                            viewModel.login(username, password)
//                        }
//                    },
//                contentAlignment = Alignment.Center
//            ) {
//                if (authState is UiState.Loading) {
//                    CircularProgressIndicator(
//                        color = White,
//                        modifier = Modifier.size(24.dp),
//                        strokeWidth = 2.dp
//                    )
//                } else {
//                    Text(
//                        "Sign In",
//                        fontSize = 17.sp,
//                        fontWeight = FontWeight.Bold,
//                        color = White
//                    )
//                }
//            }
//
//            // Register link
//            Row {
//                Text("New to Sunne? ", fontSize = 14.sp, color = LightGray)
//                Text(
//                    "Sign up now",
//                    fontSize = 14.sp,
//                    color = White,
//                    fontWeight = FontWeight.Bold,
//                    modifier = Modifier.clickable { onNavigateToRegister() }
//                )
//            }
//        }
//    }
//}
//
//// ─── RegisterScreen ────────────────────────────────────────────────────────────
//
//@Composable
//fun RegisterScreen(
//    viewModel: AuthViewModel,
//    onRegisterSuccess: () -> Unit,
//    onNavigateToLogin: () -> Unit
//) {
//    val authState by viewModel.authState.collectAsState()
//    val focusManager = LocalFocusManager.current
//
//    var username by remember { mutableStateOf("") }
//    var email    by remember { mutableStateOf("") }
//    var password by remember { mutableStateOf("") }
//    var fullName by remember { mutableStateOf("") }
//
//    LaunchedEffect(authState) {
//        if (authState is UiState.Success) onRegisterSuccess()
//    }
//
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(DarkBackground),
//        contentAlignment = Alignment.Center
//    ) {
//        Column(
//            modifier = Modifier
//                .width(420.dp)
//                .padding(32.dp),
//            horizontalAlignment = Alignment.CenterHorizontally,
//            verticalArrangement = Arrangement.spacedBy(16.dp)
//        ) {
//            Text("SUNNE", fontSize = 48.sp, fontWeight = FontWeight.Black,
//                color = NetflixRed, letterSpacing = 8.sp)
//            Text("Create your account", fontSize = 16.sp, color = LightGray)
//
//            Spacer(Modifier.height(4.dp))
//
//            if (authState is UiState.Error) {
//                Box(
//                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
//                        .background(NetflixRed.copy(alpha = 0.15f)).padding(12.dp)
//                ) {
//                    Text((authState as UiState.Error).message, fontSize = 14.sp, color = NetflixRed)
//                }
//            }
//
//            AuthTextField(value = fullName, onValueChange = { fullName = it },
//                label = "Full Name (optional)", leadingIcon = Icons.Default.Person,
//                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
//                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }))
//
//            AuthTextField(value = username, onValueChange = { username = it },
//                label = "Username", leadingIcon = Icons.Default.AccountCircle,
//                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
//                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }))
//
//            AuthTextField(value = email, onValueChange = { email = it },
//                label = "Email", leadingIcon = Icons.Default.Email,
//                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
//                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }))
//
//            AuthTextField(value = password, onValueChange = { password = it },
//                label = "Password (min 6 chars)", leadingIcon = Icons.Default.Lock,
//                visualTransformation = PasswordVisualTransformation(),
//                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
//                keyboardActions = KeyboardActions(onDone = {
//                    focusManager.clearFocus()
//                    viewModel.register(username, email, password, fullName.takeIf { it.isNotBlank() })
//                }))
//
//            Box(
//                modifier = Modifier.fillMaxWidth().height(52.dp)
//                    .clip(RoundedCornerShape(8.dp))
//                    .background(if (authState is UiState.Loading) NetflixRed.copy(alpha = 0.5f) else NetflixRed)
//                    .clickable(enabled = authState !is UiState.Loading) {
//                        viewModel.register(username, email, password, fullName.takeIf { it.isNotBlank() })
//                    },
//                contentAlignment = Alignment.Center
//            ) {
//                if (authState is UiState.Loading) {
//                    CircularProgressIndicator(color = White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
//                } else {
//                    Text("Create Account", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = White)
//                }
//            }
//
//            Row {
//                Text("Already have an account? ", fontSize = 14.sp, color = LightGray)
//                Text("Sign in", fontSize = 14.sp, color = White, fontWeight = FontWeight.Bold,
//                    modifier = Modifier.clickable { onNavigateToLogin() })
//            }
//        }
//    }
//}
//
//// ─── Shared AuthTextField ──────────────────────────────────────────────────────
//
//@Composable
//private fun AuthTextField(
//    value: String,
//    onValueChange: (String) -> Unit,
//    label: String,
//    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
//    visualTransformation: VisualTransformation = VisualTransformation.None,
//    trailingIcon: (@Composable () -> Unit)? = null,
//    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
//    keyboardActions: KeyboardActions = KeyboardActions.Default
//) {
//    OutlinedTextField(
//        value = value,
//        onValueChange = onValueChange,
//        label = { Text(label, color = LightGray, fontSize = 14.sp) },
//        leadingIcon = {
//            Icon(leadingIcon, contentDescription = null, tint = LightGray, modifier = Modifier.size(20.dp))
//        },
//        trailingIcon = trailingIcon,
//        visualTransformation = visualTransformation,
//        keyboardOptions = keyboardOptions,
//        keyboardActions = keyboardActions,
//        singleLine = true,
//        modifier = Modifier.fillMaxWidth(),
//        colors = TextFieldDefaults.outlinedTextFieldColors(
//            textColor = White,
//            backgroundColor = CardBackground,
//            focusedBorderColor = NetflixRed,
//            unfocusedBorderColor = MediumGray,
//            cursorColor = NetflixRed
//        ),
//        shape = RoundedCornerShape(8.dp)
//    )
//}