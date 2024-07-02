package net.ttiimm.morsecode.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import net.ttiimm.morsecode.R

val AbrilFatface = FontFamily(
    Font(R.font.abril_fatface_regular)
)

val AppTypography = Typography(
    displayMedium = TextStyle(
        fontFamily = AbrilFatface,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp
    )
)