package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BrowserTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabGridSwitcher(
    tabs: List<BrowserTab>,
    activeTabId: String,
    thumbnails: Map<String, ImageBitmap>,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onNewTab: (incognito: Boolean) -> Unit,
    onCloseVisibleTabs: (incognito: Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Regular vs incognito pages; defaults to the active tab's mode on open.
    var showIncognito by remember(activeTabId) {
        mutableStateOf(tabs.firstOrNull { it.id == activeTabId }?.isIncognito == true)
    }
    val visibleTabs = remember(tabs, showIncognito) {
        tabs.filter { it.isIncognito == showIncognito }
    }
    val regularCount = tabs.count { !it.isIncognito }
    val incognitoCount = tabs.size - regularCount

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (showIncognito) "Incognito ($incognitoCount)"
                            else "Tabs ($regularCount)",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                navigationIcon = {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("tab_switcher_close")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close tab switcher")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onNewTab(showIncognito) },
                        modifier = Modifier.testTag("tab_switcher_new_tab")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "New Tab")
                    }
                    if (visibleTabs.size > 1) {
                        IconButton(
                            onClick = { onCloseVisibleTabs(showIncognito) },
                            modifier = Modifier.testTag("tab_switcher_close_all")
                        ) {
                            Icon(Icons.Outlined.DeleteSweep, contentDescription = "Close all tabs")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNewTab(showIncognito) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(if (showIncognito) "New Incognito Tab" else "New Tab") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("fab_new_tab")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Regular / incognito pages
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ModeToggleChip(
                    selected = !showIncognito,
                    onClick = { showIncognito = false },
                    testTag = "tab_mode_regular",
                    modifier = Modifier.weight(1f),
                    content = { Text("Tabs ($regularCount)") }
                )
                ModeToggleChip(
                    selected = showIncognito,
                    onClick = { showIncognito = true },
                    testTag = "tab_mode_incognito",
                    modifier = Modifier.weight(1f),
                    content = {
                        Icon(
                            imageVector = Icons.Outlined.VisibilityOff,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Incognito ($incognitoCount)")
                    }
                )
            }

            if (visibleTabs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (showIncognito) "No incognito tabs open"
                        else "No tabs open",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(visibleTabs, key = { _, tab -> tab.id }) { index, tab ->
                        val isActive = tab.id == activeTabId
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(
                                animationSpec = tween(durationMillis = 220, delayMillis = (index * 45).coerceAtMost(270))
                            ) + scaleIn(
                                initialScale = 0.92f,
                                animationSpec = tween(durationMillis = 220, delayMillis = (index * 45).coerceAtMost(270))
                            )
                        ) {
                            DismissibleTabCard(
                                tab = tab,
                                isActive = isActive,
                                thumbnail = thumbnails[tab.id],
                                onSelect = { onSelectTab(tab.id) },
                                onClose = { onCloseTab(tab.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeToggleChip(
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected) MaterialTheme.colorScheme.surfaceVariant
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
private fun DismissibleTabCard(
    tab: BrowserTab,
    isActive: Boolean,
    thumbnail: ImageBitmap?,
    onSelect: () -> Unit,
    onClose: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) {
                onClose()
            }
            true
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            // Intentionally empty: no colored box behind the tile while swiping.
            Box(modifier = Modifier.fillMaxSize())
        },
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true
    ) {
        TabCard(
            tab = tab,
            isActive = isActive,
            thumbnail = thumbnail,
            onSelect = onSelect,
            onClose = onClose
        )
    }
}

@Composable
private fun TabCard(
    tab: BrowserTab,
    isActive: Boolean,
    thumbnail: ImageBitmap?,
    onSelect: () -> Unit,
    onClose: () -> Unit
) {
    val borderColor = if (isActive) Color(0xFF9AA0AA) else Color(0xFF5B626B)
    val borderWidth = if (isActive) 2.dp else 1.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .border(borderWidth, borderColor, RoundedCornerShape(16.dp))
            .clickable { onSelect() }
            .testTag("tab_card_${tab.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            // Opaque grey: translucency lets the swipe-to-dismiss red
            // background bleed through and tint the tile pink.
            containerColor = if (isActive) {
                Color(0xFF424750)
            } else {
                Color(0xFF353B43)
            }
        )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Card Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isActive) Color(0xFF4A515B) else Color(0xFF3B4149)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (tab.isIncognito) {
                        Icon(
                            imageVector = Icons.Outlined.VisibilityOff,
                            contentDescription = "Incognito",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = if (tab.isStartPage) "New Tab" else tab.title,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(24.dp)
                        .testTag("close_tab_${tab.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close tab",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Card Body: full-bleed live page preview, grey when nothing loaded
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF6B7280))
                    )
                }
            }
        }
    }
}
