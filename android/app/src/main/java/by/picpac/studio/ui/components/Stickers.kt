package by.picpac.studio.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import by.picpac.studio.data.StickerKind
import by.picpac.studio.ui.theme.PicPacColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Цвет стикера по умолчанию — подобран под каждый рисунок. */
fun StickerKind.defaultColor(): Color = when (this) {
    StickerKind.HEART, StickerKind.LOVE -> Color(0xFFDD5757)
    StickerKind.STAR, StickerKind.SPARKLE -> Color(0xFFE2B44C)
    StickerKind.SUN -> Color(0xFFF5C242)
    StickerKind.FLOWER -> Color(0xFFF29BB9)
    StickerKind.CLOUD -> Color(0xFFD9E8F2)
    StickerKind.CAMERA -> Color(0xFF364C49)
    StickerKind.CAT -> PicPacColors.CatFur
}

/** Стикер, нарисованный кодом. [fontSize] нужен только надписи «love». */
@Composable
fun StickerView(kind: StickerKind, modifier: Modifier, fontSize: TextUnit = TextUnit.Unspecified) {
    if (kind == StickerKind.LOVE) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text(
                "love",
                color = kind.defaultColor(),
                fontFamily = FontFamily.Cursive,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                fontSize = fontSize,
                maxLines = 1,
            )
        }
        return
    }
    Canvas(modifier) { drawSticker(kind) }
}

private fun DrawScope.drawSticker(kind: StickerKind) {
    val w = size.minDimension
    val c = center
    val color = kind.defaultColor()
    val outline = Color(0xFF3A2A1E)
    when (kind) {
        StickerKind.HEART -> {
            val heart = Path().apply {
                moveTo(c.x, c.y + w * 0.38f)
                cubicTo(c.x - w * 0.55f, c.y + w * 0.02f, c.x - w * 0.38f, c.y - w * 0.42f, c.x, c.y - w * 0.18f)
                cubicTo(c.x + w * 0.38f, c.y - w * 0.42f, c.x + w * 0.55f, c.y + w * 0.02f, c.x, c.y + w * 0.38f)
                close()
            }
            drawPath(heart, color)
            drawPath(heart, outline, style = Stroke(w * 0.035f))
        }
        StickerKind.STAR -> {
            val star = starPath(c, w * 0.46f, w * 0.2f, 5)
            drawPath(star, color)
            drawPath(star, outline, style = Stroke(w * 0.03f))
        }
        StickerKind.SPARKLE -> {
            // Четырёхлучевой «блеск», как золотая звезда в брендбуке.
            drawPath(starPath(c, w * 0.48f, w * 0.1f, 4), color)
            drawPath(starPath(c + Offset(w * 0.28f, -w * 0.28f), w * 0.14f, w * 0.03f, 4), color)
        }
        StickerKind.SUN -> {
            repeat(10) { i ->
                val a = i * 2 * PI / 10
                drawLine(
                    color,
                    c + Offset((cos(a) * w * 0.3f).toFloat(), (sin(a) * w * 0.3f).toFloat()),
                    c + Offset((cos(a) * w * 0.46f).toFloat(), (sin(a) * w * 0.46f).toFloat()),
                    strokeWidth = w * 0.06f, cap = StrokeCap.Round,
                )
            }
            drawCircle(color, radius = w * 0.24f, center = c)
            drawCircle(outline, radius = w * 0.24f, center = c, style = Stroke(w * 0.03f))
        }
        StickerKind.FLOWER -> {
            repeat(5) { i ->
                val a = i * 2 * PI / 5 - PI / 2
                val petal = c + Offset((cos(a) * w * 0.22f).toFloat(), (sin(a) * w * 0.22f).toFloat())
                drawCircle(color, radius = w * 0.18f, center = petal)
                drawCircle(outline, radius = w * 0.18f, center = petal, style = Stroke(w * 0.025f))
            }
            drawCircle(Color(0xFFF5C242), radius = w * 0.13f, center = c)
            drawCircle(outline, radius = w * 0.13f, center = c, style = Stroke(w * 0.025f))
        }
        StickerKind.CLOUD -> {
            val parts = listOf(Offset(-0.2f, 0.06f) to 0.2f, Offset(0.02f, -0.08f) to 0.26f, Offset(0.24f, 0.06f) to 0.19f)
            parts.forEach { (o, r) -> drawCircle(outline, radius = w * r + w * 0.03f, center = c + o * w) }
            parts.forEach { (o, r) -> drawCircle(color, radius = w * r, center = c + o * w) }
            drawRoundRect(color, c + Offset(-w * 0.38f, w * 0.02f), Size(w * 0.78f, w * 0.22f), CornerRadius(w * 0.1f))
        }
        StickerKind.CAMERA -> {
            val body = Size(w * 0.86f, w * 0.58f)
            val topLeft = c - Offset(body.width / 2, body.height / 2 - w * 0.06f)
            drawRoundRect(color, topLeft + Offset(w * 0.12f, -w * 0.12f), Size(w * 0.24f, w * 0.14f), CornerRadius(w * 0.04f))
            drawRoundRect(color, topLeft, body, CornerRadius(w * 0.1f))
            drawCircle(Color.White, radius = w * 0.2f, center = c + Offset(0f, w * 0.06f))
            drawCircle(color, radius = w * 0.13f, center = c + Offset(0f, w * 0.06f))
            drawCircle(Color(0xFFDD5757), radius = w * 0.04f, center = topLeft + Offset(body.width - w * 0.12f, w * 0.1f))
        }
        StickerKind.CAT -> drawCatFace(c, w * 0.36f)
        StickerKind.LOVE -> Unit
    }
}

private fun starPath(c: Offset, outer: Float, inner: Float, points: Int) = Path().apply {
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) outer else inner
        val a = i * PI / points - PI / 2
        val p = c + Offset((cos(a) * r).toFloat(), (sin(a) * r).toFloat())
        if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
    }
    close()
}
