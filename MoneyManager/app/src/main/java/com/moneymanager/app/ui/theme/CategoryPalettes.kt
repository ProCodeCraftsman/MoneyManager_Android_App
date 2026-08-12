// CategoryPalettes.kt
package com.moneymanager.app.ui.theme

import androidx.compose.ui.graphics.Color

data class ThemeColorSet(val light: Color, val dark: Color)

object CategoryPalettes {

    // CALM GREEN — Nature & Botanical
    // light: rich botanical darks for white/light backgrounds
    // dark:  luminous botanical pastels for dark backgrounds
    val calmGreenPalette = listOf(
        ThemeColorSet(Color(0xFF2E7D32), Color(0xFF81C784)),  //  0 forest green
        ThemeColorSet(Color(0xFF1565C0), Color(0xFF64B5F6)),  //  1 sky blue
        ThemeColorSet(Color(0xFF00838F), Color(0xFF4DD0E1)),  //  2 teal
        ThemeColorSet(Color(0xFFC62828), Color(0xFFEF9A9A)),  //  3 coral red
        ThemeColorSet(Color(0xFFF9A825), Color(0xFFFFE082)),  //  4 amber
        ThemeColorSet(Color(0xFF6A1B9A), Color(0xFFCE93D8)),  //  5 lavender
        ThemeColorSet(Color(0xFF283593), Color(0xFF9FA8DA)),  //  6 indigo
        ThemeColorSet(Color(0xFF00695C), Color(0xFF80CBC4)),  //  7 seafoam
        ThemeColorSet(Color(0xFF9E9D24), Color(0xFFDCE775)),  //  8 moss
        ThemeColorSet(Color(0xFFD84315), Color(0xFFFFAB91)),  //  9 burnt orange
        ThemeColorSet(Color(0xFF4527A0), Color(0xFFB39DDB)),  // 10 grape
        ThemeColorSet(Color(0xFF0277BD), Color(0xFF81D4FA)),  // 11 ocean blue
        ThemeColorSet(Color(0xFF558B2F), Color(0xFFAED581)),  // 12 mint
        ThemeColorSet(Color(0xFFEF6C00), Color(0xFFFFB74D)),  // 13 peach
        ThemeColorSet(Color(0xFFAD1457), Color(0xFFF48FB1)),  // 14 rose
        ThemeColorSet(Color(0xFF37474F), Color(0xFFB0BEC5)),  // 15 slate
        ThemeColorSet(Color(0xFF1B5E20), Color(0xFFA5D6A7)),  // 16 deep forest
        ThemeColorSet(Color(0xFF0D47A1), Color(0xFF90CAF9)),  // 17 midnight
        ThemeColorSet(Color(0xFF006064), Color(0xFF80DEEA)),  // 18 deep teal
        ThemeColorSet(Color(0xFFB71C1C), Color(0xFFFF8A80)),  // 19 crimson
        ThemeColorSet(Color(0xFFF57F17), Color(0xFFFFCC80)),  // 20 golden
        ThemeColorSet(Color(0xFF4A148C), Color(0xFFE1BEE7)),  // 21 violet
        ThemeColorSet(Color(0xFF1A237E), Color(0xFFC5CAE9)),  // 22 navy
        ThemeColorSet(Color(0xFF004D40), Color(0xFFB2DFDB)),  // 23 pine
        ThemeColorSet(Color(0xFF827717), Color(0xFFE6EE9C)),  // 24 sage
        ThemeColorSet(Color(0xFFBF360C), Color(0xFFFFCCBC)),  // 25 rust
        ThemeColorSet(Color(0xFF311B92), Color(0xFFD1C4E9)),  // 26 plum
        ThemeColorSet(Color(0xFF01579B), Color(0xFFB3E5FC)),  // 27 sky
        ThemeColorSet(Color(0xFF33691E), Color(0xFFDCEDC8)),  // 28 fern
        ThemeColorSet(Color(0xFFE65100), Color(0xFFFFE0B2)),  // 29 tangerine
        ThemeColorSet(Color(0xFF880E4F), Color(0xFFF8BBD0)),  // 30 blush
        ThemeColorSet(Color(0xFF263238), Color(0xFFCFD8DC)),  // 31 stone
        ThemeColorSet(Color(0xFF388E3C), Color(0xFF66BB6A)),  // 32 meadow
        ThemeColorSet(Color(0xFF1976D2), Color(0xFF42A5F5)),  // 33 cornflower
        ThemeColorSet(Color(0xFF0097A7), Color(0xFF26C6DA)),  // 34 lagoon
        ThemeColorSet(Color(0xFFE53935), Color(0xFFFF5252)),  // 35 tomato
        ThemeColorSet(Color(0xFFF4511E), Color(0xFFFF8A65)),  // 36 warm orange
        ThemeColorSet(Color(0xFF8E24AA), Color(0xFFBA68C8)),  // 37 orchid
        ThemeColorSet(Color(0xFF3949AB), Color(0xFF7986CB)),  // 38 periwinkle
        ThemeColorSet(Color(0xFF00897B), Color(0xFF26A69A))   // 39 jade
    )

