package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import android.media.RingtoneManager
import android.widget.Toast
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Market
import com.example.model.MarketStatus
import com.example.model.MarketStatusDetails
import com.example.viewmodel.MarketUiState
import com.example.viewmodel.MarketViewModel
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MarketClockScreen(
    viewModel: MarketViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.notificationEvents.collect { marketName ->
            try {
                val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = RingtoneManager.getRingtone(context, notificationUri)
                ringtone?.play()
                Toast.makeText(context, "$marketName Stock Market is now OPEN!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Base surface in Premium Elegant Sophisticated Dark theme
    Surface(
        modifier = modifier.fillMaxSize(),
        color = if (uiState.darkTheme) Color(0xFF1C1B1F) else Color(0xFFF4F5F7) // Deep obsidian or light body background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Screen header
            AppHeader(uiState = uiState, onToggleLive = { viewModel.setLiveMode(it) })

            Spacer(modifier = Modifier.height(16.dp))

            // Timezone selectors
            TimezoneSelectorRow(
                uiState = uiState,
                onSelectZone = { viewModel.selectReferenceTimezone(it) }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // The main visual Clock-like structure
            ClockCard(
                uiState = uiState,
                viewModel = viewModel
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Playback scrubber timeline container
            ScrubberController(
                uiState = uiState,
                onScrubberChange = { viewModel.updateScrubberHour(it) },
                onToggleLive = { viewModel.setLiveMode(it) }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Section divider with list of markets
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Global Stock Exchanges",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.darkTheme) Color.White else Color.Black
                    )
                )
                
                Text(
                    text = "${uiState.markets.size} Tracked",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.LightGray.copy(alpha = 0.6f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Markets listings column (using individual cards)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.markets.forEach { market ->
                    val statusDetails = viewModel.getMarketStatus(market)
                    val activeArcs = viewModel.getMarketActiveArcs(market)
                    val isSelected = uiState.selectedMarketId == market.id
                    
                    MarketRowCard(
                        market = market,
                        statusDetails = statusDetails,
                        activeArcs = activeArcs,
                        isSelected = isSelected,
                        isSoundEnabled = uiState.soundEnabledMarketIds.contains(market.id),
                        isDark = uiState.darkTheme,
                        onToggleSound = { viewModel.toggleSoundNotification(market.id) },
                        onClick = { viewModel.selectMarket(market.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun AppHeader(
    uiState: MarketUiState,
    onToggleLive: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "GLOBAL VIEW",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD0BCFF),
                    letterSpacing = 2.sp
                )
            )
            
            Text(
                text = "Market Hours",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = if (uiState.darkTheme) Color.White else Color.Black
                ),
                modifier = Modifier.testTag("app_title")
            )
        }

        // Mode indicator pill (Pulsing live indicator)
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val liveAlpha by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse
            ),
            label = "live_indicator"
        )
        
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (uiState.isLiveMode) Color(0x26B6EEA9) else Color(0x26F2B8B5))
                .border(1.dp, if (uiState.isLiveMode) Color(0xFFB6EEA9) else Color(0xFFF2B8B5), RoundedCornerShape(12.dp))
                .clickable { onToggleLive(!uiState.isLiveMode) }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (uiState.isLiveMode) {
                            Color(0xFFB6EEA9).copy(alpha = liveAlpha)
                        } else {
                            Color(0xFFF2B8B5).copy(alpha = liveAlpha)
                        }
                    )
                    .border(
                        1.dp, 
                        if (uiState.isLiveMode) Color(0xFFB6EEA9) else Color(0xFFF2B8B5), 
                        CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (uiState.isLiveMode) "LIVE" else "SANDBOX",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            )
        }
    }
}

@Composable
fun TimezoneSelectorRow(
    uiState: MarketUiState,
    onSelectZone: (Int) -> Unit
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Reference Frame Timezone",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFF938F99)
            ),
            modifier = Modifier.padding(bottom = 6.dp)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            uiState.referenceTimezones.forEachIndexed { index, zone ->
                val isSelected = uiState.selectedReferenceZoneIndex == index
                val borderCol = if (isSelected) Color(0xFFD0BCFF) else Color(0x3349454F)
                val bgCol = if (isSelected) Color(0x1FD0BCFF) else Color(0xFF2B2930)
                val textCol = if (isSelected) Color(0xFFD0BCFF) else Color(0xFFE6E1E5)
                
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(bgCol)
                        .border(1.dp, borderCol, RoundedCornerShape(8.dp))
                        .clickable { onSelectZone(index) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val codeSym = getZoneFlagOrSymbol(zone.name)
                    if (codeSym.isNotEmpty()) {
                        Text(text = codeSym, modifier = Modifier.padding(end = 6.dp))
                    }
                    Text(
                        text = zone.name,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                            color = textCol
                        )
                    )
                }
            }
        }
    }
}

