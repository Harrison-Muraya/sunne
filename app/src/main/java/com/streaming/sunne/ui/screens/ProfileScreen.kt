package com.streaming.sunne.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.streaming.sunne.ui.theme.*
import com.streaming.sunne.viewmodel.AuthViewModel
import com.streaming.sunne.viewmodel.ProfileViewModel
import com.streaming.sunne.viewmodel.UiState

@Composable
fun ProfileScreen(
    authViewModel:    AuthViewModel,
    profileViewModel: ProfileViewModel,
    onBackClick:      () -> Unit,
    onLogout:         () -> Unit
) {
    val user        by authViewModel.currentUser.collectAsState()
    val updateState by profileViewModel.updateState.collectAsState()
    val forYou      by profileViewModel.forYou.collectAsState()

    var editMode    by remember { mutableStateOf(false) }
    var emailInput  by remember { mutableStateOf(user?.email ?: "") }
    var nameInput   by remember { mutableStateOf(user?.fullName ?: "") }
    var passInput   by remember { mutableStateOf("") }
    var passVisible by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Sync email/name when user loads
    LaunchedEffect(user) {
        emailInput = user?.email ?: ""
        nameInput  = user?.fullName ?: ""
    }

    Column(
        modifier = Modifier.fillMaxSize().background(DarkBackground)
            .verticalScroll(rememberScrollState())
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.ArrowBack, "Back", tint = White,
                    modifier = Modifier.size(24.dp).clickable { onBackClick() })
                Text("Profile", fontSize = sp(20), fontWeight = FontWeight.Bold, color = White)
            }
            if (!editMode) {
                Icon(Icons.Default.Edit, "Edit", tint = LightGray,
                    modifier = Modifier.size(22.dp).clickable { editMode = true })
            }
        }

        // Avatar + name section
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.fillMaxWidth().padding(vertical = 24.dp)
        ) {
            Box(
                modifier = Modifier.size(84.dp).clip(CircleShape).background(NetflixRed),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    user?.username?.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    fontSize   = sp(36),
                    fontWeight = FontWeight.Bold,
                    color      = White
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(user?.username ?: "", fontSize = sp(22), fontWeight = FontWeight.Bold, color = White)
            if (user?.isAdmin == true) {
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier.clip(RoundedCornerShape(4.dp)).background(NetflixRed.copy(0.2f))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text("ADMIN", fontSize = sp(11), color = NetflixRed, fontWeight = FontWeight.Bold)
                }
            }
            Text(user?.email ?: "", fontSize = sp(14), color = LightGray, modifier = Modifier.padding(top = 4.dp))
        }

        // Edit form
        if (editMode) {
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text("Edit Profile", fontSize = sp(16), fontWeight = FontWeight.SemiBold, color = White)
                Spacer(Modifier.height(12.dp))

                ProfileTextField(label = "Full Name", value = nameInput, onValueChange = { nameInput = it })
                Spacer(Modifier.height(10.dp))
                ProfileTextField(label = "Email", value = emailInput, onValueChange = { emailInput = it })
                Spacer(Modifier.height(10.dp))
                ProfileTextField(
                    label          = "New Password (optional)",
                    value          = passInput,
                    onValueChange  = { passInput = it },
                    visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon   = {
                        Icon(
                            if (passVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            null, tint = LightGray,
                            modifier = Modifier.size(20.dp).clickable { passVisible = !passVisible }
                        )
                    }
                )
                Spacer(Modifier.height(16.dp))

                if (updateState is UiState.Error) {
                    Text((updateState as UiState.Error).message, color = NetflixRed, fontSize = sp(13))
                    Spacer(Modifier.height(8.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF2A2A2A))
                            .clickable { editMode = false }.padding(horizontal = 20.dp, vertical = 12.dp)
                    ) { Text("Cancel", color = LightGray, fontSize = sp(15)) }

                    Box(
                        Modifier.clip(RoundedCornerShape(8.dp)).background(NetflixRed)
                            .clickable {
                                profileViewModel.updateProfile(
                                    email    = emailInput.ifBlank { null },
                                    fullName = nameInput.ifBlank { null },
                                    password = passInput.ifBlank { null }
                                )
                                editMode = false
                            }
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        if (updateState is UiState.Loading) {
                            CircularProgressIndicator(color = White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Save", color = White, fontSize = sp(15), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }

        Divider(color = White.copy(0.07f), modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(20.dp))

        // Stats
        if (user != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatBox("Member since", user!!.createdAt.take(10))
                StatBox("Last Login", user?.lastLogin?.take(10) ?: "—")
            }
            Spacer(Modifier.height(20.dp))
        }

        Divider(color = White.copy(0.07f), modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(12.dp))

        // Sign out
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showLogoutDialog = true }
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.Logout, null, tint = NetflixRed, modifier = Modifier.size(20.dp))
            Text("Sign Out", fontSize = sp(15), color = NetflixRed)
        }

        Spacer(Modifier.height(32.dp))
    }

    // Logout confirmation
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            backgroundColor  = Color(0xFF1E1E1E),
            contentColor     = White,
            title   = { Text("Sign Out", fontWeight = FontWeight.Bold) },
            text    = { Text("Are you sure you want to sign out?", color = LightGray) },
            confirmButton = {
                TextButton(onClick = {
                    authViewModel.logout()
                    onLogout()
                }) { Text("Sign Out", color = NetflixRed) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = LightGray)
                }
            }
        )
    }
}

@Composable
private fun StatBox(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = sp(15), fontWeight = FontWeight.Bold, color = White)
        Text(label, fontSize = sp(11), color = LightGray)
    }
}

@Composable
private fun ProfileTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value          = value,
        onValueChange  = onValueChange,
        label          = { Text(label, color = LightGray) },
        colors         = TextFieldDefaults.outlinedTextFieldColors(
            textColor             = White,
            focusedBorderColor    = NetflixRed,
            unfocusedBorderColor  = LightGray.copy(0.3f),
            cursorColor           = NetflixRed,
            focusedLabelColor     = NetflixRed
        ),
        visualTransformation = visualTransformation,
        trailingIcon   = trailingIcon,
        modifier       = Modifier.fillMaxWidth(),
        singleLine     = true
    )
}

private fun sp(value: Int)   = androidx.compose.ui.unit.TextUnit(value.toFloat(), androidx.compose.ui.unit.TextUnitType.Sp)
private fun sp(value: Float) = androidx.compose.ui.unit.TextUnit(value, androidx.compose.ui.unit.TextUnitType.Sp)
