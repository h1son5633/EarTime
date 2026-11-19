package com.h1son5633.eartime.data

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** "1760h 47min" style, like the reference app. Under an hour: "47min". Zero: "0min". */
fun formatDuration(ms: Long): String {
    val totalMin = ms / 60_000
    val h = totalMin / 60
    val m = totalMin % 60
    return if (h > 0) "${h}h ${m}min" else "${m}min"
}

/** Live-timer style: 1:23:45 */
fun formatClock(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return String.format(Locale.US, "%d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
}

private val zone: ZoneId get() = ZoneId.systemDefault()

fun LocalDate.toLabel(): String = format(DateTimeFormatter.ofPattern("yyyy.M.d"))
fun Long.toDate(): LocalDate = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()
fun Long.toTimeLabel(): String =
    Instant.ofEpochMilli(this).atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm"))

data class DaySegment(val date: LocalDate, val durationMs: Long)

/** Split one session at local midnights so cross-midnight wear counts on both days. */
fun splitByDay(session: SessionEntity, now: Long): List<DaySegment> {
    val end = session.effectiveEnd(now)
    if (end <= session.startMs) return emptyList()
    val out = ArrayList<DaySegment>()
    var cursor = session.startMs
    while (cursor < end) {
        val date = cursor.toDate()
        val nextMidnight = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val segEnd = minOf(end, nextMidnight)
        out.add(DaySegment(date, segEnd - cursor))
        cursor = segEnd
    }
    return out
}

class Stats(private val sessions: List<SessionEntity>, private val now: Long) {

    val totalMs: Long = sessions.sumOf { it.durationMs(now) }
    val firstDay: LocalDate? = sessions.minOfOrNull { it.startMs }?.toDate()

    private val byDay: Map<LocalDate, Long> by lazy {
        val map = HashMap<LocalDate, Long>()
        for (s in sessions) for (seg in splitByDay(s, now)) {
            map[seg.date] = (map[seg.date] ?: 0L) + seg.durationMs
        }
        map
    }

    fun dayTotal(date: LocalDate): Long = byDay[date] ?: 0L

    fun monthTotal(ym: YearMonth): Long =
        byDay.entries.filter { YearMonth.from(it.key) == ym }.sumOf { it.value }

    fun yearTotal(year: Int): Long =
        byDay.entries.filter { it.key.year == year }.sumOf { it.value }

    /** (duration, date) of the single longest day. */
    fun longestDay(): Pair<Long, LocalDate?> {
        val e = byDay.entries.maxByOrNull { it.value } ?: return 0L to null
        return e.value to e.key
    }

    /** The single longest session (raw, not split). */
    fun longestSession(): SessionEntity? =
        sessions.maxByOrNull { it.durationMs(now) }

    /** (days, startDate, endDate) of the longest run of consecutive days with any wear. */
    fun longestStreak(): Triple<Int, LocalDate?, LocalDate?> {
        val days = byDay.filterValues { it > 0 }.keys.sorted()
        if (days.isEmpty()) return Triple(0, null, null)
        var bestLen = 1; var bestStart = days[0]; var bestEnd = days[0]
        var curLen = 1; var curStart = days[0]
        for (i in 1 until days.size) {
            if (days[i] == days[i - 1].plusDays(1)) {
                curLen++
            } else {
                curLen = 1; curStart = days[i]
            }
            if (curLen > bestLen) { bestLen = curLen; bestStart = curStart; bestEnd = days[i] }
        }
        return Triple(bestLen, bestStart, bestEnd)
    }

    /** (duration, month) of the single longest month. */
    fun longestMonth(): Pair<Long, YearMonth?> {
        val byMonth = HashMap<YearMonth, Long>()
        for ((d, v) in byDay) {
            val ym = YearMonth.from(d)
            byMonth[ym] = (byMonth[ym] ?: 0L) + v
        }
        val e = byMonth.entries.maxByOrNull { it.value } ?: return 0L to null
        return e.value to e.key
    }

    data class DeviceTotal(
        val name: String,
        val address: String,
        val totalMs: Long,
        val sessionCount: Int,
        val lastUsedMs: Long
    )

    fun deviceTotals(): List<DeviceTotal> =
        sessions.groupBy { it.deviceAddress }.map { (addr, list) ->
            DeviceTotal(
                name = list.maxByOrNull { it.startMs }?.deviceName ?: addr,
                address = addr,
                totalMs = list.sumOf { it.durationMs(now) },
                sessionCount = list.size,
                lastUsedMs = list.maxOf { it.effectiveEnd(now) }
            )
        }.sortedByDescending { it.totalMs }

    fun mostWornDevice(): DeviceTotal? = deviceTotals().firstOrNull()
}

enum class Period { WEEK, MONTH, YEAR, ALL }

data class PeriodData(
    val label: String,
    val totalMs: Long,
    val bars: List<Pair<String, Long>>,
    val previousTotalMs: Long
)

/** Bars + total for the selected period, shifted by [offset] whole periods from current. */
fun periodData(stats: Stats, period: Period, offset: Int, today: LocalDate): PeriodData {
    return when (period) {
        Period.WEEK -> {
            val monday = today.minusDays((today.dayOfWeek.value - 1).toLong()).plusWeeks(offset.toLong())
            val days = (0..6).map { monday.plusDays(it.toLong()) }
            val bars = days.map { d -> d.dayOfWeek.value.let {
                listOf("一", "二", "三", "四", "五", "六", "日")[it - 1]
            } to stats.dayTotal(d) }
            val prevMonday = monday.minusWeeks(1)
            val prev = (0..6).sumOf { stats.dayTotal(prevMonday.plusDays(it.toLong())) }
            PeriodData(
                label = "${monday.toLabel()} – ${monday.plusDays(6).toLabel()}",
                totalMs = bars.sumOf { it.second }, bars = bars, previousTotalMs = prev
            )
        }
        Period.MONTH -> {
            val ym = YearMonth.from(today).plusMonths(offset.toLong())
            val bars = (1..ym.lengthOfMonth()).map { d -> d.toString() to stats.dayTotal(ym.atDay(d)) }
            PeriodData(
                label = "${ym.year}年${ym.monthValue}月",
                totalMs = bars.sumOf { it.second }, bars = bars,
                previousTotalMs = stats.monthTotal(ym.minusMonths(1))
            )
        }
        Period.YEAR -> {
            val year = today.year + offset
            val bars = (1..12).map { m -> "${m}月" to stats.monthTotal(YearMonth.of(year, m)) }
            PeriodData(
                label = "${year}年",
                totalMs = bars.sumOf { it.second }, bars = bars,
                previousTotalMs = stats.yearTotal(year - 1)
            )
        }
        Period.ALL -> {
            val first = stats.firstDay
            if (first == null) {
                PeriodData("总共", 0L, (1..12).map { "${it}月" to 0L }, 0L)
            } else {
                val months = ArrayList<YearMonth>()
                var cur = YearMonth.from(first)
                val last = YearMonth.from(today)
                while (!cur.isAfter(last)) { months.add(cur); cur = cur.plusMonths(1) }
                if (months.size <= 12) {
                    val bars = months.map { ym -> "${ym.monthValue}月" to stats.monthTotal(ym) }
                    PeriodData("总共", bars.sumOf { it.second }, bars, 0L)
                } else {
                    val years = (first.year..today.year).toList()
                    val bars = years.map { y -> "${y}" to stats.yearTotal(y) }
                    PeriodData("总共", bars.sumOf { it.second }, bars, 0L)
                }
            }
        }
    }
}
