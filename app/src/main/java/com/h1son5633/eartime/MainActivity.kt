package com.h1son5633.eartime

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.h1son5633.eartime.tracking.TrackingService
import com.h1son5633.eartime.ui.DevicesScreen
import com.h1son5633.eartime.ui.EarTimeTheme
import com.h1son5633.eartime.ui.MainViewModel
import com.h1son5633.eartime.ui.ScreenBackground
import com.h1son5633.eartime.ui.StatsScreen
import com.h1son5633.eartime.ui.TextPrimary
import com.h1son5633.eartime.ui.TextSecondary
import com.h1son5633.eartime.ui.TodayScreen
import com.h1son5633.eartime.ui.CardFill

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EarTimeTheme {
                EarTimeRoot()
            }
        }
    }
}

@Composable
private fun EarTimeRoot() {
    val context = LocalContext.current
    val vm: MainViewModel = viewModel()
    val sessions by vm.sessions.collectAsState()

    var tab by remember { mutableIntStateOf(0) }
    var hasBtPermission by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var batteryExempt by remember {
        mutableStateOf(isBatteryExempt(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasBtPermission = result[Manifest.permission.BLUETOOTH_CONNECT] ?: hasBtPermission
        }
        TrackingService.start(context)
    }

    LaunchedEffect(Unit) {
        val needed = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !hasBtPermission
        ) needed.add(Manifest.permission.BLUETOOTH_CONNECT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) needed.add(Manifest.permission.POST_NOTIFICATIONS)
        if (needed.isEmpty()) {
            TrackingService.start(context)
        } else {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    // Re-check battery state when returning to the screen.
    LaunchedEffect(tab) { batteryExempt = isBatteryExempt(context) }

    ScreenBackground {
        when (tab) {
            0 -> TodayScreen(
                sessions = sessions,
                batteryExempt = batteryExempt,
                hasBtPermission = hasBtPermission,
                onRequestBattery = {
                    val intent = Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:${context.packageName}")
                    )
                    runCatching { context.startActivity(intent) }
                },
                onRequestBtPermission = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        permissionLauncher.launch(arrayOf(Manifest.permission.BLUETOOTH_CONNECT))
                    }
                }
            )
            1 -> StatsScreen(sessions)
            else -> DevicesScreen(sessions)
        }

        // Pill bottom tab bar
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 26.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(CardFill)
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for ((i, label) in listOf("今天", "统计", "设备").withIndex()) {
                    val selected = i == tab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (selected) com.h1son5633.eartime.ui.AccentBlue.copy(alpha = 0.35f)
                                else androidx.compose.ui.graphics.Color.Transparent
                            )
                            .clickable { tab = i }
                            .padding(horizontal = 34.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            color = if (selected) TextPrimary else TextSecondary,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

private fun isBatteryExempt(context: android.content.Context): Boolean {
    val pm = context.getSystemService(PowerManager::class.java)
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}
