package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.BanglaFormatters
import com.example.model.Member
import com.example.ui.components.SectionPanelCard
import com.example.ui.theme.BrandHeroEnd
import com.example.ui.theme.BrandHeroStart
import com.example.ui.theme.ExpenseCrimson
import com.example.ui.theme.ExpenseSurface
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.IncomeSurface

@Composable
fun MemberProfileScreen(
    member: Member,
    transactionCount: Int,
    onOpenChangePassword: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                    MemberAvatarView(
                        name = member.name,
                        size = 72.dp
                    )

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
                        ).joinToString(" • ").ifBlank { "Member Profile" }
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
                                text = if (member.isActive) "Active" else "Inactive",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (member.isActive) IncomeEmerald else ExpenseCrimson,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            SectionPanelCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Personal Info & Profile",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                ProfileListDivider()

                MemberDetailListRow(
                    icon = Icons.Outlined.QrCode,
                    label = "Code (Login ID)",
                    value = member.code.ifBlank { "-" }
                )
                ProfileListDivider()

                MemberDetailListRow(
                    icon = Icons.Outlined.Person,
                    label = "Name",
                    value = member.name
                )
                ProfileListDivider()

                MemberDetailListRow(
                    icon = Icons.Outlined.Badge,
                    label = "Designation",
                    value = member.designation.ifBlank { "-" }
                )
                ProfileListDivider()

                MemberDetailListRow(
                    icon = Icons.Outlined.LocationOn,
                    label = "Area",
                    value = member.area.ifBlank { "-" }
                )
                ProfileListDivider()

                MemberDetailListRow(
                    icon = Icons.Outlined.Storefront,
                    label = "Depot",
                    value = member.depot.ifBlank { "-" }
                )
                ProfileListDivider()

                MemberDetailListRow(
                    icon = Icons.Outlined.Phone,
                    label = "Phone",
                    value = member.phone.ifBlank { "-" }
                )
                ProfileListDivider()

                MemberDetailListRow(
                    icon = Icons.Outlined.Email,
                    label = "Email",
                    value = member.email.ifBlank { "-" }
                )
                ProfileListDivider()

                MemberDetailListRow(
                    icon = Icons.Outlined.Home,
                    label = "Address",
                    value = member.address.ifBlank { "-" }
                )
                ProfileListDivider()

                MemberDetailListRow(
                    icon = Icons.Outlined.CalendarMonth,
                    label = "Joining Date",
                    value = if (member.joiningDate.isNotBlank()) {
                        BanglaFormatters.formatBanglaDate(member.joiningDate)
                    } else {
                        "-"
                    }
                )
                ProfileListDivider()

                MemberDetailListRow(
                    icon = Icons.Outlined.VerifiedUser,
                    label = "Saved Transactions",
                    value = transactionCount.toString()
                )
            }
        }

        item {
            SectionPanelCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Account & Security",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                ProfileListDivider()

                MemberActionListRow(
                    icon = Icons.Outlined.Lock,
                    title = "Change Password",
                    subtitle = if (member.isDefaultPassword) {
                        "Current default password: ${member.code.ifBlank { "-" }}"
                    } else {
                        "Update your login password"
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
                    title = "Log Out",
                    subtitle = "Lock session and return to sign-in screen",
                    iconBg = ExpenseSurface,
                    iconTint = ExpenseCrimson,
                    titleColor = MaterialTheme.colorScheme.error,
                    onClick = onLogout,
                    testTag = "member_logout_button"
                )
            }
        }
    }
}

@Composable
fun MemberAvatarView(
    name: String,
    photoPath: String = "",
    size: Dp,
    modifier: Modifier = Modifier
) {
    val initials = remember(name) {
        name.trim().split(" ").filter { it.isNotBlank() }.take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifBlank { "M" }
    }

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
