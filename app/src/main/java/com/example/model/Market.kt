package com.example.model

import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.Instant
import java.time.Duration

data class Market(
    val id: String,
    val name: String,
    val exchange: String,
    val code: String,
    val flag: String,
    val city: String,
    val timezoneId: String,
    val openTimeLocal: LocalTime,
    val closeTimeLocal: LocalTime,
    val color: Color,
    val hasLunchBreak: Boolean = false,
    val lunchStartLocal: LocalTime? = null,
    val lunchEndLocal: LocalTime? = null
) {
    // A single trading interval local to the market
    data class LocalTimeInterval(val start: LocalTime, val end: LocalTime)

    // List of active local sessions (handles lunch breaks as split sessions)
    fun getLocalSessions(): List<LocalTimeInterval> {
        return if (hasLunchBreak && lunchStartLocal != null && lunchEndLocal != null) {
            listOf(
                LocalTimeInterval(openTimeLocal, lunchStartLocal),
                LocalTimeInterval(lunchEndLocal, closeTimeLocal)
            )
        } else {
            listOf(LocalTimeInterval(openTimeLocal, closeTimeLocal))
        }
    }

    /**
     * Gets the active absolute intervals (in ZonedDateTime) of this market for a set of target dates
     * relative to its local timezone. Typically, we query current, previous, and next dates to
     * find any overlapping periods on the reference day.
     */
    fun getTradingSessionsForDate(localTargetDate: LocalDate): List<Pair<ZonedDateTime, ZonedDateTime>> {
        val zoneId = ZoneId.of(timezoneId)
        // Weekend check: Saturday and Sunday are closed
        val dayOfWeek = localTargetDate.dayOfWeek
        if (dayOfWeek == java.time.DayOfWeek.SATURDAY || dayOfWeek == java.time.DayOfWeek.SUNDAY) {
            return emptyList()
        }

        return getLocalSessions().map { session ->
            val sessionStart = ZonedDateTime.of(localTargetDate, session.start, zoneId)
            val sessionEnd = ZonedDateTime.of(localTargetDate, session.end, zoneId)
            Pair(sessionStart, sessionEnd)
        }
    }
}

enum class MarketStatus {
    OPEN,
    LUNCH,
    CLOSED
}

data class MarketStatusDetails(
    val status: MarketStatus,
    val statusText: String,
    val countdownText: String,
    val currentLocalTimeText: String,
    val timezoneOffset: String
)

/**
 * Calculations helper for World Markets
 */
object MarketCalculator {

    val SupportedMarkets = listOf(
        Market(
            id = "nyse",
            name = "United States",
            exchange = "New York Stock Exchange",
            code = "NYSE",
            flag = "🇺🇸",
            city = "New York",
            timezoneId = "America/New_York",
            openTimeLocal = LocalTime.of(9, 30),
            closeTimeLocal = LocalTime.of(16, 0),
            color = Color(0xFFD0BCFF) // Beautiful Lavender Accent
        ),
        Market(
            id = "lse",
            name = "United Kingdom",
            exchange = "London Stock Exchange",
            code = "LSE",
            flag = "🇬🇧",
            city = "London",
            timezoneId = "Europe/London",
            openTimeLocal = LocalTime.of(8, 0),
            closeTimeLocal = LocalTime.of(16, 30),
            color = Color(0xFF7D5260) // Plum Orchid Accent
        ),
        Market(
            id = "dax",
            name = "Germany",
            exchange = "Frankfurt Stock Exchange",
            code = "XETRA",
            flag = "🇩🇪",
            city = "Frankfurt",
            timezoneId = "Europe/Berlin",
            openTimeLocal = LocalTime.of(9, 0),
            closeTimeLocal = LocalTime.of(17, 30),
            color = Color(0xFFE8DEF8) // Soft Heather Lavender
        ),
        Market(
            id = "sse",
            name = "China",
            exchange = "Shanghai Stock Exchange",
            code = "SSE",
            flag = "🇨🇳",
            city = "Shanghai",
            timezoneId = "Asia/Shanghai",
            openTimeLocal = LocalTime.of(9, 30),
            closeTimeLocal = LocalTime.of(15, 0),
            color = Color(0xFFF2B8B5), // Soft Peach Rose
            hasLunchBreak = true,
            lunchStartLocal = LocalTime.of(11, 30),
            lunchEndLocal = LocalTime.of(13, 0)
        ),
        Market(
            id = "tse",
            name = "Japan",
            exchange = "Tokyo Stock Exchange",
            code = "TSE",
            flag = "🇯🇵",
            city = "Tokyo",
            timezoneId = "Asia/Tokyo",
            openTimeLocal = LocalTime.of(9, 0),
            closeTimeLocal = LocalTime.of(15, 0),
            color = Color(0xFFEFB8C8), // Pastel Rose Cherry
            hasLunchBreak = true,
            lunchStartLocal = LocalTime.of(11, 30),
            lunchEndLocal = LocalTime.of(12, 30)
        ),
        Market(
            id = "hkex",
            name = "Hong Kong",
            exchange = "Hong Kong Stock Exchange",
            code = "HKEX",
            flag = "🇭🇰",
            city = "Hong Kong",
            timezoneId = "Asia/Hong_Kong",
            openTimeLocal = LocalTime.of(9, 30),
            closeTimeLocal = LocalTime.of(16, 0),
            color = Color(0xFFFFB4AB), // Warm Coral Red
            hasLunchBreak = true,
            lunchStartLocal = LocalTime.of(12, 0),
            lunchEndLocal = LocalTime.of(13, 0)
        ),
        Market(
            id = "asx",
            name = "Australia",
            exchange = "Australian Securities Exchange",
            code = "ASX",
            flag = "🇦🇺",
            city = "Sydney",
            timezoneId = "Australia/Sydney",
            openTimeLocal = LocalTime.of(10, 0),
            closeTimeLocal = LocalTime.of(16, 0),
            color = Color(0xFFB6EEA9) // Pale Mint Green
        ),
        Market(
            id = "nse",
            name = "India",
            exchange = "National Stock Exchange",
            code = "NSE",
            flag = "🇮🇳",
            city = "Mumbai",
            timezoneId = "Asia/Kolkata",
            openTimeLocal = LocalTime.of(9, 15),
            closeTimeLocal = LocalTime.of(15, 30),
            color = Color(0xFFFFCC80) // Soft Warm Apricot Orange
        )
    )

