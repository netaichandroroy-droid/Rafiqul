package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ui.components.hisab3DButtonSurface
import com.example.ui.components.hisab3DFieldDepth
import com.example.ui.components.hisabFormFieldColors
import com.example.ui.theme.BrandHeroStart
import com.example.ui.theme.CardSurfaceSolid
import com.example.ui.theme.ExpenseCrimson
import com.example.ui.theme.GlassTextPrimary
import com.example.ui.theme.GlassTextSecondary
import com.example.ui.theme.NeonCyan

@Composable
fun ChangePasswordDialog(
    currentLoginId: String,
    allowLoginIdEdit: Boolean = true,
    onDismiss: () -> Unit,
    onChangePassword: (
        currentPassword: String,
        newPassword: String,
        newLoginId: String
    ) -> Result<Unit>,
    onSuccess: () -> Unit
) {
    var loginId by remember(currentLoginId) { mutableStateOf(currentLoginId) }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showCurrent by remember { mutableStateOf(false) }
    var showNew by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardSurfaceSolid,
        titleContentColor = GlassTextPrimary,
        textContentColor = GlassTextSecondary,
        title = { Text("Change Password") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = loginId,
                    onValueChange = {
                        if (allowLoginIdEdit) {
                            loginId = it
                            errorMessage = null
                        }
                    },
                    enabled = allowLoginIdEdit,
                    readOnly = !allowLoginIdEdit,
                    label = { Text("Login ID") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = hisabFormFieldColors(accentColor = NeonCyan),
                    modifier = Modifier
                        .fillMaxWidth()
                        .hisab3DFieldDepth(accentColor = NeonCyan)
                        .testTag("change_pw_login_id_input")
                )

                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = {
                        currentPassword = it
                        errorMessage = null
                    },
                    label = { Text("Current Password") },
                    singleLine = true,
                    visualTransformation = if (showCurrent) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showCurrent = !showCurrent }) {
                            Icon(
                                imageVector = if (showCurrent) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (showCurrent) "Hide" else "Show"
                            )
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = hisabFormFieldColors(accentColor = NeonCyan),
                    modifier = Modifier
                        .fillMaxWidth()
                        .hisab3DFieldDepth(accentColor = NeonCyan)
                        .testTag("change_pw_current_input")
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                        errorMessage = null
                    },
                    label = { Text("New Password (min 4 characters)") },
                    singleLine = true,
                    visualTransformation = if (showNew) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showNew = !showNew }) {
                            Icon(
                                imageVector = if (showNew) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (showNew) "Hide" else "Show"
                            )
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = hisabFormFieldColors(accentColor = NeonCyan),
                    modifier = Modifier
                        .fillMaxWidth()
                        .hisab3DFieldDepth(accentColor = NeonCyan)
                        .testTag("change_pw_new_input")
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        errorMessage = null
                    },
                    label = { Text("Confirm New Password") },
                    singleLine = true,
                    visualTransformation = if (showNew) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(12.dp),
                    colors = hisabFormFieldColors(accentColor = NeonCyan),
                    modifier = Modifier
                        .fillMaxWidth()
                        .hisab3DFieldDepth(accentColor = NeonCyan)
                        .testTag("change_pw_confirm_input")
                )

                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = ExpenseCrimson
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newPassword != confirmPassword) {
                        errorMessage = "New passwords do not match."
                        return@Button
                    }
                    val result = onChangePassword(currentPassword, newPassword, loginId)
                    if (result.isSuccess) {
                        onSuccess()
                    } else {
                        errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to update password."
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandHeroStart,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .hisab3DButtonSurface(accentColor = BrandHeroStart, cornerRadius = 12.dp)
                    .testTag("change_pw_submit_button")
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("change_pw_cancel_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
