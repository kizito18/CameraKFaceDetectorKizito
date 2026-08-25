package com.binkes.camerakfacedetectorkizito.models

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import camerakfacedetectorkizito.shared.generated.resources.Res
import camerakfacedetectorkizito.shared.generated.resources.manrope_bold
import camerakfacedetectorkizito.shared.generated.resources.manrope_medium
import camerakfacedetectorkizito.shared.generated.resources.manrope_regular
import camerakfacedetectorkizito.shared.generated.resources.manrope_semi_bold
import org.jetbrains.compose.resources.Font


@Composable
private fun manropeFontFamily() = FontFamily(
    Font(Res.font.manrope_regular, FontWeight.Normal),
    Font(Res.font.manrope_medium, FontWeight.Medium),
    Font(Res.font.manrope_semi_bold, FontWeight.SemiBold),
    Font(Res.font.manrope_bold, FontWeight.Bold)
)



object AppTypography {

    // 1. Add this nested object for raw sizes (No @Composable here)
    object Sizes {
        val displaySize = 32.sp
        val heading1Size = 24.sp
        val heading2Size = 20.sp
        val bodyLargeSize = 18.sp
        val bodyMainSize = 16.sp
        val labelSize = 14.sp
        val captionSize = 12.sp
        val smallTagSize = 10.sp
    }

    // Display: size 32 - Bold
    val display: TextStyle
        @Composable get() = TextStyle(
            fontFamily = manropeFontFamily(),
            fontWeight = FontWeight.Bold,
            fontSize = Sizes.displaySize, // Use the size from the object
            lineHeight = 40.sp
        )

    // Heading 1: size 24 - Bold
    val heading1: TextStyle
        @Composable get() = TextStyle(
            fontFamily = manropeFontFamily(),
            fontWeight = FontWeight.Bold,
            fontSize = Sizes.heading1Size,
            lineHeight = 32.sp
        )

    // Heading 2: size 20 - SemiBold
    val heading2: TextStyle
        @Composable get() = TextStyle(
            fontFamily = manropeFontFamily(),
            fontWeight = FontWeight.SemiBold,
            fontSize = Sizes.heading2Size,
            lineHeight = 28.sp
        )

    // Body Large: size 18 - Medium
    val bodyLarge: TextStyle
        @Composable get() = TextStyle(
            fontFamily = manropeFontFamily(),
            fontWeight = FontWeight.Medium,
            fontSize = Sizes.bodyLargeSize,
            lineHeight = 26.sp
        )

    // Body Main: size 16 - Regular
    val bodyMain: TextStyle
        @Composable get() = TextStyle(
            fontFamily = manropeFontFamily(),
            fontWeight = FontWeight.Normal,
            fontSize = Sizes.bodyMainSize,
            lineHeight = 24.sp
        )

    // Labels/Buttons: size 14 - SemiBold
    val label: TextStyle
        @Composable get() = TextStyle(
            fontFamily = manropeFontFamily(),
            fontWeight = FontWeight.SemiBold,
            fontSize = Sizes.labelSize,
            lineHeight = 20.sp
        )

    // Captions: size 12 - Regular
    val caption: TextStyle
        @Composable get() = TextStyle(
            fontFamily = manropeFontFamily(),
            fontWeight = FontWeight.Normal,
            fontSize = Sizes.captionSize,
            lineHeight = 16.sp
        )

    // Small Tags: size 10 - Bold
    val smallTag: TextStyle
        @Composable get() = TextStyle(
            fontFamily = manropeFontFamily(),
            fontWeight = FontWeight.Bold,
            fontSize = Sizes.smallTagSize,
            lineHeight = 14.sp
        )
}