package by.picpac.studio.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import by.picpac.studio.ui.theme.PicPacColors

/** Какую иллюстрацию рисовать вместо фото товара. */
enum class ArtKind { PRINTS, BOOK, LAYFLAT, PREMIUM, COLLAGE }

/**
 * Лёгкие векторные иллюстрации вместо тяжёлых фото из макета:
 * стопка снимков для печати и раскрытая книга для фотокниг.
 */
@Composable
fun ProductArt(kind: ArtKind, modifier: Modifier = Modifier) {
    val background = when (kind) {
        ArtKind.PRINTS -> listOf(Color(0xFFDCE5E0), Color(0xFFC3D2CA))
        ArtKind.BOOK -> listOf(Color(0xFFF8E6D6), Color(0xFFF1D2BA))
        ArtKind.LAYFLAT -> listOf(Color(0xFFF3E5CF), Color(0xFFE6CFAE))
        ArtKind.PREMIUM -> listOf(Color(0xFFEFD9D0), Color(0xFFDDB9A8))
        ArtKind.COLLAGE -> listOf(Color(0xFFF6E7C9), Color(0xFFEBD3A3))
    }
    Canvas(modifier.clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(background))) {
        when (kind) {
            ArtKind.PRINTS -> {
                val w = size.minDimension * 0.5f
                val h = w * 0.78f
                photoCard(center + Offset(-w * 0.2f, h * 0.05f), w, h, -14f, 0)
                photoCard(center + Offset(w * 0.18f, -h * 0.02f), w, h, 9f, 1)
                photoCard(center + Offset(0f, h * 0.12f), w, h, -2f, 2)
            }
            ArtKind.BOOK -> openBook(PicPacColors.Terracotta)
            ArtKind.LAYFLAT -> openBook(PicPacColors.Green)
            ArtKind.PREMIUM -> openBook(PicPacColors.Brick)
            ArtKind.COLLAGE -> collageSheet()
        }
    }
}

/** Лист-коллаж: большое фото слева и два маленьких справа. */
private fun DrawScope.collageSheet() {
    val h = size.height * 0.66f
    val w = h * 0.78f
    val c = center
    rotate(-6f, c) {
        val topLeft = c - Offset(w / 2, h / 2)
        drawRoundRect(Color.Black.copy(alpha = 0.12f), topLeft + Offset(3f, 6f), Size(w, h), CornerRadius(8f))
        drawRoundRect(Color.White, topLeft, Size(w, h), CornerRadius(8f))
        val pad = w * 0.07f
        val gap = w * 0.04f
        val innerW = w - pad * 2
        val innerH = h - pad * 2
        val leftW = innerW * 0.58f
        scene(topLeft + Offset(pad, pad), Size(leftW, innerH), 0)
        val rightW = innerW - leftW - gap
        val cellH = (innerH - gap) / 2
        scene(topLeft + Offset(pad + leftW + gap, pad), Size(rightW, cellH), 1)
        scene(topLeft + Offset(pad + leftW + gap, pad + cellH + gap), Size(rightW, cellH), 2)
    }
}

private fun DrawScope.photoCard(c: Offset, w: Float, h: Float, angle: Float, variant: Int) {
    rotate(angle, c) {
        val topLeft = c - Offset(w / 2, h / 2)
        drawRoundRect(Color.Black.copy(alpha = 0.12f), topLeft + Offset(3f, 6f), Size(w, h), CornerRadius(8f))
        drawRoundRect(Color.White, topLeft, Size(w, h), CornerRadius(8f))
        val pad = w * 0.07f
        scene(topLeft + Offset(pad, pad), Size(w - pad * 2, h - pad * 2), variant)
    }
}

private fun DrawScope.openBook(cover: Color) {
    val w = size.width * 0.8f
    val h = size.height * 0.52f
    val topLeft = center - Offset(w / 2, h / 2)
    drawRoundRect(Color.Black.copy(alpha = 0.12f), topLeft + Offset(4f, 8f), Size(w, h), CornerRadius(10f))
    drawRoundRect(cover, topLeft - Offset(6f, 6f), Size(w + 12f, h + 12f), CornerRadius(12f))
    val pageW = w / 2
    for (i in 0..1) {
        val pageTopLeft = topLeft + Offset(pageW * i, 0f)
        drawRect(Color(0xFFFFFBF5), pageTopLeft, Size(pageW, h))
        val pad = pageW * 0.08f
        scene(pageTopLeft + Offset(pad, pad), Size(pageW - pad * 2, h - pad * 2), i + 1)
    }
    // Тень у корешка.
    drawRect(
        Brush.horizontalGradient(
            listOf(Color.Transparent, Color.Black.copy(alpha = 0.18f), Color.Transparent),
            startX = center.x - 14f, endX = center.x + 14f,
        ),
        Offset(center.x - 14f, topLeft.y), Size(28f, h),
    )
}

/** Мини-пейзаж «закат в горах», как на фото в макете. */
private fun DrawScope.scene(topLeft: Offset, s: Size, variant: Int) {
    val skies = listOf(
        listOf(Color(0xFFF6C38B), Color(0xFFF7E3C4)),
        listOf(Color(0xFFB9D3DC), Color(0xFFEAF1EE)),
        listOf(Color(0xFFF2A97E), Color(0xFFF8D9B8)),
    )
    val sky = skies[variant % skies.size]
    clipRect(topLeft.x, topLeft.y, topLeft.x + s.width, topLeft.y + s.height) {
        drawRect(Brush.verticalGradient(sky, startY = topLeft.y, endY = topLeft.y + s.height), topLeft, s)
        drawCircle(Color(0xFFFFF1D6), radius = s.minDimension * 0.13f, center = topLeft + Offset(s.width * 0.7f, s.height * 0.38f))
        val far = Path().apply {
            moveTo(topLeft.x, topLeft.y + s.height)
            lineTo(topLeft.x + s.width * 0.35f, topLeft.y + s.height * 0.38f)
            lineTo(topLeft.x + s.width * 0.62f, topLeft.y + s.height * 0.72f)
            lineTo(topLeft.x + s.width * 0.8f, topLeft.y + s.height * 0.5f)
            lineTo(topLeft.x + s.width, topLeft.y + s.height * 0.68f)
            lineTo(topLeft.x + s.width, topLeft.y + s.height)
            close()
        }
        drawPath(far, Color(0xFF7D8F86))
        val near = Path().apply {
            moveTo(topLeft.x, topLeft.y + s.height)
            lineTo(topLeft.x, topLeft.y + s.height * 0.78f)
            lineTo(topLeft.x + s.width * 0.25f, topLeft.y + s.height * 0.62f)
            lineTo(topLeft.x + s.width * 0.55f, topLeft.y + s.height * 0.86f)
            lineTo(topLeft.x + s.width, topLeft.y + s.height * 0.74f)
            lineTo(topLeft.x + s.width, topLeft.y + s.height)
            close()
        }
        drawPath(near, PicPacColors.Green)
    }
}