private fun getZoneFlagOrSymbol(zoneName: String): String {
    return when (zoneName) {
        "Local Time" -> "📱"
        "UTC" -> "🌐"
        "New York" -> "🇺🇸"
        "London" -> "🇬🇧"
        "Frankfurt" -> "🇩🇪"
        "Shanghai" -> "🇨🇳"
        "Tokyo" -> "🇯🇵"
        "Hong Kong" -> "🇭🇰"
        "Sydney" -> "🇦🇺"
        "Mumbai" -> "🇮🇳"
        else -> ""
    }
}

@Composable
fun ClockCard(
    uiState: MarketUiState,
    viewModel: MarketViewModel
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 480.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (uiState.darkTheme) Color(0xFF2B2930) else Color.White
        ),
        shape = RoundedCornerShape(24.dp),
        border = BorderStrokeCustom(1.dp, if (uiState.darkTheme) Color(0x4D49454F) else Color(0xFFE0E0E0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // Rendered clock canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                val textMeasurer = rememberTextMeasurer()
                
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("world_market_circular_clock")
                ) {
                    val centerOffset = Offset(size.width / 2f, size.height / 2f)
                    val outerDiameterRadius = size.minDimension / 2f
                    val clockDrawnRadius = outerDiameterRadius * 0.92f
                    
                    // Draw outer border disk
                    drawCircle(
                        color = if (uiState.darkTheme) Color(0xFF313033) else Color(0xFFE0E0E0),
                        radius = clockDrawnRadius,
                        style = Stroke(width = 12.dp.toPx())
                    )
                    drawCircle(
                        color = (if (uiState.darkTheme) Color(0xFF49454F) else Color.Gray).copy(alpha = 0.4f),
                        radius = clockDrawnRadius + 6.dp.toPx(),
                        style = Stroke(width = 1.dp.toPx())
                    )
                    
                    // 1. Draw concentric empty background guides for all tracks
                    val numMarkets = uiState.markets.size
                    val maxTrackRadius = clockDrawnRadius * 0.82f
                    val trackThickness = clockDrawnRadius * 0.046f
                    val trackGap = clockDrawnRadius * 0.016f
                    
                    for (i in 0 until numMarkets) {
                        val r = maxTrackRadius - i * (trackThickness + trackGap)
                        drawCircle(
                            color = if (uiState.darkTheme) Color(0xFF313033) else Color(0xFFEFEFEF), // Silent guide track ring
                            radius = r,
                            style = Stroke(width = trackThickness)
                        )
                    }

                    // 2. Draw active schedules (arcs) for each market
                    uiState.markets.forEachIndexed { i, market ->
                        val r = maxTrackRadius - i * (trackThickness + trackGap)
                        val activeArcs = viewModel.getMarketActiveArcs(market)
                        val status = viewModel.getMarketStatus(market)
                        
                        // Check if currently selected in the UI list to highlight
                        val isSelected = uiState.selectedMarketId == market.id
                        val opacity = if (status.status == MarketStatus.OPEN) 1.0f else (if (status.status == MarketStatus.LUNCH) 0.65f else 0.22f)
                        
                        activeArcs.forEach { arc ->
                            // Arc is in decimal hours [0, 24].
                            // Convert hour to angle: 00:00 is straight UP (-90 degrees)
                            val startAngle = -90.0f + arc.first.toFloat() * 15.0f
                            val sweepAngle = (arc.second - arc.first).toFloat() * 15.0f
                            
                            // Highlight selected ring thicker and brighter
                            val currentTrackWidth = if (isSelected) trackThickness * 1.35f else trackThickness
                            val currentArcColor = if (isSelected) market.color else market.color.copy(alpha = opacity)
                            
                            // Soft glow effect first for open markets or selected market
                            if (status.status == MarketStatus.OPEN || isSelected) {
                                drawArc(
                                    color = market.color.copy(alpha = 0.2f),
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    topLeft = Offset(centerOffset.x - r, centerOffset.y - r),
                                    size = androidx.compose.ui.geometry.Size(r * 2f, r * 2f),
                                    style = Stroke(width = currentTrackWidth * 1.6f, cap = StrokeCap.Round)
                                )
                            }
                            
                            // Normal colored arc session
                            drawArc(
                                color = currentArcColor,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                topLeft = Offset(centerOffset.x - r, centerOffset.y - r),
                                size = androidx.compose.ui.geometry.Size(r * 2f, r * 2f),
                                style = Stroke(width = currentTrackWidth, cap = StrokeCap.Round)
                            )
                        }
                    }

                    // 3. Draw radial Hour tick marks (every hour) and Text labels (every 2 hours)
                    for (h in 0 until 24) {
                        val angleRad = Math.toRadians((-90.0 + h * 15.0)).toFloat()
                        
                        // Smaller or larger tick based on significance
                        val isMajor = h % 4 == 0
                        val isEven = h % 2 == 0
                        
                        val tickStart = if (isMajor) clockDrawnRadius * 0.83f else (if (isEven) clockDrawnRadius * 0.85f else clockDrawnRadius * 0.86f)
                        val tickEnd = clockDrawnRadius * 0.88f
                        
                        val sX = centerOffset.x + cos(angleRad) * tickStart
                        val sY = centerOffset.y + sin(angleRad) * tickStart
                        val eX = centerOffset.x + cos(angleRad) * tickEnd
                        val eY = centerOffset.y + sin(angleRad) * tickEnd
                        
                        val tickColor = when {
                            h == 0 -> Color(0xFFD0BCFF) // Midnight highlight
                            isMajor -> (if (uiState.darkTheme) Color(0xFFE6E1E5) else Color.DarkGray).copy(alpha = 0.5f)
                            else -> (if (uiState.darkTheme) Color(0xFF938F99) else Color.Gray).copy(alpha = 0.3f)
                        }
                        
                        drawLine(
                            color = tickColor,
                            start = Offset(sX, sY),
                            end = Offset(eX, eY),
                            strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx()
                        )
                        
                        // Text hours labels on even numbers
                        if (isEven) {
                            val textRadius = clockDrawnRadius * 0.94f
                            val textLabelX = centerOffset.x + cos(angleRad) * textRadius
                            val textLabelY = centerOffset.y + sin(angleRad) * textRadius
                            
                            val textStr = if (h < 10) "0$h" else "$h"
                            
                            // Distinct color for noon/midnight
                            val textStyle = TextStyle(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (h == 0) Color(0xFFD0BCFF) else (if (uiState.darkTheme) Color(0xFF938F99) else Color.DarkGray)
                            )
                            
                            val textLayoutResult = textMeasurer.measure(
                                text = textStr,
                                style = textStyle
                            )
                            
                            val textW = textLayoutResult.size.width
                            val textH = textLayoutResult.size.height
                            
                            drawText(
                                textLayoutResult,
                                topLeft = Offset(textLabelX - textW / 2f, textLabelY - textH / 2f)
                            )
                        }
                    }
                    
                    // 4. Draw Center Axle Cap (metallic cover)
                    drawCircle(
                        color = if (uiState.darkTheme) Color(0xFF1C1B1F) else Color(0xFFF4F5F7),
                        radius = clockDrawnRadius * 0.12f
                    )
                    drawCircle(
                        color = Color(0xFF49454F),
                        radius = clockDrawnRadius * 0.08f,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // 5. Draw sweeping Needle (The Current Reference Time Hand)
                    val pointerAngleRad = Math.toRadians((-90.0 + uiState.scrubberHour * 15.0)).toFloat()
                    val dialEndMultiplier = clockDrawnRadius * 0.88f
                    
                    val needleEndX = centerOffset.x + cos(pointerAngleRad) * dialEndMultiplier
                    val needleEndY = centerOffset.y + sin(pointerAngleRad) * dialEndMultiplier
                    
                    // Trace a glowing line from center outwards
                    drawLine(
                        color = Color(0xFFD0BCFF).copy(alpha = 0.3f),
                        start = centerOffset,
                        end = Offset(needleEndX, needleEndY),
                        strokeWidth = 5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    
                    drawLine(
                        color = Color(0xFFD0BCFF),
                        start = centerOffset,
                        end = Offset(needleEndX, needleEndY),
                        strokeWidth = 1.8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    
                    // Draw tip node halo
                    drawCircle(
                        color = Color(0xFFD0BCFF).copy(alpha = 0.45f),
                        radius = 8.dp.toPx(),
                        center = Offset(needleEndX, needleEndY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 3.5.dp.toPx(),
                        center = Offset(needleEndX, needleEndY)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Time Display Below Clock Face
            val formatterLocal = DateTimeFormatter.ofPattern("hh:mm:ss a")
            val formattedTime = uiState.currentZonedDateTime.format(formatterLocal)
            val dayOfWeek = uiState.currentZonedDateTime.dayOfWeek.name
            val formattedDate = uiState.currentZonedDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            
            Text(
                text = formattedTime,
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 26.sp,
                    color = if (uiState.darkTheme) Color.White else Color.Black,
                    letterSpacing = 1.sp
                )
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                val refZoneIndex = uiState.selectedReferenceZoneIndex
                val refZoneName = uiState.referenceTimezones.getOrNull(refZoneIndex)?.name ?: "Local"
                val offsetText = uiState.currentReferenceZone.rules.getOffset(uiState.currentInstant).id
                val adjustedOffText = if (offsetText == "Z") "UTC+0" else "UTC$offsetText"

                Text(
                    text = "$dayOfWeek • $formattedDate • $refZoneName ($adjustedOffText)",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFD0BCFF)
                    )
                )
            }
        }
    }
}

@Composable
fun ScrubberController(
    uiState: MarketUiState,
    onScrubberChange: (Double) -> Unit,
    onToggleLive: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 480.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (uiState.darkTheme) Color(0xFF2B2930) else Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStrokeCustom(1.dp, if (uiState.darkTheme) Color(0x4D49454F) else Color(0xFFE0E0E0))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Playback Speed",
                        tint = if (uiState.isLiveMode) Color.Gray else Color(0xFFD0BCFF),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (uiState.isLiveMode) "Timeline (Locked to Live)" else "Interactive Sandbox Time",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.isLiveMode) Color(0xFF938F99) else Color(0xFFD0BCFF)
                        )
                    )
                }
                
                // Switch
                Switch(
                    checked = !uiState.isLiveMode,
                    onCheckedChange = { isSandbox -> onToggleLive(!isSandbox) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFFD0BCFF),
                        checkedTrackColor = Color(0xFF49454F),
                        uncheckedThumbColor = Color(0xFF938F99),
                        uncheckedTrackColor = Color(0xFF211F26)
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Time Slider (only interactive in sandbox/non-live mode)
            val currentSliderHour = uiState.scrubberHour
            
            Column {
                Slider(
                    value = currentSliderHour.toFloat(),
                    onValueChange = { onScrubberChange(it.toDouble()) },
                    valueRange = 0.0f..24.0f,
                    steps = 0,
                    enabled = !uiState.isLiveMode,
                    colors = SliderDefaults.colors(
                        thumbColor = if (uiState.isLiveMode) Color.Gray else Color(0xFFD0BCFF),
                        activeTrackColor = if (uiState.isLiveMode) Color(0xFF49454F) else Color(0xFFD0BCFF),
                        inactiveTrackColor = Color(0xFF313033)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("timeline_hour_slider")
                )

                // Slider axis labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("00:00", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text("06:00", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text("12:00 (Noon)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text("18:00", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text("24:00", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }

            // Quick preset schedules
            if (!uiState.isLiveMode) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Quick Peak Events Preset (Ref Time):",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.LightGray.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Set timezone to Local/UTC then jump hour
                    // Or simple standard presets
                    PresetChip(label = "Asia Wakeup (02:00)", onClick = { onScrubberChange(2.0) })
                    PresetChip(label = "Europe Open (07:00)", onClick = { onScrubberChange(7.0) })
                    PresetChip(label = "NY Open (14:00)", onClick = { onScrubberChange(14.0) })
                    PresetChip(label = "NY Close (20:00)", onClick = { onScrubberChange(20.0) })
                    PresetChip(label = "Overlap Peaks (15:00)", onClick = { onScrubberChange(15.0) })
                }
            }
        }
    }
}

