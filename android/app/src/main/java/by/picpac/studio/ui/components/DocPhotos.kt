package by.picpac.studio.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import by.picpac.studio.data.FormatOption
import by.picpac.studio.data.Pricing

/**
 * Одно фото на документ: снимок обрезан под нужные пропорции, поверх — пунктирный овал,
 * куда должно попасть лицо, и линии макушки и подбородка. Это подсказка для кадрирования, а не проверка.
 */
@Composable
fun DocPhoto(uri: String, format: FormatOption, width: Dp, guide: Boolean = true) {
    Box(Modifier.width(width).aspectRatio(format.widthMm.toFloat() / format.heightMm).background(Color.White)) {
        PhotoThumb(uri, Modifier.fillMaxSize())
        if (guide) {
            Canvas(Modifier.fillMaxSize()) {
                val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                val ow = size.width * 0.62f
                val oh = size.height * 0.68f
                drawOval(
                    Color.White, Offset((size.width - ow) / 2, size.height * 0.12f), Size(ow, oh),
                    style = Stroke(2.dp.toPx(), pathEffect = dash),
                )
                listOf(0.12f, 0.80f).forEach { y ->
                    drawLine(Color(0xFFDD5757), Offset(0f, size.height * y), Offset(size.width, size.height * y), 1.5.dp.toPx(), pathEffect = dash)
                }
            }
        }
    }
}

/** Лист 10×15 с копиями фото — так он придёт из печати. Лист повёрнут, если так помещается больше копий. */
@Composable
fun DocSheet(uri: String, format: FormatOption, modifier: Modifier = Modifier) {
    val count = Pricing.docsPerSheet(format)
    val upright = (100 / format.widthMm) * (150 / format.heightMm)
    val landscape = count > upright
    val cols = if (landscape) 150 / format.widthMm else 100 / format.widthMm
    val rows = (count + cols - 1) / cols
    val sheetRatio = if (landscape) 1.5f else 1f / 1.5f
    val sheetW = if (landscape) 260.dp else 170.dp
    val sheetMm = if (landscape) 150 else 100
    // Ширина фото на экране: внутренняя ширина листа (минус поля и зазоры) пропорционально миллиметрам.
    val photoW = (sheetW - 20.dp) * format.widthMm / sheetMm
    Box(
        modifier
            .width(sheetW)
            .aspectRatio(sheetRatio)
            .background(Color.White, RectangleShape)
            .border(1.dp, Color(0x22000000))
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(rows) { r ->
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(minOf(cols, count - r * cols)) {
                        DocPhoto(uri, format, photoW, guide = false)
                    }
                }
            }
        }
    }
}
