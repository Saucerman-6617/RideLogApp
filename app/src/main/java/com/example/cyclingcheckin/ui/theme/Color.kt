package com.example.cyclingcheckin.ui.theme

import androidx.compose.ui.graphics.Color

/** 品牌与语义色，集中定义，禁止在页面内硬编码颜色。 */
val BrandBlue = Color(0xFF2F6BFF)
val BrandBlueLight = Color(0xFF5B8CFF)
val BrandBlueDeep = Color(0xFF1F55D6)
val AccentOrange = Color(0xFFFF7A45)
val SuccessGreen = Color(0xFF2BB673)
val DangerRed = Color(0xFFE53935)

val LightBackground = Color(0xFFF4F7FC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF0F3FA)
val LightOutline = Color(0xFFE4E8F0)
val LightTextPrimary = Color(0xFF16213A)
val LightTextSecondary = Color(0xFF7C8698)

val DarkBackground = Color(0xFF0E1420)
val DarkSurface = Color(0xFF161E2C)
val DarkSurfaceVariant = Color(0xFF1E293B)
val DarkOutline = Color(0xFF2B3648)
val DarkTextPrimary = Color(0xFFE8EDF7)
val DarkTextSecondary = Color(0xFF9AA6B8)

/** 页面使用的扩展语义色（Material 3 之外的骑行相关配色）。 */
data class RideLogColors(
    val accent: Color,
    val success: Color,
    val danger: Color,
    val chartTrack: Color,
    val chartHighlight: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val secondaryText: Color,
    val cardShadow: Color
)

val LightRideLogColors = RideLogColors(
    accent = AccentOrange,
    success = SuccessGreen,
    danger = DangerRed,
    chartTrack = Color(0xFFE3EBFE),
    chartHighlight = BrandBlueDeep,
    heroStart = BrandBlue,
    heroEnd = BrandBlueLight,
    secondaryText = LightTextSecondary,
    cardShadow = Color(0x142F6BFF)
)

val DarkRideLogColors = RideLogColors(
    accent = Color(0xFFFF9A6B),
    success = Color(0xFF4CD39A),
    danger = Color(0xFFFF6B66),
    chartTrack = Color(0xFF24314A),
    chartHighlight = Color(0xFF8FB2FF),
    heroStart = Color(0xFF2A55C0),
    heroEnd = Color(0xFF4A78E8),
    secondaryText = DarkTextSecondary,
    cardShadow = Color(0x33000000)
)
