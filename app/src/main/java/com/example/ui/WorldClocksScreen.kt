package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MarketStatus
import com.example.viewmodel.MarketViewModel
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.ZoneId

@Composable
fun WorldClocksScreen(
    viewModel: MarketViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm:ss") }

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
            text = "World Clocks",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Black,
                color = if (uiState.darkTheme) Color.White else Color.Black,
                fontSize = 24.sp
            ),
            modifier = Modifier.padding(vertical = 12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Clocks List
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            uiState.markets.forEach { market ->
                val statusDetails = viewModel.getMarketStatus(market)
                val marketZone = remember(market.timezoneId) { ZoneId.of(market.timezoneId) }
                val marketTime = ZonedDateTime.ofInstant(uiState.currentInstant, marketZone)
                val formattedTime = marketTime.format(timeFormatter)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.darkTheme) Color(0xFF2B2D31) else Color.White
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Column: Market Info
                        Column(modifier = Modifier.weight(1.2f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = market.flag,
                                    fontSize = 20.sp,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                Text(
                                    text = market.city,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (uiState.darkTheme) Color.White else Color.Black
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${market.exchange} (${market.code})",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = market.timezoneId,
                                color = if (uiState.darkTheme) Color.LightGray.copy(alpha = 0.5f) else Color.Gray,
                                fontSize = 11.sp
                            )
                        }

                        // Right Column: Time & Status
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.weight(0.9f)
                        ) {
                            Text(
                                text = formattedTime,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    color = market.color,
                                    fontSize = 20.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            val badgeColor = when (statusDetails.status) {
                                MarketStatus.OPEN -> Color(0xFF4CAF50)
                                MarketStatus.LUNCH -> Color(0xFFAB47BC)
                                MarketStatus.CLOSED -> Color(0xFF78909C)
                            }
                            
                            val badgeTextColor = Color.White

                            Surface(
                                color = badgeColor,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.wrapContentSize()
                            ) {
                                Text(
                                    text = statusDetails.status.name,
                                    color = badgeTextColor,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}
