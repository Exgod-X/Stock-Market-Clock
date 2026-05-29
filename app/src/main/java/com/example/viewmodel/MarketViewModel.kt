package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.Market
import com.example.model.MarketCalculator
import com.example.model.MarketStatusDetails
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    val markets: List<Market> = MarketCalculator.SupportedMarkets
) {
    val currentReferenceZone: ZoneId
        get() = referenceTimezones.getOrNull(selectedReferenceZoneIndex)?.zoneId ?: ZoneId.systemDefault()

    val currentZonedDateTime: ZonedDateTime
        get() = ZonedDateTime.ofInstant(currentInstant, currentReferenceZone)
}

class MarketViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MarketUiState())
    val uiState: StateFlow<MarketUiState> = _uiState.asStateFlow()

    private var tickerJob: Job? = null

    init {
        // Initialize reference timezone list
        val localZone = ZoneId.systemDefault()
        val zones = mutableListOf<ReferenceTimezone>()
        
        // Always add Local Time and UTC
        zones.add(ReferenceTimezone("Local Time", localZone, "My device timezone (" + localZone.id + ")"))
        zones.add(ReferenceTimezone("UTC", ZoneId.of("UTC"), "Coordinated Universal Time"))
        
        // Add individual major market zone presets
        MarketCalculator.SupportedMarkets.forEach { m ->
            val zone = ZoneId.of(m.timezoneId)
            // Prevent duplicates (e.g. if localZone is same as marketZone)
            if (zones.none { it.zoneId.id == zone.id }) {
                zones.add(ReferenceTimezone(m.city, zone, "${m.exchange} timezone"))
            }
        }

        _uiState.update { it.copy(referenceTimezones = zones, selectedMarketId = "nyse") }

        startTicker()
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
