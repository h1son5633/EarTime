package com.h1son5633.eartime.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.h1son5633.eartime.data.SessionEntity
import com.h1son5633.eartime.data.Stats
import com.h1son5633.eartime.data.formatClock
import com.h1son5633.eartime.data.formatDuration
import com.h1son5633.eartime.data.toTimeLabel
import java.time.LocalDate
import kotlinx.coroutines.delay

@Composable
fun TodayScreen(
    sessions: List<SessionEntity>,
    batteryExempt: Boolean,
    hasBtPermission: Boolean,
    onRequestBattery: () -> Unit,
    onRequestBtPermission: () -> Unit
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { now = System.currentTimeMillis(); delay(1_000) }
    }
    val stats = remember(sessions, now / 60_000) { Stats(sessions, now) }
    val today = LocalDate.now()
    val open = sessions.filter { it.endMs == null }.maxByOrNull { it.startMs }
    val todaySessions = sessions.filter {
        // sessions that overlap today
        val end = it.effectiveEnd(now)
        val dayStart = today.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val dayEnd = today.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        end > dayStart && it.startMs < dayEnd
    }.sortedByDescending { it.startMs }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 28.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("今天", color = TextPrimary, fontSize = 30.sp, fontWeight = FontWeight.Bold)

        // ---- current status ----
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                if (open != null) {
                    Text("正在佩戴", color = AccentBlue, fontSize = 14.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(open.deviceName, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        formatClock(now - open.startMs),
                        color = BigNumber, fontSize = 44.sp, fontWeight = FontWeight.Bold
                    )
                    Text("从 ${open.startMs.toTimeLabel()} 开始", color = TextSecondary, fontSize = 13.sp)
                } else {
                    Text("未连接", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text("连接蓝牙耳机后自动开始记录", color = TextSecondary, fontSize = 14.sp)
                }
            }
        }

        // ---- today totals ----
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            GlassCard(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("今日累计", color = TextSecondary, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(formatDuration(stats.dayTotal(today)), color = BigNumber, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }
            }
            GlassCard(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("今日次数", color = TextSecondary, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("${todaySessions.size} 次", color = BigNumber, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ---- setup cards ----
        if (!hasBtPermission) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("需要蓝牙权限", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text("没有蓝牙权限就读不到耳机连接状态，无法记录。", color = TextSecondary, fontSize = 14.sp)
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = onRequestBtPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) { Text("授予蓝牙权限", color = NavyBottom) }
                }
            }
        }
        if (!batteryExempt) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("把 EarTime 加入电池白名单", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "ColorOS 会杀后台。不加白名单，记录可能中断。点下面按钮，在系统弹窗里允许忽略电池优化。",
                        color = TextSecondary, fontSize = 14.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = onRequestBattery,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) { Text("去设置白名单", color = NavyBottom) }
                }
            }
        }

        // ---- today's sessions ----
        Text("今日记录", color = TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
        if (todaySessions.isEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("从今天开始记录", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text("戴上耳机听一会儿，这里就会出现第一条记录。", color = TextSecondary, fontSize = 14.sp)
                }
            }
        } else {
            for (s in todaySessions) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(s.deviceName, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Text(
                                "${s.startMs.toTimeLabel()} – ${s.endMs?.toTimeLabel() ?: "进行中"}",
                                color = TextSecondary, fontSize = 13.sp
                            )
                        }
                        Text(formatDuration(s.durationMs(now)), color = BigNumber, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
