package io.vscodex.net.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import io.vscodex.net.resources.R

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val InterFontFamily = FontFamily(
    Font(googleFont = GoogleFont("Inter"), fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("Inter"), fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = GoogleFont("Inter"), fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = GoogleFont("Inter"), fontProvider = provider, weight = FontWeight.Bold),
)

private val baseline = Typography()

val Typography = Typography(
    displayLarge = baseline.displayLarge.copy(fontFamily = InterFontFamily),
    displayMedium = baseline.displayMedium.copy(fontFamily = InterFontFamily),
    displaySmall = baseline.displaySmall.copy(fontFamily = InterFontFamily),
    headlineLarge = baseline.headlineLarge.copy(fontFamily = InterFontFamily),
    headlineMedium = baseline.headlineMedium.copy(fontFamily = InterFontFamily),
    headlineSmall = baseline.headlineSmall.copy(fontFamily = InterFontFamily),
    titleLarge = baseline.titleLarge.copy(fontFamily = InterFontFamily),
    titleMedium = baseline.titleMedium.copy(fontFamily = InterFontFamily),
    titleSmall = baseline.titleSmall.copy(fontFamily = InterFontFamily),
    bodyLarge = baseline.bodyLarge.copy(fontFamily = InterFontFamily),
    bodyMedium = baseline.bodyMedium.copy(fontFamily = InterFontFamily),
    bodySmall = baseline.bodySmall.copy(fontFamily = InterFontFamily),
    labelLarge = baseline.labelLarge.copy(fontFamily = InterFontFamily),
    labelMedium = baseline.labelMedium.copy(fontFamily = InterFontFamily),
    labelSmall = baseline.labelSmall.copy(fontFamily = InterFontFamily),
)
