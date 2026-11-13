package by.picpac.studio.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import by.picpac.studio.R

private val Outline = Color(0xFF2B1E16)
private val Kraft = Color(0xFFD9A066)
private val KraftSide = Color(0xFFC0844C)
private val KraftInside = Color(0xFF9C6534)
private val KraftFlap = Color(0xFFE4B07A)
private val Tape = Color(0xFFEBC48F)

/** Точка рисунка в долях его ширины. */
private fun DrawScope.p(x: Float, y: Float) = Offset(size.width * x, size.width * y)

private fun DrawScope.shape(pts: List<Offset>, fill: Color) {
    val path = Path().apply {
        moveTo(pts[0].x, pts[0].y)
        pts.drop(1).forEach { lineTo(it.x, it.y) }
        close()
    }
    drawPath(path, fill)
    drawPath(path, Outline, style = Stroke(size.width * 0.022f, join = StrokeJoin.Round))
}

/**
 * Котик выглядывает из картонной коробки с надписью «Тут пусто» — для пустой корзины.
 * Ракурс три четверти сверху, как у коробки на баннере «Подарки». Кот — иллюстрация из макета команды,
 * коробка нарисована кодом в том же стиле: тёмный контур, тёплые цвета крафт-картона.
 */
@Composable
fun CatInBox(modifier: Modifier = Modifier, width: Dp = 240.dp) {
    BoxWithConstraints(modifier.width(width).aspectRatio(1f / 0.76f)) {
        val w = maxWidth
        // Рисунок задан в долях ширины с запасом сверху — сдвигаем его вверх, чтобы не было пустоты.
        val lift = Modifier.fillMaxSize().offset(y = -w * 0.26f)
        // Сзади: внутренние стенки и задние клапаны — они за котом.
        Canvas(lift) {
            val fl = p(0.06f, 0.54f); val fr = p(0.80f, 0.54f)
            val bl = p(0.24f, 0.40f); val br = p(0.94f, 0.40f)
            // Задний клапан откинут назад, левый опущен вниз вдоль левой стенки (он за передней стенкой).
            shape(listOf(bl, br, p(0.90f, 0.30f), p(0.28f, 0.30f)), fill = KraftFlap)
            val down = Offset(-0.06f * size.width, 0.20f * size.width)
            shape(listOf(fl, bl, bl + down, fl + down), fill = KraftFlap)
            // Тёмная внутренность коробки.
            shape(listOf(fl, bl, br, fr), fill = KraftInside)
        }
        // Кот: голова и лапки над передним краем, остальное — в коробке.
        Image(
            painterResource(R.drawable.cat_peek),
            contentDescription = null,
            modifier = Modifier
                .offset(x = w * 0.13f, y = w * 0.04f)
                .fillMaxWidth(0.66f)
                .aspectRatio(330f / 118f),
        )
        // Спереди: передняя стенка, правый бок, правый и передний клапаны, полоска скотча.
        Canvas(lift) {
            val s = size.width
            val fl = p(0.06f, 0.54f); val fr = p(0.80f, 0.54f); val br = p(0.94f, 0.40f)
            val ffl = p(0.12f, 1.02f); val ffr = p(0.76f, 1.02f); val fbr = p(0.90f, 0.84f)
            shape(listOf(fr, br, fbr, ffr), fill = KraftSide)
            shape(listOf(fl, fr, ffr, ffl), fill = Kraft)
            // Правый клапан откинут наружу.
            shape(listOf(fr, br, p(1.0f, 0.46f), p(0.88f, 0.62f)), fill = KraftFlap)
            // Передний клапан отогнут вниз и висит на передней стенке, с полоской скотча посередине.
            val flapL = p(0.075f, 0.69f); val flapR = p(0.785f, 0.69f)
            shape(listOf(fl, fr, flapR, flapL), fill = KraftFlap)
            shape(listOf(p(0.40f, 0.54f), p(0.48f, 0.54f), p(0.48f, 0.69f), p(0.40f, 0.69f)), fill = Tape)
            // Тень под клапаном — он немного отходит от стенки.
            drawLine(Color(0x33000000), p(0.09f, 0.71f), p(0.775f, 0.71f), s * 0.014f, cap = StrokeCap.Round)
        }
        // Надпись маркером на передней стенке под клапаном — картинка из макета команды, чуть наклонена.
        Image(
            painterResource(R.drawable.label_empty),
            contentDescription = "Тут пока пусто",
            modifier = Modifier
                .offset(x = w * 0.25f, y = w * 0.52f)
                .width(w * 0.38f)
                .aspectRatio(418f / 204f)
                .rotate(-3f),
        )
    }
}