    // COCO BROWN — Earthy, Warm, Autumnal
    // light: deep earthy tones for white/light backgrounds
    // dark:  warm glowing tones for dark backgrounds
    val cocoBrownPalette = listOf(
        // --- High-Contrast Premium Neutrals  ---
        ThemeColorSet(Color(0xFF231F1A), Color(0xFFF5EFEB)),  //  0 Crisp champagne (bright & elegant)
        ThemeColorSet(Color(0xFFe6af2e), Color(0xFFFFCA28)),  //  1 Luminous gold
        ThemeColorSet(Color(0xFF79B473), Color(0xFF69F0AE)),  //  2 Glowing spring green
        ThemeColorSet(Color(0xFFA2A3BB), Color(0xFFD1D2EE)),  //  3 Luminous periwinkle/silver
        ThemeColorSet(Color(0xFFD81E5B), Color(0xFFFF6090)),  //  4 Vibrant watermelon pink

        // --- High-Visibility Pops (Oranges & Golds) ---
        ThemeColorSet(Color(0xFFD84315), Color(0xFFFF7043)),  //  5 Glowing coral
        ThemeColorSet(Color(0xFFE64A19), Color(0xFFFF8A65)),  //  6 Vibrant peach
        ThemeColorSet(Color(0xFFE65100), Color(0xFFFF9800)),  //  7 Electric orange
        ThemeColorSet(Color(0xFFF57C00), Color(0xFFFFB74D)),  //  8 Bright mango
        ThemeColorSet(Color(0xFFFF6D00), Color(0xFFFFD180)),  //  9 Neon peach/yellow

        // --- Nature Greens (Crisp & Fresh against Brown) ---
        ThemeColorSet(Color(0xFFBF360C), Color(0xFFFF6E40)),  // 10 Glowing fiery orange
        ThemeColorSet(Color(0xFF827717), Color(0xFFEEFF41)),  // 11 Electric chartreuse
        ThemeColorSet(Color(0xFF558B2F), Color(0xFFB2FF59)),  // 12 Vibrant yellow-green
        ThemeColorSet(Color(0xFF33691E), Color(0xFF76FF03)),  // 13 Electric lime
        ThemeColorSet(Color(0xFF2E7D32), Color(0xFF69F0AE)),  // 14 Glowing mint
        ThemeColorSet(Color(0xFF1B5E20), Color(0xFF1DE9B6)),  // 15 Vibrant teal-mint

        // --- The Best Contrast Group: Teals & Blues ---
        ThemeColorSet(Color(0xFF004D40), Color(0xFF1DE9B6)),  // 16 Bright turquoise
        ThemeColorSet(Color(0xFF006064), Color(0xFF00E5FF)),  // 17 Electric cyan
        ThemeColorSet(Color(0xFF00838F), Color(0xFF18FFFF)),  // 18 Neon aqua

        // --- High Contrast Reds & Pinks ---
        ThemeColorSet(Color(0xFFC62828), Color(0xFFFF5252)),  // 19 Vibrant coral red
        ThemeColorSet(Color(0xFFB71C1C), Color(0xFFFF1744)),  // 20 Electric crimson
        ThemeColorSet(Color(0xFFD50000), Color(0xFFFF4081)),  // 21 Hot pink
        ThemeColorSet(Color(0xFFFF1744), Color(0xFFFF80AB)),  // 22 Neon flamingo

        // --- Purples & Berries ---
        ThemeColorSet(Color(0xFF880E4F), Color(0xFFFF4081)),  // 23 Vibrant magenta
        ThemeColorSet(Color(0xFFAD1457), Color(0xFFE040FB)),  // 24 Neon fuchsia
        ThemeColorSet(Color(0xFF7B1FA2), Color(0xFFEA80FC)),  // 25 Bright orchid
        ThemeColorSet(Color(0xFF4A148C), Color(0xFFB388FF)),  // 26 Glowing lavender
        ThemeColorSet(Color(0xFF651FFF), Color(0xFF8C9EFF)),  // 27 Electric periwinkle

        // --- Deep Blues (Stands out sharply against brown) ---
        ThemeColorSet(Color(0xFF311B92), Color(0xFF7C4DFF)),  // 28 Vibrant deep purple
        ThemeColorSet(Color(0xFF1A237E), Color(0xFF536DFE)),  // 29 Electric indigo
        ThemeColorSet(Color(0xFF0D47A1), Color(0xFF448AFF)),  // 30 Bright blue
        ThemeColorSet(Color(0xFF2962FF), Color(0xFF40C4FF)),  // 31 Vibrant sky blue
        ThemeColorSet(Color(0xFF0091EA), Color(0xFF00E5FF)),  // 32 Cyan splash

        // --- Bright UI Utilitarian Pops ---
        ThemeColorSet(Color(0xFFD32F2F), Color(0xFFFF5252)),  // 33 Vibrant error red
        ThemeColorSet(Color(0xFFFFC107), Color(0xFFFFD740)),  // 34 Glowing warning yellow
        ThemeColorSet(Color(0xFF4CAF50), Color(0xFF69F0AE)),  // 35 Vibrant success mint
        ThemeColorSet(Color(0xFF9C27B0), Color(0xFFE040FB)),  // 36 Vibrant info purple

        // --- Clean Neutrals (No muddy greys) ---
        ThemeColorSet(Color(0xFF37474F), Color(0xFFCFD8DC)),  // 37 Crisp silver
        ThemeColorSet(Color(0xFF212121), Color(0xFFFAFAFA)),  // 38 Pure stark white
        ThemeColorSet(Color(0xFF1E2628), Color(0xFF80CBC4))   // 39 Luminous teal mist
    )

