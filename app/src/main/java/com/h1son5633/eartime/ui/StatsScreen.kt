package com.h1son5633.eartime.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.h1son5633.eartime.data.Period
import com.h1son5633.eartime.data.SessionEntity
import com.h1son5633.eartime.data.Stats
import com.h1son5633.eartime.data.formatDuration
import com.h1son5633.eartime.data.periodData
import com.h1son5633.eartime.data.toLabel
import com.h1son5633.eartime.data.toTimeLabel
import java.time.LocalDate

@Composable
fun StatsScreen(sessions: List<SessionEntity>) {
    val now = remember { System.currentTimeMillis() }
    val stats = remember(sessions) { Stats(sessions, now) }
    var period by remember { mutableStateOf(Period.YEAR) }
    var offset by remember { mutableIntStateOf(0) }
    val data = remember(stats, period, offset) { periodData(stats, period, offset, LocalDate.now()) }

    val (longestDayMs, longestDayDate) = stats.longestDay()
    val longestSession = stats.longestSession()
    val (streakDays, streakStart, streakEnd) = stats.longestStreak()
    val (longestMonthMs, longestMonth) = stats.longestMonth()
    val mostWorn = stats.mostWornDevice()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 28.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("统计", color = TextPrimary, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text("个人纪录", color = TextSecondary, fontSize = 15.sp)

        // ---- records 2x2 ----
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            RecordCard(Modifier.weight(1f), "单日佩戴最长", formatDuration(longestDayMs),
                longestDayDate?.toLabel() ?: "暂无记录")
            RecordCard(Modifier.weight(1f), "单次佩戴最长",
                longestSession?.let { formatDuration(it.durationMs(now)) } ?: "0min",
                longestSession?.let {
                    "${it.startMs.toLabel()} ${it.startMs.toTimeLabel()} – ${it.effectiveEnd(now).toTimeLabel()}"
                } ?: "暂无记录")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            RecordCard(Modifier.weight(1f), "连续记录最长", "${streakDays}天",
                if (streakStart != null && streakEnd != null)
                    "${streakStart.toLabel()} – ${streakEnd.toLabel()}" else "暂无记录")
            RecordCard(Modifier.weight(1f), "单月佩戴最长", formatDuration(longestMonthMs),
                longestMonth?.let { "${it.year}年${it.monthValue}月" } ?: "暂无记录")
        }

        // ---- most worn ----
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("🎧 最常戴耳机", color = TextSecondary, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                if (mostWorn != null) {
                    Text(mostWorn.name, color = TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(formatDuration(mostWorn.totalMs), color = BigNumber, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Text("累计佩戴", color = TextSecondary, fontSize = 13.sp)
                } else {
                    Text("还没有记录", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text("从今天开始记录，第一副耳机就是它。", color = TextSecondary, fontSize = 13.sp)
                }
            }
        }

        // ---- period chart ----
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("累计佩戴时长", color = TextSecondary, fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))
                Text(formatDuration(data.totalMs), color = BigNumber, fontSize = 38.sp, fontWeight = FontWeight.Bold)
                if (period != Period.ALL && data.previousTotalMs > 0) {
                    val diff = data.totalMs - data.previousTotalMs
                    val sign = if (diff >= 0) "+" else "−"
                    Text(
                        "较上期 $sign${formatDuration(kotlin.math.abs(diff))}",
                        color = TextSecondary, fontSize = 13.sp
                    )
                }
                Spacer(Modifier.height(14.dp))

                // segmented tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(CardFill)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for ((p, label) in listOf(
                        Period.WEEK to "周", Period.MONTH to "月",
                        Period.YEAR to "年", Period.ALL to "总共"
                    )) {
                        val selected = p == period
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(50))
                                .background(if (selected) AccentBlue.copy(alpha = 0.35f) else androidx.compose.ui.graphics.Color.Transparent)
                                .clickable { period = p; offset = 0 }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, color = if (selected) TextPrimary else TextSecondary, fontSize = 15.sp)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))

                // period navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavArrow("‹", period != Period.ALL) { offset-- }
                    Text(data.label, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    NavArrow("›", period != Period.ALL && offset < 0) { offset++ }
                }
                Spacer(Modifier.height(14.dp))

                BarChart(bars = data.bars)
            }
        }
    }
}

@Composable
private fun RecordCard(modifier: Modifier, title: String, value: String, sub: String) {
    GlassCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(6.dp))
            Text(value, color = BigNumber, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(sub, color = TextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun NavArrow(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 14.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (enabled) TextPrimary else TextSecondary.copy(alpha = 0.3f),
            fontSize = 26.sp
        )
    }
}

@Composable
private fun BarChart(bars: List<Pair<String, Long>>) {
    val maxV = (bars.maxOfOrNull { it.second } ?: 0L).coerceAtLeast(1L)
    val chartHeight = 130.dp
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            for ((_, v) in bars) {
                val frac = (v.toFloat() / maxV.toFloat()).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height((chartHeight.value * maxOf(frac, 0.02f)).dp)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(
                            if (v > 0) BarColor else BarColor.copy(alpha = 0.18f)
                        )
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            val showEvery = when {
                bars.size <= 8 -> 1
                bars.size <= 16 -> 2
                else -> 5
            }
            bars.forEachIndexed { i, (label, _) ->
                Text(
                    if (i % showEvery == 0) label else "",
                    color = TextSecondary, fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        val maxLabel = bars.maxByOrNull { it.second }
        if (maxLabel != null && maxLabel.second > 0) {
            Spacer(Modifier.height(4.dp))
            Text("最高：${maxLabel.first} · ${formatDuration(maxLabel.second)}", color = TextSecondary, fontSize = 12.sp)
        }
    }
}