    /**
     * Compute active intervals (in reference decimal hours from 0.0 to 24.0)
     * of a market relative to a specific date in the reference timezone.
     */
    fun getMarketActiveDecimalHoursOnDay(
        market: Market,
        refDate: LocalDate,
        refZoneId: ZoneId
    ): List<Pair<Double, Double>> {
        val refDayStart = ZonedDateTime.of(refDate, LocalTime.MIN, refZoneId)
        val refDayEnd = ZonedDateTime.of(refDate.plusDays(1), LocalTime.MIN, refZoneId)

        // To guarantee we fetch all overlaps, query sessions for market local days:
        // refDate - 2, refDate - 1, refDate, refDate + 1, refDate + 2 (covers all extremes of offset)
        val marketZone = ZoneId.of(market.timezoneId)
        val marketDateAround = LocalDate.now(marketZone) // standard default anchor reference
        
        // We can just generate dates dynamically around the refDate
        val datesToQuery = listOf(
            refDate.minusDays(2),
            refDate.minusDays(1),
            refDate,
            refDate.plusDays(1),
            refDate.plusDays(2)
        )

        val intersectingHours = mutableListOf<Pair<Double, Double>>()

        for (queryDate in datesToQuery) {
            val sessions = market.getTradingSessionsForDate(queryDate)
            for (session in sessions) {
                val sessionStartInRef = session.first.withZoneSameInstant(refZoneId)
                val sessionEndInRef = session.second.withZoneSameInstant(refZoneId)

                // Compute intersection of [sessionStartInRef, sessionEndInRef] with [refDayStart, refDayEnd]
                val overlapStart = if (sessionStartInRef.isBefore(refDayStart)) refDayStart else sessionStartInRef
                val overlapEnd = if (sessionEndInRef.isAfter(refDayEnd)) refDayEnd else sessionEndInRef

                if (overlapStart.isBefore(overlapEnd)) {
                    // Turn times into decimal hours from midnight
                    val startDuration = java.time.Duration.between(refDayStart, overlapStart)
                    val endDuration = java.time.Duration.between(refDayStart, overlapEnd)

                    val startHour = startDuration.toMillis() / 3600000.0
                    val endHour = endDuration.toMillis() / 3600000.0

                    // Keep values strictly within [0.0, 24.0]
                    val boundedStart = startHour.coerceIn(0.0, 24.0)
                    val boundedEnd = endHour.coerceIn(0.0, 24.0)
                    if (boundedEnd > boundedStart + 0.001) {
                        intersectingHours.add(Pair(boundedStart, boundedEnd))
                    }
                }
            }
        }
        return intersectingHours
    }

