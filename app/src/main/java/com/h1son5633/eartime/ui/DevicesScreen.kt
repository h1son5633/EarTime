package com.h1son5633.eartime.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.h1son5633.eartime.data.SessionEntity
import com.h1son5633.eartime.data.Stats
import com.h1son5633.eartime.data.formatDuration
import com.h1son5633.eartime.data.toDate
import com.h1son5633.eartime.data.toLabel

@Composable
fun DevicesScreen(sessions: List<SessionEntity>) {
    val now = remember { System.currentTimeMillis() }
    val stats = remember(sessions) { Stats(sessions, now) }
    val devices = stats.deviceTotals()
    val total = stats.totalMs.coerceAtLeast(1L)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 28.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("设备", color = TextPrimary, fontSize = 30.sp, fontWeight = FontWeight.Bold)

        if (devices.isEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("还没有设备记录", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text("连接过的蓝牙耳机会出现在这里，并按累计佩戴时长排名。", color = TextSecondary, fontSize = 14.sp)
                }
            }
        } else {
            for (d in devices) {
                val share = d.totalMs.toFloat() / total.toFloat()
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(d.name, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            formatDuration(d.totalMs),
                            color = BigNumber, fontSize = 28.sp, fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(10.dp))
                        // share bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(BarColor.copy(alpha = 0.15f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(share.coerceIn(0f, 1f))
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(BarColor)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "占比 ${(share * 100).toInt()}% · ${d.sessionCount} 次 · 最近使用 ${d.lastUsedMs.toDate().toLabel()}",
                            color = TextSecondary, fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
