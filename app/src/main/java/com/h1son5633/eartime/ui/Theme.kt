package com.h1son5633.eartime.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val NavyTop = Color(0xFF16283F)
val NavyBottom = Color(0xFF0B1226)
val CardFill = Color(0x14FFFFFF)      // frosted glass white ~8%
val CardBorder = Color(0x1FFFFFFF)
val AccentBlue = Color(0xFF8FB8EC)
val BigNumber = Color(0xFFBDD7F5)
val TextPrimary = Color(0xFFF2F6FC)
val TextSecondary = Color(0xFF9FB2CC)
val BarColor = Color(0xFFA9C6EE)

private val scheme = darkColorScheme(
    background = NavyBottom,
    surface = Color(0xFF14213A),
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    primary = AccentBlue
)

@Composable
fun EarTimeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
fun ScreenBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NavyTop, NavyBottom))),
        content = content
    )
}

/** Frosted-glass rounded card, like the reference UI. */
@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(CardFill)
            .border(1.dp, CardBorder, shape),
        content = content
    )
}
