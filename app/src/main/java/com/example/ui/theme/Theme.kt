package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = CreamPrimary,
    onPrimary = Color.White,
    primaryContainer = CreamSurface,
    onPrimaryContainer = CreamOnBackground,
    secondary = CreamSecondary,
    onSecondary = CreamOnBackground,
    secondaryContainer = CreamCardBg,
    onSecondaryContainer = CreamOnBackground,
    tertiary = CreamTertiary,
    onTertiary = Color.White,
    background = CreamBackground,
    onBackground = CreamOnBackground,
    surface = CreamSurface,
    onSurface = CreamOnSurface,
    surfaceVariant = CreamCardBg,
    onSurfaceVariant = CreamOnSurface,
    outline = CreamSecondary
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkCoffeePrimary,
    onPrimary = DarkCoffeeBackground,
    primaryContainer = DarkCoffeeSurface,
    onPrimaryContainer = DarkCoffeeOnBackground,
    secondary = DarkCoffeeSecondary,
    onSecondary = DarkCoffeeOnBackground,
    secondaryContainer = DarkCoffeeCardBg,
    onSecondaryContainer = DarkCoffeeOnBackground,
    tertiary = DarkCoffeeTertiary,
    onTertiary = DarkCoffeeBackground,
    background = DarkCoffeeBackground,
    onBackground = DarkCoffeeOnBackground,
    surface = DarkCoffeeSurface,
    onSurface = DarkCoffeeOnSurface,
    surfaceVariant = DarkCoffeeCardBg,
    onSurfaceVariant = DarkCoffeeOnSurface,
    outline = DarkCoffeeSecondary
)

// Configure highly rounded shapes for a cute, organic, minimalistic feel
val CreamShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // We intentionally ignore system dynamicColor to maintain our designed Butter/Cream boutique look
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = CreamShapes,
        content = content
    )
}
