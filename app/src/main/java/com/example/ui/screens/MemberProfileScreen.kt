package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.MemberPhotoStorage
import com.example.model.BanglaFormatters
import com.example.model.Member
import com.example.ui.components.SectionPanelCard
import com.example.ui.theme.BrandHeroEnd
import com.example.ui.theme.BrandHeroStart
import com.example.ui.theme.ExpenseCrimson
import com.example.ui.theme.ExpenseSurface
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.IncomeSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MemberProfileScreen(
    member: Member,
    transactionCount: Int,
    onUpdateProfile: (
        phone: String,
        email: String,
        address: String,
        profilePhotoPath: String?
    ) -> Unit,
    onOpenChangePassword: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showEditDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val savedPath = withContext(Dispatchers.IO) {
                    MemberPhotoStorage.saveAvatarFromUri(context, member.id, uri)
                }
                if (savedPath != null) {
                    onUpdateProfile(member.phone, member.email, member.address, savedPath)
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SectionPanelCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .testTag("member_profile_avatar_button"),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        MemberAvatarView(
                            name = member.name,
                            photoPath = member.profilePhotoPath,
                            size = 72.dp
                        )
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .border(1.5.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CameraAlt,
                                contentDescription = "ছবি পরিবর্তন করুন",
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = member.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val subtitle = listOfNotNull(
                            member.designation.takeIf { it.isNotBlank() },
                            member.depot.takeIf { it.isNotBlank() }
                        ).joinToString(" • ").ifBlank { "সদস্য প্রোফাইল" }
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (member.isActive) IncomeSurface else ExpenseSurface
                        ) {
                            Text(
                                text = if (member.isActive) "Active (সক্রিয়)" else "Inactive",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (member.isActive) IncomeEmerald else ExpenseCrimson,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { showEditDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("member_edit_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "প্রোফাইল সম্পাদনা",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("আপডেট", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        item {
            SectionPanelCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "ব্যক্তিগত তথ্য ও প্রোফাইল",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                ProfileListDivider()

                MemberDetailListRow(
                    icon = Icons.Outlined.QrCode,
                    label = "কোড (Code)",
                    value = member.code.ifBlank { "-" }
                )
                ProfileListDivider()

                MemberDetailListRow(
                    icon = Icons.Outlined.Person,
                    label = "নাম (Login ID)",
                    value = member.name
                )
                ProfileListDivider()

                MemberDetailListRow(
                    icon = Icons.Outlined.Badge,
                    label = "পদবি (Desig:)",
                    value = member.designation.ifBlank { "-" }
                )
                ProfileListDivider()

                MemberDetailListRow(
                    icon = Icons.Outlined.LocationOn,
                    label = "এরিয়া (Area)",
                    value = member.area.ifBlank { "-" }
                )
                ProfileListDivider()

                MemberDetailListRow(
                    icon = Icons.Outlined.Storefront,
                    label = "ডিপো (Depot)",
                    value = member.depot.ifBlank { "-" }
                )
                ProfileListDivider()

                MemberDetailListRow(
                    icon = Icons.Outlined.Phone,
                    label = "ফোন নম্বর",
                    value = member.phone.ifBlank { "-" }
                )
                ProfileListDivider()

                MemberDetailListRow(
                    icon = Icons.Outlined.Email,
                    label = "ইমেইল (Email)",
                    value = member.email.ifBlank { "-" }
                )
                ProfileListDivider()

                MemberDetailListRow(
                    icon = Icons.Outlined.Home,
                    label = "ঠিকানা (Address)",
                    value = member.address.ifBlank { "-" }
                )
                ProfileListDivider()

                MemberDetailListRow(
                    icon = Icons.Outlined.CalendarMonth,
                    label = "যোগদানের তারিখ",
                    value = if (member.joiningDate.isNotBlank()) {
                        BanglaFormatters.formatBanglaDate(member.joiningDate)
                    } else {
                        "-"
                    }
                )
                ProfileListDivider()

                MemberDetailListRow(
                    icon = Icons.Outlined.VerifiedUser,
                    label = "মোট সংরক্ষিত লেনদেন",
                    value = "${BanglaFormatters.toBanglaDigits(transactionCount.toString())} টি"
                )
            }
        }

        item {
            SectionPanelCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "অ্যাকাউন্ট ও নিরাপত্তা",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                ProfileListDivider()

                MemberActionListRow(
                    icon = Icons.Outlined.Lock,
                    title = "পাসওয়ার্ড পরিবর্তন",
                    subtitle = if (member.isDefaultPassword) {
                        "বর্তমান ডিফল্ট পাসওয়ার্ড: ${member.name}"
                    } else {
                        "আপনার লগইন পাসওয়ার্ড পরিবর্তন করুন"
                    },
                    iconBg = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.primary,
                    titleColor = MaterialTheme.colorScheme.onSurface,
                    onClick = onOpenChangePassword,
                    testTag = "member_change_password_button"
                )
                ProfileListDivider()

                MemberActionListRow(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    title = "লগআউট",
                    subtitle = "অ্যাপ সেশন বন্ধ করে লগইন স্ক্রিনে ফিরুন",
                    iconBg = ExpenseSurface,
                    iconTint = ExpenseCrimson,
                    titleColor = MaterialTheme.colorScheme.error,
                    onClick = onLogout,
                    testTag = "member_logout_button"
                )
            }
        }
    }

    if (showEditDialog) {
        MemberEditProfileDialog(
            member = member,
            onDismiss = { showEditDialog = false },
            onPickPhoto = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onSave = { phone, email, address ->
                onUpdateProfile(phone, email, address, null)
                showEditDialog = false
            }
        )
    }
}

@Composable
fun MemberAvatarView(
    name: String,
    photoPath: String,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(photoPath) { MemberPhotoStorage.loadBitmapFromPath(photoPath) }
    val initials = remember(name) {
        name.trim().split(" ").filter { it.isNotBlank() }.take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifBlank { "M" }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "$name প্রোফাইল ছবি",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), CircleShape)
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(BrandHeroStart, BrandHeroEnd))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                style = if (size <= 44.dp) {
                    MaterialTheme.typography.labelLarge
                } else {
                    MaterialTheme.typography.titleLarge
                },
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun MemberEditProfileDialog(
    member: Member,
    onDismiss: () -> Unit,
    onPickPhoto: () -> Unit,
    onSave: (phone: String, email: String, address: String) -> Unit
) {
    var phone by remember(member) { mutableStateOf(member.phone) }
    var email by remember(member) { mutableStateOf(member.email) }
    var address by remember(member) { mutableStateOf(member.address) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("প্রোফাইল আপডেট করুন") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onPickPhoto,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("প্রোফাইল ছবি নির্বাচন করুন")
                }

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("ফোন নম্বর (Phone)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_edit_phone_input")
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("ইমেইল (Email)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_edit_email_input")
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("ঠিকানা (Address)") },
                    maxLines = 2,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_edit_address_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(phone, email, address) },
                modifier = Modifier.testTag("profile_edit_save_button")
            ) {
                Text("সংরক্ষণ করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}

@Composable
private fun ProfileListDivider() {
    HorizontalDivider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
    )
}

@Composable
private fun MemberDetailListRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 11.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun MemberActionListRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconBg: Color,
    iconTint: Color,
    titleColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = titleColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}