@Composable
fun PresetChip(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF211F26))
            .border(1.dp, Color(0x3349454F), RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFF938F99)
            )
        )
    }
}

@Composable
fun MarketRowCard(
    market: Market,
    statusDetails: MarketStatusDetails,
    activeArcs: List<Pair<Double, Double>>,
    isSelected: Boolean,
    isSoundEnabled: Boolean,
    isDark: Boolean,
    onToggleSound: () -> Unit,
    onClick: () -> Unit
) {
    val containerBorderColor by animateColorAsState(
        targetValue = if (isSelected) market.color else (if (isDark) Color(0x4D49454F) else Color(0xFFE0E0E0)),
        label = "border_color"
    )
    val cardBackground = if (isDark) {
        if (isSelected) Color(0xFF211F26) else Color(0xFF2B2930)
    } else {
        if (isSelected) Color(0xFFE0E0E0) else Color.White
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("market_card_${market.id}"),
        colors = CardDefaults.cardColors(
            containerColor = cardBackground,
        ),
        shape = RoundedCornerShape(14.dp),
        border = BorderStrokeCustom(if (isSelected) 1.5.dp else 1.dp, containerBorderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Market thematic left accent indicator strip (Matches its ring color!)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(market.color)
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Name & Code
                Column(
                    modifier = Modifier.weight(1.3f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = market.flag,
                            style = TextStyle(fontSize = 18.sp),
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = market.code,
                             style = MaterialTheme.typography.titleMedium.copy(
                                 fontWeight = FontWeight.Black,
                                 color = if (isDark) Color.White else Color.Black
                             )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = market.city,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.Gray
                            )
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    Text(
                        text = market.exchange,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF90A4AE)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Local Trade Hours
                    val lunchPeriod = if (market.hasLunchBreak) {
                        " (Lunch: ${market.lunchStartLocal}-${market.lunchEndLocal})"
                    } else ""
                    
                    Text(
                        text = "Local hours: ${market.openTimeLocal} - ${market.closeTimeLocal}$lunchPeriod",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                    )
                }

                // Sound Notification Toggle
                IconButton(
                    onClick = { onToggleSound() },
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Sound Notification for ${market.name}",
                        tint = if (isSoundEnabled) market.color else Color.Gray.copy(alpha = 0.4f)
                    )
                }

                // Status pillar
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    val badgeColor = when (statusDetails.status) {
                        MarketStatus.OPEN -> Color(0xFFB6EEA9)   // Pale lime-green
                        MarketStatus.LUNCH -> Color(0xFFD0BCFF)  // Light Lavender
                        MarketStatus.CLOSED -> Color(0xFFF2B8B5) // Coral soft pink
                    }
                    val badgeBg = when (statusDetails.status) {
                        MarketStatus.OPEN -> Color(0x1FB6EEA9)
                        MarketStatus.LUNCH -> Color(0x1FD0BCFF)
                        MarketStatus.CLOSED -> Color(0x1FF2B8B5)
                    }

                    // Pulsing animation for active OPEN status tag
                    val labelPulsingAlpha = if (statusDetails.status == MarketStatus.OPEN) {
                        val infiniteTransition = rememberInfiniteTransition("status")
                        val blink by infiniteTransition.animateFloat(
                            initialValue = 0.5f,
                            targetValue = 1.0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(800),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulsing"
                        )
                        blink
                    } else 1.0f

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeBg)
                            .border(1.dp, badgeColor.copy(alpha = labelPulsingAlpha), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = statusDetails.statusText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = badgeColor
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = statusDetails.countdownText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (statusDetails.status == MarketStatus.OPEN) Color(0xFFB6EEA9) else Color.White.copy(alpha = 0.7f)
                        ),
                        textAlign = TextAlign.End
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Local: ${statusDetails.currentLocalTimeText.substringBeforeLast(" ")}",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            color = Color.LightGray.copy(alpha = 0.6f)
                        ),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

/**
 * Portable border stroke support for older Jetpack Compose versions
 */
@Composable
fun BorderStrokeCustom(width: androidx.compose.ui.unit.Dp, color: Color): androidx.compose.foundation.BorderStroke {
    return remember(width, color) {
        androidx.compose.foundation.BorderStroke(width, color)
    }
}