    /**
     * Compute market status details relative to an absolute Instant
     */
    fun getMarketStatusDetails(market: Market, nowInstant: Instant): MarketStatusDetails {
        val marketZone = ZoneId.of(market.timezoneId)
        val marketZonedDateTime = ZonedDateTime.ofInstant(nowInstant, marketZone)
        val marketLocalDate = marketZonedDateTime.toLocalDate()
        val marketLocalTime = marketZonedDateTime.toLocalTime()

        // Formatting local offsets & time
        val offset = marketZone.rules.getOffset(nowInstant)
        val offsetText = if (offset.id == "Z") "UTC+0" else "UTC${offset.id}"
        val formatter = java.time.format.DateTimeFormatter.ofPattern("hh:mm:ss a")
        val currentLocalTimeText = marketZonedDateTime.format(formatter)

        // Find the status and countdown
        // We will scan sessions for Yesterday, Today, and Tomorrow (local to market)
        val dates = listOf(marketLocalDate.minusDays(1), marketLocalDate, marketLocalDate.plusDays(1))
        
        // Let's hold all upcoming and running sessions in absolute chronological order
        data class SessionZonedDateTime(
            val type: String, // "MAIN" or "LUNCH"
            val start: ZonedDateTime,
            val end: ZonedDateTime,
            val sessionIndex: Int // if split, first (1) or second (2)
        )

        val sessions = mutableListOf<SessionZonedDateTime>()
        for (d in dates) {
            // Is it a weekend?
            val dow = d.dayOfWeek
            if (dow == java.time.DayOfWeek.SATURDAY || dow == java.time.DayOfWeek.SUNDAY) {
                continue
            }

            if (market.hasLunchBreak && market.lunchStartLocal != null && market.lunchEndLocal != null) {
                sessions.add(
                    SessionZonedDateTime(
                        "MORNING",
                        ZonedDateTime.of(d, market.openTimeLocal, marketZone),
                        ZonedDateTime.of(d, market.lunchStartLocal, marketZone),
                        1
                    )
                )
                sessions.add(
                    SessionZonedDateTime(
                        "LUNCH",
                        ZonedDateTime.of(d, market.lunchStartLocal, marketZone),
                        ZonedDateTime.of(d, market.lunchEndLocal, marketZone),
                        0
                    )
                )
                sessions.add(
                    SessionZonedDateTime(
                        "AFTERNOON",
                        ZonedDateTime.of(d, market.lunchEndLocal, marketZone),
                        ZonedDateTime.of(d, market.closeTimeLocal, marketZone),
                        2
                    )
                )
            } else {
                sessions.add(
                    SessionZonedDateTime(
                        "MAIN",
                        ZonedDateTime.of(d, market.openTimeLocal, marketZone),
                        ZonedDateTime.of(d, market.closeTimeLocal, marketZone),
                        1
                    )
                )
            }
        }

        // Sort chronological
        val sortedSessions = sessions.sortedBy { it.start }

        // Find current active session, if any
        val nowZoned = ZonedDateTime.ofInstant(nowInstant, marketZone)
        val activeSession = sortedSessions.find { !nowZoned.isBefore(it.start) && !nowZoned.isAfter(it.end) }

        if (activeSession != null) {
            if (activeSession.type == "LUNCH") {
                val nextActive = sortedSessions.find { it.start.isAfter(nowZoned) && it.type != "LUNCH" }
                val durationLeft = Duration.between(nowZoned, activeSession.end)
                val countdownText = if (nextActive != null) {
                    "Resumes in ${formatDuration(durationLeft)}"
                } else {
                    "Lunch ends in ${formatDuration(durationLeft)}"
                }
                return MarketStatusDetails(
                    status = MarketStatus.LUNCH,
                    statusText = "LUNCH BREAK",
                    countdownText = countdownText,
                    currentLocalTimeText = currentLocalTimeText,
                    timezoneOffset = offsetText
                )
            } else {
                // Trading active! Either MORNING, AFTERNOON, or MAIN session
                // We show time remaining in current session OR till the official end.
                // If it is MORNING, it goes to lunch, so it "Closes for lunch in ...", otherwise "Closes in ..."
                val durationLeft = Duration.between(nowZoned, activeSession.end)
                val countdownText = when (activeSession.type) {
                    "MORNING" -> "Lunch in ${formatDuration(durationLeft)}"
                    "AFTERNOON", "MAIN" -> "Closes in ${formatDuration(durationLeft)}"
                    else -> "Closes in ${formatDuration(durationLeft)}"
                }
                return MarketStatusDetails(
                    status = MarketStatus.OPEN,
                    statusText = "OPEN",
                    countdownText = countdownText,
                    currentLocalTimeText = currentLocalTimeText,
                    timezoneOffset = offsetText
                )
            }
        }

        // None active. The market is CLOSED.
        // We find the NEXT upcoming session (excluding actual LUNCH break sessions)
        val nextSession = sortedSessions.find { it.start.isAfter(nowZoned) && it.type != "LUNCH" }
        val countdownText = if (nextSession != null) {
            val durationToOpen = Duration.between(nowZoned, nextSession.start)
            "Opens in ${formatDuration(durationToOpen)}"
        } else {
            // Highly unlikely but fallback: find next working day manually
            "Opens Monday"
        }

        return MarketStatusDetails(
            status = MarketStatus.CLOSED,
            statusText = "CLOSED",
            countdownText = countdownText,
            currentLocalTimeText = currentLocalTimeText,
            timezoneOffset = offsetText
        )
    }

    private fun formatDuration(duration: Duration): String {
        val days = duration.toDays()
        val hours = duration.toHours() % 24
        val minutes = duration.toMinutes() % 60
        return when {
            days > 0 -> "${days}d ${hours}h ${minutes}m"
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }
    }
}
