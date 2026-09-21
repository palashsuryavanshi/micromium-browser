package com.example.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BrowserTab
import com.example.privacy.BlockCategory
import com.example.privacy.BlockedEvent
import com.example.privacy.ShieldConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyShieldSheet(
    sheetState: SheetState,
    activeTab: BrowserTab?,
    shieldConfig: ShieldConfig,
    onConfigChange: (ShieldConfig) -> Unit,
    onClearSiteData: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showBlockedLog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.testTag("privacy_shield_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header: Shield Icon + Master Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (shieldConfig.isShieldEnabled) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (shieldConfig.isShieldEnabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(verticalArrangement = Arrangement.Center) {
                        Text(
                            text = "Privacy Shield",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Switch(
                    checked = shieldConfig.isShieldEnabled,
                    onCheckedChange = { onConfigChange(shieldConfig.copy(isShieldEnabled = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MaterialTheme.colorScheme.secondary
                    ),
                    modifier = Modifier.testTag("master_shield_switch")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Plain-language verdict: one sentence anyone can understand.
            val pageBlockedTotal = (activeTab?.totalBlockedOnPage ?: 0)
            val verdictText = when {
                !shieldConfig.isShieldEnabled ->
                    "Protection is off — ads and trackers on this site can follow you."
                pageBlockedTotal == 0 ->
                    "Nothing blocked yet — this page looks clean so far."
                pageBlockedTotal == 1 ->
                    "Blocked 1 hidden ad or tracker on this page — it can't follow you."
                else ->
                    "Blocked $pageBlockedTotal hidden ads and trackers on this page — they can't follow you."
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("shield_verdict"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (shieldConfig.isShieldEnabled)
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                    else MaterialTheme.colorScheme.error.copy(alpha = 0.10f)
                )
            ) {
                Text(
                    text = verdictText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(14.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Page Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = (activeTab?.pageBlockedAds ?: 0).toString(),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            text = "Ads stopped",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(30.dp)
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = (activeTab?.pageBlockedTrackers ?: 0).toString(),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        )
                        Text(
                            text = "Trackers stopped",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(30.dp)
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    val blockedTotal = (activeTab?.totalBlockedOnPage ?: 0)
                    val estSavingsKb = blockedTotal * 140
                    val estSavingsDisplay = if (estSavingsKb >= 1024) "%.1f MB".format(estSavingsKb / 1024f) else "$estSavingsKb KB"
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = estSavingsDisplay,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        )
                        Text(
                            text = "Data saved",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Granular Protection Toggles
            Text(
                text = "Protection Controls",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            ShieldToggleItem(
                icon = Icons.Default.Block,
                title = "Block ads",
                subtitle = "Hides banners, pop-ups and video ads",
                checked = shieldConfig.blockAds,
                enabled = shieldConfig.isShieldEnabled,
                onCheckedChange = { onConfigChange(shieldConfig.copy(blockAds = it)) },
                testTag = "toggle_block_ads"
            )

            ShieldToggleItem(
                icon = Icons.Default.SmartDisplay,
                title = "Block YouTube ads",
                subtitle = "Separate switch — turning it on may make videos load slowly",
                checked = shieldConfig.blockYouTubeAds,
                enabled = shieldConfig.isShieldEnabled,
                onCheckedChange = { onConfigChange(shieldConfig.copy(blockYouTubeAds = it)) },
                testTag = "toggle_block_youtube_ads"
            )

            ShieldToggleItem(
                icon = Icons.Default.Fingerprint,
                title = "Block trackers",
                subtitle = "Stops hidden code that follows you across sites",
                checked = shieldConfig.blockTrackers,
                enabled = shieldConfig.isShieldEnabled,
                onCheckedChange = { onConfigChange(shieldConfig.copy(blockTrackers = it)) },
                testTag = "toggle_block_trackers"
            )

            ShieldToggleItem(
                icon = Icons.Default.Link,
                title = "Clean tracking links",
                subtitle = "Removes hidden tracking tags from links before you open them",
                checked = shieldConfig.stripTrackingParams,
                enabled = shieldConfig.isShieldEnabled,
                onCheckedChange = { onConfigChange(shieldConfig.copy(stripTrackingParams = it)) },
                testTag = "toggle_strip_params"
            )

            ShieldToggleItem(
                icon = Icons.Default.Cookie,
                title = "Block third-party cookies",
                subtitle = "Stops other companies' cookies from following you around",
                checked = shieldConfig.blockThirdPartyCookies,
                enabled = shieldConfig.isShieldEnabled,
                onCheckedChange = { onConfigChange(shieldConfig.copy(blockThirdPartyCookies = it)) },
                testTag = "toggle_block_cookies"
            )

            ShieldToggleItem(
                icon = Icons.Default.CleaningServices,
                title = "Tidy up blocked ads",
                subtitle = "Closes the empty gaps left behind by blocked ads",
                checked = shieldConfig.cosmeticFiltering,
                enabled = shieldConfig.isShieldEnabled,
                onCheckedChange = { onConfigChange(shieldConfig.copy(cosmeticFiltering = it)) },
                testTag = "toggle_cosmetic_filtering"
            )

            ShieldToggleItem(
                icon = Icons.Default.Lock,
                title = "Always use secure connection",
                subtitle = "Automatically upgrades sites to the encrypted (https) version",
                checked = shieldConfig.forceHttps,
                enabled = shieldConfig.isShieldEnabled,
                onCheckedChange = { onConfigChange(shieldConfig.copy(forceHttps = it)) },
                testTag = "toggle_force_https"
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Real-time blocked resources log for this page
            val blockedEvents = activeTab?.blockedEvents ?: emptyList()
            if (blockedEvents.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showBlockedLog = !showBlockedLog }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Stopped on this site (${blockedEvents.size})",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = if (showBlockedLog) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        AnimatedVisibility(visible = showBlockedLog) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp)
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                blockedEvents.forEach { event ->
                                    BlockedLogEntry(event)
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Clear site cache / data button
            OutlinedButton(
                onClick = onClearSiteData,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("clear_site_data_button"),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Forget this site")
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ShieldToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled && checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 0.8f else 0.4f),
                fontSize = 11.sp
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.secondary
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun BlockedLogEntry(event: BlockedEvent) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val (badgeText, badgeColor) = when (event.category) {
            BlockCategory.AD -> "AD" to MaterialTheme.colorScheme.primary
            BlockCategory.TRACKER -> "TRACKER" to MaterialTheme.colorScheme.secondary
            BlockCategory.FINGERPRINT -> "FP" to Color(0xFFE53935)
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(badgeColor.copy(alpha = 0.18f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = badgeText,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = badgeColor
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = event.url,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 11.sp
        )
    }
}
