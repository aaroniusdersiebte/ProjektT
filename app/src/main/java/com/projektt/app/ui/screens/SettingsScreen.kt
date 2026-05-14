package com.projektt.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.projektt.app.data.repository.WidgetListSetting
import com.projektt.app.ui.theme.ProjektTTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = ProjektTTheme.colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Widget Einstellungen",
                        style = ProjektTTheme.typography.titleLarge,
                        color = ProjektTTheme.colors.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zurueck",
                            tint = ProjektTTheme.colors.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ProjektTTheme.colors.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "> LISTEN IM WIDGET",
                style = ProjektTTheme.typography.titleMedium,
                color = ProjektTTheme.colors.onBackgroundDim,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Text(
                text = "Aktiviere/deaktiviere Listen. Pfeile aendern Reihenfolge.",
                style = ProjektTTheme.typography.labelSmall,
                color = ProjektTTheme.colors.onBackgroundDim,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ProjektTTheme.colors.primary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        items = uiState.listSettings,
                        key = { _, item -> item.listId }
                    ) { index, setting ->
                        ListSettingItem(
                            setting = setting,
                            isFirst = index == 0,
                            isLast = index == uiState.listSettings.lastIndex,
                            onToggle = { enabled ->
                                viewModel.toggleListEnabled(setting.listId, enabled)
                            },
                            onMoveUp = { viewModel.moveList(index, index - 1) },
                            onMoveDown = { viewModel.moveList(index, index + 1) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ListSettingItem(
    setting: WidgetListSetting,
    isFirst: Boolean,
    isLast: Boolean,
    onToggle: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ProjektTTheme.colors.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Up/Down Buttons
            Column {
                IconButton(
                    onClick = onMoveUp,
                    enabled = !isFirst,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        contentDescription = "Nach oben",
                        tint = if (!isFirst) ProjektTTheme.colors.primary else ProjektTTheme.colors.surface,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = !isLast,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Nach unten",
                        tint = if (!isLast) ProjektTTheme.colors.primary else ProjektTTheme.colors.surface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = setting.listTitle,
                style = ProjektTTheme.typography.bodyMedium,
                color = if (setting.isEnabled)
                    ProjektTTheme.colors.onBackground
                else
                    ProjektTTheme.colors.onBackgroundDim,
                modifier = Modifier.weight(1f)
            )

            Switch(
                checked = setting.isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ProjektTTheme.colors.background,
                    checkedTrackColor = ProjektTTheme.colors.primary,
                    uncheckedThumbColor = ProjektTTheme.colors.onBackgroundDim,
                    uncheckedTrackColor = ProjektTTheme.colors.surface
                )
            )
        }
    }
}
