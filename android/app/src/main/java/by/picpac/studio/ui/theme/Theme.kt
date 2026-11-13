package by.picpac.studio.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import by.picpac.studio.R

/**
 * Палитра из брендбука PicPac: красный #DD5757 — только для главных действий и цен,
 * тёмно-зелёный #364C49 — текст, кремовый #F6EFE6 — фон. Остальные цвета — их оттенки.
 * Котик — отдельный тёплый рыжий, он нарисован и не должен сливаться с кнопками.
 */
object PicPacColors {
    val Red = Color(0xFFDD5757)
    val RedDark = Color(0xFFB8403F)
    val RedSoft = Color(0xFFF7DEDB)
    val Ink = Color(0xFF364C49)
    val InkSoft = Color(0xFF5E706C)
    val Paper = Color(0xFFF6EFE6)
    val PaperDeep = Color(0xFFEDE3D5)
    val Line = Color(0xFFE2D6C6)

    // Старые имена: их ещё используют рисунки товаров.
    val Cream = Paper
    val Terracotta = Red
    val Brick = RedDark
    val Honey = Color(0xFFD2925C)
    val Green = Ink
    val Dark = Ink
    val Peach = RedSoft
    val Sand = PaperDeep

    val CatFur = Color(0xFFF08A3C)
    val CatStripe = Color(0xFFC9662A)
    val CatCream = Color(0xFFFFF4E6)
    val CatPink = Color(0xFFF4A7A0)
    val CatLine = Color(0xFF3A2A1E)
}

private val LightColors = lightColorScheme(
    primary = PicPacColors.Red,
    onPrimary = Color.White,
    primaryContainer = PicPacColors.RedSoft,
    onPrimaryContainer = PicPacColors.RedDark,
    secondary = PicPacColors.Ink,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDE5E2),
    onSecondaryContainer = PicPacColors.Ink,
    tertiary = PicPacColors.Honey,
    onTertiary = Color.White,
    background = PicPacColors.Paper,
    onBackground = PicPacColors.Ink,
    surface = Color(0xFFFFFCF8),
    onSurface = PicPacColors.Ink,
    surfaceVariant = PicPacColors.PaperDeep,
    // Приглушённый текст: контраст с кремовым фоном не ниже 4,5:1.
    onSurfaceVariant = PicPacColors.InkSoft,
    outline = PicPacColors.Line,
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE87474),
    onPrimary = Color(0xFF2B0B0B),
    primaryContainer = Color(0xFF4A2525),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFF9FB5B0),
    onSecondary = Color(0xFF14201E),
    secondaryContainer = Color(0xFF2C3B38),
    onSecondaryContainer = Color(0xFFDDE5E2),
    tertiary = PicPacColors.Honey,
    onTertiary = Color(0xFF2A1A0C),
    background = Color(0xFF161D1C),
    onBackground = Color(0xFFF2ECE3),
    surface = Color(0xFF1F2826),
    onSurface = Color(0xFFF2ECE3),
    surfaceVariant = Color(0xFF283331),
    onSurfaceVariant = Color(0xFFB7C2BF),
    outline = Color(0xFF34413E),
    error = Color(0xFFF2B8B5),
)

/**
 * Jost — открытый (OFL) геометрический шрифт по мотивам Futura. Futura PT из брендбука платная,
 * поэтому берём ближайший бесплатный аналог. Файл один (вариативный), толщину задаём настройкой.
 */
private fun jost(weight: Int) = Font(
    R.font.jost,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Jost = FontFamily(jost(400), jost(500), jost(600), jost(700))

private val AppTypography = Typography().run {
    fun TextStyle.j(weight: FontWeight, size: Int, line: Int, spacing: Double = 0.0) = copy(
        fontFamily = Jost,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = line.sp,
        letterSpacing = spacing.em,
    )
    copy(
        displaySmall = displaySmall.j(FontWeight.Medium, 34, 38, -0.01),
        headlineLarge = headlineLarge.j(FontWeight.Medium, 30, 34, -0.01),
        headlineMedium = headlineMedium.j(FontWeight.Medium, 26, 30, -0.01),
        headlineSmall = headlineSmall.j(FontWeight.Medium, 22, 28),
        titleLarge = titleLarge.j(FontWeight.Medium, 20, 26),
        titleMedium = titleMedium.j(FontWeight.Medium, 17, 22),
        titleSmall = titleSmall.j(FontWeight.Medium, 15, 20),
        bodyLarge = bodyLarge.j(FontWeight.Normal, 16, 24),
        bodyMedium = bodyMedium.j(FontWeight.Normal, 15, 22),
        bodySmall = bodySmall.j(FontWeight.Normal, 13, 18),
        labelLarge = labelLarge.j(FontWeight.Medium, 16, 20, 0.01),
        labelMedium = labelMedium.j(FontWeight.Medium, 13, 16, 0.02),
        labelSmall = labelSmall.j(FontWeight.Medium, 12, 16, 0.04),
    )
}

/** Скругления умеренные: «таблетки» и пузыри повсюду — главный признак шаблонного дизайна. */
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
)

@Composable
fun PicPacTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
