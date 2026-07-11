package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.MarketViewModel
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MarketViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    
    var showOffsetDialog by remember { mutableStateOf(false) }
    var showCustomizeAlarmsDialog by remember { mutableStateOf(false) }
    
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm:ss") }
    val localZonedDateTime = ZonedDateTime.ofInstant(uiState.currentInstant, uiState.customGmtZoneId)
    val formattedTime = localZonedDateTime.format(timeFormatter)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (uiState.darkTheme) Color(0xFF1C1B1F) else Color(0xFFF4F5F7))
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Page Title
        Text(
            text = "Settings",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Black,
                color = if (uiState.darkTheme) Color.White else Color.Black,
                fontSize = 24.sp
            ),
            modifier = Modifier.padding(vertical = 12.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        // Local Time Section
        SectionHeader(
            icon = Icons.Default.Info,
            title = "Local Time",
            isDark = uiState.darkTheme
        )
        Spacer(modifier = Modifier.height(8.dp))
        SettingsCard(isDark = uiState.darkTheme) {
            // Current Time Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Current Time",
                    color = if (uiState.darkTheme) Color.White else Color.Black,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formattedTime,
                    color = if (uiState.darkTheme) Color.LightGray else Color.DarkGray,
                    fontWeight = FontWeight.Bold
                )
            }
            
            HorizontalDivider(color = if (uiState.darkTheme) Color(0xFF333333) else Color(0xFFE0E0E0))
            
            // GMT Offset Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showOffsetDialog = true }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "GMT Offset",
                    color = if (uiState.darkTheme) Color.White else Color.Black,
                    fontWeight = FontWeight.Medium
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val prefix = if (uiState.gmtOffsetHours >= 0) "+" else ""
                    Text(
                        text = "GMT$prefix${uiState.gmtOffsetHours}",
                        color = if (uiState.darkTheme) Color.LightGray else Color.DarkGray,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit GMT Offset",
                        tint = if (uiState.darkTheme) Color.Gray else Color.DarkGray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Alarms Section
        SectionHeader(
            icon = Icons.Default.Notifications,
            title = "Alarms",
            isDark = uiState.darkTheme
        )
        Spacer(modifier = Modifier.height(8.dp))
        SettingsCard(isDark = uiState.darkTheme) {
            // Enable Alarms Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Enable Alarms",
                    color = if (uiState.darkTheme) Color.White else Color.Black,
                    fontWeight = FontWeight.Medium
                )
                Switch(
                    checked = uiState.alarmsEnabled,
                    onCheckedChange = { viewModel.toggleAlarmsEnabled() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF34C759), // Vibrant iOS green
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color.LightGray
                    )
                )
            }
            
            HorizontalDivider(color = if (uiState.darkTheme) Color(0xFF333333) else Color(0xFFE0E0E0))
            
            // Customize Alarms Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCustomizeAlarmsDialog = true }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Customize Alarms",
                    color = if (uiState.darkTheme) Color.White else Color.Black,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = Icons.Default.PlayArrow, // Right arrow chevron indicator
                    contentDescription = "Customize Alarms Details",
                    tint = if (uiState.darkTheme) Color.Gray else Color.DarkGray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // General Settings Section
        SectionHeader(
            icon = Icons.Default.Settings,
            title = "Settings",
            isDark = uiState.darkTheme
        )
        Spacer(modifier = Modifier.height(8.dp))
        SettingsCard(isDark = uiState.darkTheme) {
            // Theme Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Theme",
                    color = if (uiState.darkTheme) Color.White else Color.Black,
                    fontWeight = FontWeight.Medium
                )
                Switch(
                    checked = uiState.darkTheme,
                    onCheckedChange = { viewModel.toggleDarkTheme() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF2B2D31),
                        checkedTrackColor = Color(0xFF00E676),
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color.LightGray
                    )
                )
            }
        }
    }

    // Dialog: Edit GMT Offset
    if (showOffsetDialog) {
        var offsetInput by remember { mutableStateOf(uiState.gmtOffsetHours.toString()) }
        var isError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showOffsetDialog = false },
            title = {
                Text(
                    text = "Customize GMT Offset",
                    fontWeight = FontWeight.Bold,
                    color = if (uiState.darkTheme) Color.White else Color.Black
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter a GMT Offset in hours (e.g. +5.5 for India, -8 for Pacific Time):",
                        fontSize = 14.sp,
                        color = if (uiState.darkTheme) Color.LightGray else Color.DarkGray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = offsetInput,
                        onValueChange = {
                            offsetInput = it
                            isError = it.toDoubleOrNull() == null
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text("Offset (hours)") },
                        isError = isError,
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00E676),
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = if (uiState.darkTheme) Color.White else Color.Black,
                            unfocusedTextColor = if (uiState.darkTheme) Color.White else Color.Black
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (isError) {
                        Text(
                            text = "Please enter a valid number",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val parsed = offsetInput.toDoubleOrNull()
                        if (parsed != null && parsed >= -12.0 && parsed <= 14.0) {
                            viewModel.updateGmtOffset(parsed)
                            showOffsetDialog = false
                        } else {
                            isError = true
                        }
                    },
                    enabled = !isError
                ) {
                    Text("OK", color = Color(0xFF00E676), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showOffsetDialog = false }) {
                    Text("Cancel", color = if (uiState.darkTheme) Color.LightGray else Color.DarkGray)
                }
            },
            containerColor = if (uiState.darkTheme) Color(0xFF2D2F34) else Color.White
        )
    }

    // Dialog: Customize specific market notifications
    if (showCustomizeAlarmsDialog) {
        AlertDialog(
            onDismissRequest = { showCustomizeAlarmsDialog = false },
            title = {
                Text(
                    text = "Customize Alarms",
                    fontWeight = FontWeight.Bold,
                    color = if (uiState.darkTheme) Color.White else Color.Black
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Toggle sound notifications when specific markets open:",
                        fontSize = 14.sp,
                        color = if (uiState.darkTheme) Color.LightGray else Color.DarkGray,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    uiState.markets.forEach { market ->
                        val isSoundOn = uiState.soundEnabledMarketIds.contains(market.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = market.flag,
                                    fontSize = 20.sp,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Column {
                                    Text(
                                        text = market.name,
                                        fontWeight = FontWeight.Bold,
                                        color = if (uiState.darkTheme) Color.White else Color.Black,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = market.exchange,
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Switch(
                                checked = isSoundOn,
                                onCheckedChange = { viewModel.toggleSoundNotification(market.id) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = market.color,
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = Color.LightGray
                                )
                            )
                        }
                        HorizontalDivider(color = if (uiState.darkTheme) Color(0xFF3E3F45) else Color(0xFFECEFF1))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCustomizeAlarmsDialog = false }) {
                    Text("Done", color = Color(0xFF00E676), fontWeight = FontWeight.Bold)
                }
            },
            containerColor = if (uiState.darkTheme) Color(0xFF2D2F34) else Color.White
        )
    }
}

@Composable
fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    isDark: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isDark) Color.LightGray else Color.DarkGray,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.LightGray else Color.DarkGray,
                fontSize = 16.sp
            )
        )
    }
}

@Composable
fun SettingsCard(
    isDark: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF2B2D31) else Color.White
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(content = content)
    }
}
