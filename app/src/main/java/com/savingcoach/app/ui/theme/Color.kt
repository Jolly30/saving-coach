package com.savingcoach.app.ui.theme

import androidx.compose.ui.graphics.Color

// ==========================================
// Balanced "Matcha & Cream" Design Tokens (60-30-10 Rule)
// 60% Warm Linen / Oat Canvas
// 30% Warm Neutral Structure & Charcoal Typography
// 10% Intentional Ceremonial Kyoto Matcha Accents
// ==========================================

// 10% Primary Matcha Accents (Buttons, Active States, Key Icons)
val MatchaPrimary = Color(0xFF446E4C)         // Fresh Ceremonial Kyoto Matcha (5.1:1+ AA/AAA contrast)
val MatchaPrimaryDark = Color(0xFF35563D)     // Deep Forest Matcha Accent
val MatchaPrimaryLight = Color(0xFF659C6D)    // Vibrant Sprout Leaf Accent
val MatchaContainer = Color(0xFFF1EDE4)       // Warm Oat Neutral Container / Pill (Prevents green wash)
val MatchaTextPill = Color(0xFF1F1D1A)        // Warm Charcoal for high-contrast legible pill text

// 60% Cream & Oat Canvas Tones (Light)
val CreamBackground = Color(0xFFF9F7F2)       // Warm Linen Cream Canvas
val CreamSurface = Color(0xFFFFFFFF)          // Pure Crisp White Cards
val CreamSurfaceVariant = Color(0xFFF1EDE4)   // Warm Oat Card / Secondary Container
val CreamOutline = Color(0xFFE5E0D5)          // Warm Stone / Oat Border
val CreamOutlineVariant = Color(0xFFECE7DE)   // Soft Linen Divider

// 30% Neutral Charcoal & Stone Typography (Light)
val DarkRoast = Color(0xFF1F1D1A)             // Warm Neutral Charcoal (Primary Text)
val EarthySlate = Color(0xFF635E56)           // Warm Stone (Secondary / Muted Text)
val MutedHerb = Color(0xFF9E988D)             // Warm Neutral Placeholder / Disabled

// Warm Ceramic Clay & Toasted Oat Accents
val WarmClay = Color(0xFFB87D56)              // Warm Ceramic Mug Clay
val WarmOat = Color(0xFFEBE3D0)               // Toasted Warm Oat
val WarmCaramel = WarmClay
val WarmCaramelVariant = Color(0xFF9E653F)
val WarmCaramelContainer = Color(0xFFF7ECE0)

// ==========================================
// Dark Mode Tokens ("Espresso Velvet & Luminous Matcha")
// 60% Deep Roasted Charcoal & Warm Slate Surfaces
// 30% Froth Cream & Warm Stone Neutral Typography & Outlines
// 10% Luminous Bamboo Sprout Green Accents
// ==========================================
val DarkMatchaBackground = Color(0xFF141412)       // 60% Deep Roasted Charcoal (Warm, no green cast)
val DarkMatchaSurface = Color(0xFF1E1E1B)          // 60% Warm Charcoal Card / Sheet
val DarkMatchaSurfaceVariant = Color(0xFF292824)   // 60% Elevated Oat Surface / Input / Chip Track
val DarkMatchaPrimary = Color(0xFF7CB88B)          // 10% Luminous Bamboo Sprout Accent
val DarkMatchaPrimaryContainer = Color(0xFF223526) // 10% Dark Matcha Cushion
val DarkMatchaSecondary = Color(0xFFD49B74)        // Warm Terracotta Clay
val DarkMatchaOnBackground = Color(0xFFF2EFE8)     // 30% Froth Cream Text (Crisp high-contrast)
val DarkMatchaOnSurface = Color(0xFFF2EFE8)        // 30% Froth Cream Text
val DarkMatchaOnSurfaceVariant = Color(0xFFA8A398) // 30% Warm Stone Subtext (Neutral, no green glow)
val DarkMatchaOutline = Color(0xFF33312C)          // 30% Warm Charcoal Outline / Border
val DarkMatchaOutlineVariant = Color(0xFF3D3A34)   // 30% Subtle Hairline Divider

// ==========================================
// Financial & Status Semantic Colors
// ==========================================
// Green (Income / Positive / High tier)
val MatchaGreen = Color(0xFF446E4C)
val MatchaGreenDark = Color(0xFF35563D)
val Green = MatchaGreen
val GreenDark = MatchaGreenDark

// Yellow / Warning (Golden Honey Custard)
val HoneyYellow = Color(0xFFD4A237)
val HoneyYellowDark = Color(0xFFB38322)
val Yellow = HoneyYellow

// Orange / Caution (Warm Ceramic Clay Amber)
val WarmOrange = Color(0xFFB87D56)
val Orange = WarmOrange

// Red / Expense / Error (Terracotta Clay Ember)
val CoralRed = Color(0xFFC94D3F)
val CoralRedDark = Color(0xFFA6392D)
val Red = CoralRed
val RedDark = CoralRedDark

// Chart & Heatmap 5-Tier Intensity Ramp
val MatchaRampTier0 = Color(0xFFEBE6DA)       // Neutral Oat Base (Zero activity)
val MatchaRampTier1 = Color(0xFFB8D8B6)       // Light Matcha Sprout
val MatchaRampTier2 = Color(0xFF79B782)       // Mid Matcha Leaf
val MatchaRampTier3 = Color(0xFF446E4C)       // Ceremonial Matcha
val MatchaRampTier4 = Color(0xFF223827)       // Deep Kyoto Tea

// ==========================================
// Semantic Aliases & Backward-Compatibility Mappings
// ==========================================
val Primary = MatchaPrimary
val PrimaryVariant = MatchaPrimaryDark
val PrimaryLight = MatchaPrimaryLight
val Secondary = WarmCaramel
val SecondaryVariant = WarmCaramelVariant

val Background = CreamBackground
val Surface = CreamSurface
val SurfaceDark = DarkMatchaSurface

val OnPrimary = Color.White
val OnSecondary = Color.White
val OnBackground = DarkRoast
val OnSurface = DarkRoast
val OnSurfaceVariant = EarthySlate

val DarkBackground = DarkMatchaBackground
val DarkSurface = DarkMatchaSurface
val DarkOnBackground = DarkMatchaOnBackground
val DarkOnSurface = DarkMatchaOnSurface

val DarkNavy = DarkRoast
val PrimaryBlue = MatchaPrimary
val LightBluePill = MatchaContainer
val TextBluePill = MatchaTextPill
val AccentGreen = MatchaGreen
val AccentOrange = WarmOrange
val SurfaceWhite = CreamSurface
val BackgroundLight = CreamBackground

// Challenge-specific semantic colors
val ChallengeActive = MatchaPrimary
val ChallengeInactive = Color(0xFFE5E0D5)
val ChallengeActiveTrack = Color(0xFFF1EDE4)
val CompletedPillBg = Color(0xFFFAF2DC)
val CompletedPillText = Color(0xFF9E6F18)
val DepositIconBg = MatchaContainer
val DepositIconTint = MatchaPrimary
val MutedGray = MutedHerb
val SlateBorder = Color(0xFFE5E0D5)
val DarkSlate = Color(0xFF1D261F)
