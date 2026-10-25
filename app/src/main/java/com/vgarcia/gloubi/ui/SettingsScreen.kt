package com.vgarcia.gloubi.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.ManageAccounts
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vgarcia.gloubi.BuildConfig
import com.vgarcia.gloubi.ui.theme.GloubiBackground
import com.vgarcia.gloubi.ui.theme.GloubiGreen
import com.vgarcia.gloubi.ui.theme.GloubiOrange
import com.vgarcia.gloubi.ui.theme.GloubiRed
import com.vgarcia.gloubi.ui.theme.GloubiSurface
import com.vgarcia.gloubi.ui.theme.GloubiSurface2
import com.vgarcia.gloubi.ui.theme.GloubiTheme

// URL à remplacer par votre vraie politique de confidentialité
private const val PRIVACY_POLICY_URL = "https://votre-site.com/privacy"
private const val TERMS_URL          = "https://votre-site.com/terms"

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = GloubiGreen,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = GloubiSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    iconTint: Color = GloubiGreen,
    title: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing?.invoke()
    }
}

@Composable
private fun SettingsDivider() {
    Divider(
        modifier = Modifier.padding(start = 68.dp),
        color = GloubiSurface2,
        thickness = 1.dp
    )
}

@Composable
fun SettingsScreen(
    notificationsEnabled: Boolean,
    onToggleNotifications: (Boolean) -> Unit,
    onOpenConsentForm: () -> Unit,
    onResetProgress: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Réinitialiser la progression ?") },
            text = { Text("Toute votre progression, vos niveaux et vos gains seront supprimés définitivement. Cette action est irréversible.") },
            confirmButton = {
                Button(
                    onClick = { showResetDialog = false; onResetProgress() },
                    colors = ButtonDefaults.buttonColors(containerColor = GloubiRed)
                ) { Text("Réinitialiser") }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Annuler") }
            },
            containerColor = GloubiSurface
        )
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(GloubiBackground, Color(0xFF0E1A12))))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, "Retour", tint = Color.White)
                }
                Text(
                    "Paramètres",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            // Notifications
            SettingsSection("Notifications") {
                SettingsRow(
                    icon = Icons.Rounded.Notifications,
                    title = "Alertes de faim",
                    subtitle = "Notifier quand Gloubi a faim ou est vide",
                    trailing = {
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = onToggleNotifications,
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = GloubiGreen)
                        )
                    }
                )
                if (!notificationsEnabled) {
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Rounded.Notifications,
                        iconTint = GloubiOrange,
                        title = "Ouvrir les paramètres système",
                        subtitle = "Activer les notifications pour cette appli",
                        trailing = { Icon(Icons.Rounded.OpenInBrowser, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            })
                        }
                    )
                }
            }

            // Confidentialité
            SettingsSection("Confidentialité & Publicités") {
                SettingsRow(
                    icon = Icons.Rounded.ManageAccounts,
                    iconTint = GloubiOrange,
                    title = "Gérer mes choix publicitaires",
                    subtitle = "Modifier vos préférences RGPD",
                    trailing = { Icon(Icons.Rounded.OpenInBrowser, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)) },
                    onClick = onOpenConsentForm
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Rounded.Gavel,
                    title = "Politique de confidentialité",
                    trailing = { Icon(Icons.Rounded.OpenInBrowser, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)) },
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL))) }
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Rounded.Gavel,
                    title = "Conditions d'utilisation",
                    trailing = { Icon(Icons.Rounded.OpenInBrowser, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)) },
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(TERMS_URL))) }
                )
            }

            // Progression
            SettingsSection("Progression") {
                SettingsRow(
                    icon = Icons.Rounded.Delete,
                    iconTint = GloubiRed,
                    title = "Réinitialiser la progression",
                    subtitle = "Supprimer tous vos niveaux et gains",
                    onClick = { showResetDialog = true }
                )
            }

            // Infos
            SettingsSection("À propos") {
                SettingsRow(
                    icon = Icons.Rounded.Gavel,
                    title = "Version",
                    subtitle = BuildConfig.VERSION_NAME
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PreviewSettings() {
    GloubiTheme {
        SettingsScreen(
            notificationsEnabled = true,
            onToggleNotifications = {},
            onOpenConsentForm = {},
            onResetProgress = {},
            onBack = {}
        )
    }
}