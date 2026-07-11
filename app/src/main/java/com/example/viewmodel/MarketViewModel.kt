package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.Market
import com.example.model.MarketCalculator
import com.example.model.MarketStatus
import com.example.model.MarketStatusDetails
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

data class ReferenceTimezone(
    val name: String,
    val zoneId: ZoneId,
    val description: String
)

data class MarketUiState(
    val currentInstant: Instant = Instant.now(),
    val isLiveMode: Boolean = true,
    val scrubberHour: Double = 12.0, // Default to noon
    val referenceTimezones: List<ReferenceTimezone> = emptyList(),
    val selectedReferenceZoneIndex: Int = 0,
    val selectedMarketId: String? = null,
    val soundEnabledMarketIds: Set<String> = emptySet(),
    val markets: List<Market> = MarketCalculator.SupportedMarkets,
    val alarmsEnabled: Boolean = true,
    val darkTheme: Boolean = true,
    val gmtOffsetHours: Double = 0.0
) {
    val customGmtZoneId: ZoneId
        get() {
            val totalMinutes = (gmtOffsetHours * 60).toInt()
            val hours = totalMinutes / 60
            val minutes = kotlin.math.abs(totalMinutes % 60)
            val prefix = if (totalMinutes >= 0) "+" else "-"
            val hoursStr = String.format("%02d", kotlin.math.abs(hours))
            val minutesStr = String.format("%02d", minutes)
            return ZoneId.of("$prefix$hoursStr:$minutesStr")
        }

    val currentReferenceZone: ZoneId
        get() {
            val ref = referenceTimezones.getOrNull(selectedReferenceZoneIndex)
            return if (ref?.name == "Local Time") customGmtZoneId else ref?.zoneId ?: ZoneId.systemDefault()
        }

    val currentZonedDateTime: ZonedDateTime
        get() = ZonedDateTime.ofInstant(currentInstant, currentReferenceZone)
}

class MarketViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPreferences = application.getSharedPreferences("market_notifications", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(MarketUiState())
    val uiState: StateFlow<MarketUiState> = _uiState.asStateFlow()

    private val _notificationEvents = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val notificationEvents: SharedFlow<String> = _notificationEvents.asSharedFlow()

    private val lastKnownMarketStatus = mutableMapOf<String, MarketStatus>()
    private var tickerJob: Job? = null

    init {
        val savedMarkets = getSavedSoundEnabledMarkets()
        val savedAlarmsEnabled = sharedPreferences.getBoolean("alarms_enabled", true)
        val savedDarkTheme = sharedPreferences.getBoolean("dark_theme", true)
        val savedGmtOffset = sharedPreferences.getFloat("gmt_offset", 0.0f).toDouble()

        // Initialize reference timezone list
        val localZone = ZoneId.systemDefault()
        val zones = mutableListOf<ReferenceTimezone>()
        
        // Calculate description for local zone
        val totalMinutes = (savedGmtOffset * 60).toInt()
        val hours = totalMinutes / 60
        val minutes = kotlin.math.abs(totalMinutes % 60)
        val prefix = if (totalMinutes >= 0) "+" else "-"
        val offsetStr = "GMT$prefix${kotlin.math.abs(hours)}:${String.format("%02d", minutes)}"
        
        zones.add(ReferenceTimezone("Local Time", localZone, "Custom Local Time ($offsetStr)"))
        zones.add(ReferenceTimezone("UTC", ZoneId.of("UTC"), "Coordinated Universal Time"))
        
        MarketCalculator.SupportedMarkets.forEach { m ->
            val zone = ZoneId.of(m.timezoneId)
            if (zones.none { it.zoneId.id == zone.id }) {
                zones.add(ReferenceTimezone(m.city, zone, "${m.exchange} timezone"))
            }
        }

        _uiState.update { it.copy(
            referenceTimezones = zones,
            selectedMarketId = "nyse",
            soundEnabledMarketIds = savedMarkets,
            alarmsEnabled = savedAlarmsEnabled,
            darkTheme = savedDarkTheme,
            gmtOffsetHours = savedGmtOffset
        ) }

        // Initialize last known statuses
        val now = Instant.now()
        MarketCalculator.SupportedMarkets.forEach { market ->
            lastKnownMarketStatus[market.id] = MarketCalculator.getMarketStatusDetails(market, now).status
        }

        startTicker()
    }

    private fun getSavedSoundEnabledMarkets(): Set<String> {
        return sharedPreferences.getStringSet("enabled_markets", emptySet()) ?: emptySet()
    }

    private fun saveSoundEnabledMarkets(enabledMarkets: Set<String>) {
        sharedPreferences.edit().putStringSet("enabled_markets", enabledMarkets).apply()
    }

    fun toggleAlarmsEnabled() {
        _uiState.update { state ->
            val newVal = !state.alarmsEnabled
            sharedPreferences.edit().putBoolean("alarms_enabled", newVal).apply()
            state.copy(alarmsEnabled = newVal)
        }
    }

    fun toggleDarkTheme() {
        _uiState.update { state ->
            val newVal = !state.darkTheme
            sharedPreferences.edit().putBoolean("dark_theme", newVal).apply()
            state.copy(darkTheme = newVal)
        }
    }

    fun updateGmtOffset(offset: Double) {
        _uiState.update { state ->
            sharedPreferences.edit().putFloat("gmt_offset", offset.toFloat()).apply()
            
            // Recalculate reference timezone list to update descriptions
            val updatedZones = state.referenceTimezones.map { zone ->
                if (zone.name == "Local Time") {
                    val totalMinutes = (offset * 60).toInt()
                    val hours = totalMinutes / 60
                    val minutes = kotlin.math.abs(totalMinutes % 60)
                    val prefix = if (totalMinutes >= 0) "+" else "-"
                    val offsetStr = "GMT$prefix${kotlin.math.abs(hours)}:${String.format("%02d", minutes)}"
                    zone.copy(description = "Custom Local Time ($offsetStr)")
                } else {
                    zone
                }
            }

            state.copy(gmtOffsetHours = offset, referenceTimezones = updatedZones)
        }
    }

    fun toggleSoundNotification(marketId: String) {
        _uiState.update { state ->
            val currentSet = state.soundEnabledMarketIds
            val isEnabling = !currentSet.contains(marketId)
            val newSet = if (isEnabling) {
                currentSet + marketId
            } else {
                currentSet - marketId
            }
            saveSoundEnabledMarkets(newSet)

            if (isEnabling && state.alarmsEnabled) {
                // Play a test chime by emitting a notification event
                val marketName = state.markets.find { it.id == marketId }?.name ?: marketId
                viewModelScope.launch {
                    _notificationEvents.emit(marketName)
                }
            }

            state.copy(soundEnabledMarketIds = newSet)
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                if (_uiState.value.isLiveMode) {
                    val now = Instant.now()
                    _uiState.update { state ->
                        // Calculate matching decimal scrubber hour in active ref zone
                        val refZoned = ZonedDateTime.ofInstant(now, state.currentReferenceZone)
                        val totalNanos = refZoned.toLocalTime().toNanoOfDay()
                        val currentFractionHour = totalNanos / 3_600_000_000_000L.toDouble()

                        // Check for market status transitions
                        state.markets.forEach { market ->
                            val currentStatus = MarketCalculator.getMarketStatusDetails(market, now).status
                            val previousStatus = lastKnownMarketStatus[market.id]
                            if (previousStatus != null && previousStatus != MarketStatus.OPEN && currentStatus == MarketStatus.OPEN) {
                                if (state.alarmsEnabled && state.soundEnabledMarketIds.contains(market.id)) {
                                    viewModelScope.launch {
                                        _notificationEvents.emit(market.name)
                                    }
                                }
                            }
                            lastKnownMarketStatus[market.id] = currentStatus
                        }

                        state.copy(
                            currentInstant = now,
                            scrubberHour = currentFractionHour
                        )
                    }
                }
                delay(200) // update every 200ms for smooth/accurate rendering
            }
        }
    }

    fun setLiveMode(isLive: Boolean) {
        _uiState.update { it.copy(isLiveMode = isLive) }
        if (isLive) {
            // Snaps current time and resumes
            val now = Instant.now()
            _uiState.update { state ->
                val refZoned = ZonedDateTime.ofInstant(now, state.currentReferenceZone)
                val totalNanos = refZoned.toLocalTime().toNanoOfDay()
                val currentFractionHour = totalNanos / 3_600_000_000_000L.toDouble()
                state.copy(currentInstant = now, scrubberHour = currentFractionHour)
            }
        }
    }

    fun updateScrubberHour(hour: Double) {
        if (!_uiState.value.isLiveMode) {
            _uiState.update { state ->
                // Form a simulated ZonedDateTime for the computed fraction hour on the current local day
                val refZone = state.currentReferenceZone
                val todayRefDate = LocalDate.now(refZone)
                val startOfToday = ZonedDateTime.of(todayRefDate, java.time.LocalTime.MIN, refZone)
                val nanosToAdd = (hour * 3_600_000_000_000L).toLong()
                val simulatedZonedDateTime = startOfToday.plusNanos(nanosToAdd)
                
                state.copy(
                    scrubberHour = hour,
                    currentInstant = simulatedZonedDateTime.toInstant()
                )
            }
        }
    }

    fun selectReferenceTimezone(index: Int) {
        if (index in _uiState.value.referenceTimezones.indices) {
            _uiState.update { state ->
                val newZone = state.referenceTimezones[index].zoneId
                // Re-calculate the matching scrubber hour based on the new reference zone's time
                val currentInstant = state.currentInstant
                val newZoned = ZonedDateTime.ofInstant(currentInstant, newZone)
                val totalNanos = newZoned.toLocalTime().toNanoOfDay()
                val newFractionHour = totalNanos / 3_600_000_000_000L.toDouble()

                state.copy(
                    selectedReferenceZoneIndex = index,
                    scrubberHour = newFractionHour
                )
            }
          }
    }

    fun selectMarket(marketId: String) {
        _uiState.update { it.copy(selectedMarketId = marketId) }
    }

    /**
     * Compute visual active arcs for each market relative to the current reference day.
     */
    fun getMarketActiveArcs(market: Market): List<Pair<Double, Double>> {
        val state = _uiState.value
        val refZone = state.currentReferenceZone
        val refDate = state.currentZonedDateTime.toLocalDate()
        return MarketCalculator.getMarketActiveDecimalHoursOnDay(market, refDate, refZone)
    }

    /**
     * Get real-time status of each market
     */
    fun getMarketStatus(market: Market): MarketStatusDetails {
        return MarketCalculator.getMarketStatusDetails(market, _uiState.value.currentInstant)
    }

    override fun onCleared() {
        tickerJob?.cancel()
        super.onCleared()
    }
}
