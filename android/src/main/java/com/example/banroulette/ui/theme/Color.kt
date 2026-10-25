package com.example.banroulette.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.core.geometry.AppColorPalette

// Common Brand colors matching Desktop & Core AppColorPalette
val BrandPrimary = Color(android.graphics.Color.parseColor(AppColorPalette.PRIMARY))
val BrandPrimaryDark = Color(android.graphics.Color.parseColor(AppColorPalette.PRIMARY_DARK))
val BrandPrimaryLight = Color(android.graphics.Color.parseColor(AppColorPalette.PRIMARY_LIGHT))
val BrandSecondary = Color(android.graphics.Color.parseColor(AppColorPalette.SECONDARY))
val BrandDanger = Color(android.graphics.Color.parseColor(AppColorPalette.DANGER))
val BrandDangerDark = Color(android.graphics.Color.parseColor(AppColorPalette.DANGER_DARK))
val BrandSuccess = Color(android.graphics.Color.parseColor(AppColorPalette.SUCCESS))

// CTA Spin Gradient Colors (Amber -> Red gradient matching desktop .btn-spin)
val SpinGradientStart = Color(android.graphics.Color.parseColor(AppColorPalette.SPIN_GRADIENT_START))
val SpinGradientEnd = Color(android.graphics.Color.parseColor(AppColorPalette.SPIN_GRADIENT_END))

// Badges
val BadgeSaved = Color(android.graphics.Color.parseColor(AppColorPalette.SAVED_BADGE))
val BadgeWheel = Color(android.graphics.Color.parseColor(AppColorPalette.WHEEL_BADGE))

// Dark theme palette (matching Desktop #0B1120 root, #111827 surface, #1F2937 variant)
val DarkBackground = Color(android.graphics.Color.parseColor(AppColorPalette.BACKGROUND))
val DarkSurface = Color(android.graphics.Color.parseColor(AppColorPalette.SURFACE))
val DarkSurfaceHeader = Color(android.graphics.Color.parseColor(AppColorPalette.SURFACE_HEADER))
val DarkSurfaceVariant = Color(android.graphics.Color.parseColor(AppColorPalette.SURFACE_VARIANT))
val DarkBorder = Color(android.graphics.Color.parseColor(AppColorPalette.BORDER))
val DarkBorderStrong = Color(android.graphics.Color.parseColor(AppColorPalette.BORDER_STRONG))
val DarkOnBackground = Color(android.graphics.Color.parseColor(AppColorPalette.TEXT_PRIMARY))
val DarkOnSurface = Color(android.graphics.Color.parseColor(AppColorPalette.TEXT_PRIMARY))
val DarkOnSurfaceVariant = Color(android.graphics.Color.parseColor(AppColorPalette.TEXT_MUTED))

// Backwards compatibility aliases
val BrandIndigo = BrandPrimary
val BrandIndigoDark = BrandPrimaryDark
val BrandPink = BrandSecondary
val BrandRed = BrandDanger
val BrandRedDark = BrandDangerDark