    // MIDNIGHT BLUE — Jewel Tones, High Contrast, Vibrant
    // light: toned jewel hues readable on white/light backgrounds
    // dark:  electric accents that pop on dark backgrounds
    val midnightBluePalette = listOf(
        ThemeColorSet(Color(0xFF1976D2), Color(0xFF2962FF)),  //  0 electric blue
        ThemeColorSet(Color(0xFF0288D1), Color(0xFF00B0FF)),  //  1 azure
        ThemeColorSet(Color(0xFF0097A7), Color(0xFF00E5FF)),  //  2 neon cyan
        ThemeColorSet(Color(0xFF00897B), Color(0xFF1DE9B6)),  //  3 seafoam
        ThemeColorSet(Color(0xFF388E3C), Color(0xFF00E676)),  //  4 neon green
        ThemeColorSet(Color(0xFF689F38), Color(0xFF76FF03)),  //  5 lime
        ThemeColorSet(Color(0xFFAFB42B), Color(0xFFC6FF00)),  //  6 chartreuse
        ThemeColorSet(Color(0xFFFFA000), Color(0xFFFFEA00)),  //  7 neon yellow
        ThemeColorSet(Color(0xFFFF8F00), Color(0xFFFFC400)),  //  8 neon amber
        ThemeColorSet(Color(0xFFF57C00), Color(0xFFFF9100)),  //  9 neon orange
        ThemeColorSet(Color(0xFFE64A19), Color(0xFFFF3D00)),  // 10 neon deep orange
        ThemeColorSet(Color(0xFFD32F2F), Color(0xFFFF1744)),  // 11 neon red
        ThemeColorSet(Color(0xFFC2185B), Color(0xFFF50057)),  // 12 neon pink
        ThemeColorSet(Color(0xFF8E24AA), Color(0xFFD500F9)),  // 13 neon purple
        ThemeColorSet(Color(0xFF512DA8), Color(0xFF651FFF)),  // 14 neon deep purple
        ThemeColorSet(Color(0xFF303F9F), Color(0xFF3D5AFE)),  // 15 neon indigo
        ThemeColorSet(Color(0xFF1565C0), Color(0xFF90CAF9)),  // 16 blue 800
        ThemeColorSet(Color(0xFF0277BD), Color(0xFF81D4FA)),  // 17 light blue 800
        ThemeColorSet(Color(0xFF00838F), Color(0xFF26C6DA)),  // 18 cyan 700
        ThemeColorSet(Color(0xFF00695C), Color(0xFF26A69A)),  // 19 teal 800
        ThemeColorSet(Color(0xFF2E7D32), Color(0xFF66BB6A)),  // 20 green 800
        ThemeColorSet(Color(0xFF558B2F), Color(0xFFAED581)),  // 21 light green 800
        ThemeColorSet(Color(0xFF9E9D24), Color(0xFFDCE775)),  // 22 lime 800
        ThemeColorSet(Color(0xFFF9A825), Color(0xFFFFE082)),  // 23 amber 800
        ThemeColorSet(Color(0xFFEF6C00), Color(0xFFFFB74D)),  // 24 orange 800
        ThemeColorSet(Color(0xFFD84315), Color(0xFFFFAB91)),  // 25 deep orange 700
        ThemeColorSet(Color(0xFFBF360C), Color(0xFFFFCCBC)),  // 26 deep orange 900
        ThemeColorSet(Color(0xFFC62828), Color(0xFFEF9A9A)),  // 27 red 800
        ThemeColorSet(Color(0xFFAD1457), Color(0xFFF48FB1)),  // 28 pink 700
        ThemeColorSet(Color(0xFF6A1B9A), Color(0xFFCE93D8)),  // 29 purple 800
        ThemeColorSet(Color(0xFF4527A0), Color(0xFFB39DDB)),  // 30 deep purple 800
        ThemeColorSet(Color(0xFF283593), Color(0xFF9FA8DA)),  // 31 indigo 800
        ThemeColorSet(Color(0xFF3949AB), Color(0xFF42A5F5)),  // 32 indigo 600
        ThemeColorSet(Color(0xFF00ACC1), Color(0xFF00BCD4)),  // 33 cyan 600
        ThemeColorSet(Color(0xFF43A047), Color(0xFF4CAF50)),  // 34 green 600
        ThemeColorSet(Color(0xFF7CB342), Color(0xFF8BC34A)),  // 35 light green 600
        ThemeColorSet(Color(0xFFC0CA33), Color(0xFFCDDC39)),  // 36 lime 600
        ThemeColorSet(Color(0xFFFFB300), Color(0xFFFFEB3B)),  // 37 amber 600
        ThemeColorSet(Color(0xFF9C27B0), Color(0xFFAB47BC)),  // 38 purple 500
        ThemeColorSet(Color(0xFF3F51B5), Color(0xFF7986CB))   // 39 indigo 500
    )

    fun getPaletteForTheme(theme: AppTheme): List<ThemeColorSet> {
        return when (theme) {
            AppTheme.CALM_GREEN -> calmGreenPalette
            AppTheme.COCO_BROWN -> cocoBrownPalette
            AppTheme.MIDNIGHT_BLUE -> midnightBluePalette
        }
    }

    fun getColor(theme: AppTheme, isDarkMode: Boolean, index: Int): Color {
        val set = getPaletteForTheme(theme)[index % 40]
        return if (isDarkMode) set.dark else set.light
    }
}
