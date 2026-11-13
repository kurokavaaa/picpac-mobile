package by.picpac.studio.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import by.picpac.studio.ui.theme.PicPacColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Котик бежит за своим хвостом, вид сверху. Тело изогнуто буквой «С»: голова впереди,
 * кончик хвоста убегает от неё по кругу, между ними зазор. Вся фигура вращается
 * по часовой стрелке, лапки перебирают по очереди.
 */
@Composable
fun ChasingCat(modifier: Modifier = Modifier, size: Dp = 150.dp) {
    val transition = rememberInfiniteTransition(label = "chase")
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing)),
        label = "spin",
    )
    // Один шаг бега: передние и задние лапы ходят в противофазе, тело в такт тянется и сжимается.
    val step by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing)),
        label = "step",
    )
    // Хвост виляет медленнее шагов — так движение не выглядит механическим.
    val wag by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "wag",
    )

    Canvas(modifier.size(size)) {
        val c = center
        val ring = this.size.minDimension * 0.30f
        val body = ring * 0.5f
        val outline = body * 0.08f
        val arcTopLeft = c - Offset(ring, ring)
        val arcSize = Size(ring * 2, ring * 2)

        // Углы по кругу (по часовой = вперёд): голова около 0°, тело назад до бёдер ≈ −205°,
        // хвост дальше ≈ до −262°. Кончик хвоста ~на 98° впереди головы — кольцо не замкнуто.
        val stretch = sin(step) * 6f                // тело чуть вытягивается и сжимается на каждом шаге
        val hip = -205f + stretch
        val headAngle = sin(step + 0.8f) * 1.5f     // голова слегка «кивает» вперёд-назад
        val tailStart = hip - 57f + sin(wag) * 8f   // хвост виляет

        rotate(spin, c) {
            // Хвост: тонкий, с тёмной обводкой и полосками.
            drawArc(
                PicPacColors.CatLine, startAngle = tailStart, sweepAngle = hip + 3f - tailStart, useCenter = false,
                topLeft = arcTopLeft, size = arcSize, style = Stroke(width = body * 0.34f + outline * 2, cap = StrokeCap.Round),
            )
            drawArc(
                PicPacColors.CatFur, startAngle = tailStart, sweepAngle = hip + 3f - tailStart, useCenter = false,
                topLeft = arcTopLeft, size = arcSize, style = Stroke(width = body * 0.34f, cap = StrokeCap.Round),
            )
            listOf(0.15f, 0.4f, 0.65f).forEach { t ->
                val r = Math.toRadians((tailStart + (hip - tailStart) * t).toDouble())
                drawLine(
                    PicPacColors.CatStripe, c + polar(r, ring - body * 0.15f), c + polar(r, ring + body * 0.15f),
                    strokeWidth = body * 0.08f, cap = StrokeCap.Round,
                )
            }

            // Лапы сверху: выносятся вперёд и назад вдоль тела (по ходу бега), а не в стороны.
            // Передняя пара у плеч, задняя у бёдер; пары в противофазе, левая и правая чуть сдвинуты по фазе.
            data class Leg(val angle: Float, val phase: Float, val side: Float)
            val legs = listOf(
                Leg(-26f, 0f, 1f), Leg(-26f, 0.7f, -1f),
                Leg(hip + 22f, PI.toFloat(), 1f), Leg(hip + 22f, PI.toFloat() + 0.7f, -1f),
            )
            legs.forEach { leg ->
                val a = Math.toRadians(leg.angle.toDouble())
                val radial = Offset(cos(a).toFloat(), sin(a).toFloat())
                val forward = Offset(-sin(a).toFloat(), cos(a).toFloat())
                val swing = sin(step + leg.phase) * body * 0.9f
                val root = c + radial * (ring + leg.side * body * 0.3f)
                val tip = root + forward * swing + radial * (leg.side * body * 0.2f)
                drawLine(PicPacColors.CatStripe, root, tip, strokeWidth = body * 0.24f, cap = StrokeCap.Round)
                drawCircle(PicPacColors.CatCream, radius = body * 0.15f, center = tip)
                drawCircle(PicPacColors.CatLine, radius = body * 0.15f, center = tip, style = Stroke(outline * 0.8f))
            }

            // Тело: толстая дуга с обводкой, на спине поперечные полоски.
            val bodySweep = (headAngle - 8f) - hip
            drawArc(
                PicPacColors.CatLine, startAngle = hip, sweepAngle = bodySweep, useCenter = false,
                topLeft = arcTopLeft, size = arcSize, style = Stroke(width = body + outline * 2, cap = StrokeCap.Round),
            )
            drawArc(
                PicPacColors.CatFur, startAngle = hip, sweepAngle = bodySweep, useCenter = false,
                topLeft = arcTopLeft, size = arcSize, style = Stroke(width = body, cap = StrokeCap.Round),
            )
            listOf(0.1f, 0.28f, 0.46f, 0.64f, 0.82f).forEach { t ->
                val r = Math.toRadians((hip + bodySweep * t).toDouble())
                drawLine(
                    PicPacColors.CatStripe, c + polar(r, ring - body * 0.3f), c + polar(r, ring + body * 0.3f),
                    strokeWidth = body * 0.12f, cap = StrokeCap.Round,
                )
            }

            // Голова на переднем конце, носом по ходу движения (по касательной к кругу).
            val headCenter = c + polar(Math.toRadians(headAngle.toDouble()), ring)
            rotate(headAngle + 180f, headCenter) {
                drawCatHeadTop(headCenter, body * 0.62f)
            }
        }
    }
}

/** Голова котика сверху: ушки и мордочка смотрят вверх (по −y), полоски на макушке. */
private fun DrawScope.drawCatHeadTop(c: Offset, r: Float) {
    val outline = Stroke(width = r * 0.1f)
    for (side in listOf(-1f, 1f)) {
        val ear = Path().apply {
            moveTo(c.x + side * r * 0.85f, c.y - r * 0.15f)
            lineTo(c.x + side * r * 0.62f, c.y - r * 1.05f)
            lineTo(c.x + side * r * 0.12f, c.y - r * 0.7f)
            close()
        }
        drawPath(ear, PicPacColors.CatFur)
        drawPath(ear, PicPacColors.CatLine, style = outline)
        val inner = Path().apply {
            moveTo(c.x + side * r * 0.68f, c.y - r * 0.32f)
            lineTo(c.x + side * r * 0.58f, c.y - r * 0.85f)
            lineTo(c.x + side * r * 0.3f, c.y - r * 0.65f)
            close()
        }
        drawPath(inner, PicPacColors.CatPink)
    }
    // Мордочка чуть выступает вперёд.
    drawOval(PicPacColors.CatCream, c + Offset(-r * 0.42f, -r * 1.08f), Size(r * 0.84f, r * 0.6f))
    drawOval(PicPacColors.CatLine, c + Offset(-r * 0.42f, -r * 1.08f), Size(r * 0.84f, r * 0.6f), style = outline)
    drawCircle(PicPacColors.CatPink, radius = r * 0.12f, center = c + Offset(0f, -r * 1.02f))

    drawCircle(PicPacColors.CatFur, radius = r, center = c)
    drawCircle(PicPacColors.CatLine, radius = r, center = c, style = outline)
    listOf(-0.35f, 0f, 0.35f).forEach { dx ->
        drawLine(
            PicPacColors.CatStripe, Offset(c.x + r * dx, c.y + r * 0.45f), Offset(c.x + r * dx, c.y - r * 0.35f),
            strokeWidth = r * 0.16f, cap = StrokeCap.Round,
        )
    }
}

/** Котик выглядывает из-за края и держится лапками — для баннеров, «Спасибо» и пустых экранов. */
@Composable
fun CatPeek(modifier: Modifier = Modifier, size: Dp = 120.dp) {
    Canvas(modifier.size(size, size * 0.72f)) {
        val w = this.size.width
        val h = this.size.height
        val r = h * 0.48f
        val head = Offset(w / 2, h * 0.62f)

        // «Искорки» радости слева сверху.
        listOf(-150.0, -125.0, -100.0).forEach { deg ->
            val a = Math.toRadians(deg)
            val start = head + polar(a, r * 1.45f) + Offset(-r * 0.35f, 0f)
            val end = head + polar(a, r * 1.8f) + Offset(-r * 0.35f, 0f)
            drawLine(PicPacColors.Terracotta, start, end, strokeWidth = r * 0.09f, cap = StrokeCap.Round)
        }

        drawCatHead(head, r, eyesOpen = false)

        // Лапки поверх края.
        listOf(-0.62f, 0.62f).forEach { dx ->
            val paw = Offset(head.x + r * dx, h - r * 0.18f)
            drawOval(PicPacColors.CatCream, topLeft = paw - Offset(r * 0.3f, r * 0.2f), size = Size(r * 0.6f, r * 0.4f))
            drawOval(
                PicPacColors.CatLine, topLeft = paw - Offset(r * 0.3f, r * 0.2f), size = Size(r * 0.6f, r * 0.4f),
                style = Stroke(width = r * 0.05f),
            )
        }
    }
}

private fun polar(angleRad: Double, radius: Float) =
    Offset((cos(angleRad) * radius).toFloat(), (sin(angleRad) * radius).toFloat())

/** Мордочка котика для стикера (с довольными закрытыми глазами). */
internal fun DrawScope.drawCatFace(c: Offset, r: Float) = drawCatHead(c, r, eyesOpen = false)

/** Голова котика с центром в [c] и «радиусом» [r]. Ушки смотрят вверх (по −y). */
private fun DrawScope.drawCatHead(c: Offset, r: Float, eyesOpen: Boolean) {
    val outline = Stroke(width = r * 0.06f)

    // Ушки.
    for (side in listOf(-1f, 1f)) {
        val ear = Path().apply {
            moveTo(c.x + side * r * 0.95f, c.y - r * 0.25f)
            lineTo(c.x + side * r * 0.78f, c.y - r * 1.2f)
            lineTo(c.x + side * r * 0.2f, c.y - r * 0.82f)
            close()
        }
        drawPath(ear, PicPacColors.CatFur)
        drawPath(ear, PicPacColors.CatLine, style = outline)
        val inner = Path().apply {
            moveTo(c.x + side * r * 0.82f, c.y - r * 0.45f)
            lineTo(c.x + side * r * 0.74f, c.y - r * 0.98f)
            lineTo(c.x + side * r * 0.38f, c.y - r * 0.78f)
            close()
        }
        drawPath(inner, PicPacColors.CatPink)
    }

    // Голова и светлая мордочка.
    val headTopLeft = c - Offset(r * 1.08f, r * 0.92f)
    val headSize = Size(r * 2.16f, r * 1.84f)
    drawOval(PicPacColors.CatFur, headTopLeft, headSize)
    drawOval(PicPacColors.CatCream, c + Offset(-r * 0.55f, r * 0.05f), Size(r * 1.1f, r * 0.75f))
    drawOval(PicPacColors.CatLine, headTopLeft, headSize, style = outline)

    // Полоски на лбу.
    listOf(-0.25f, 0f, 0.25f).forEach { dx ->
        drawLine(
            PicPacColors.CatStripe,
            Offset(c.x + r * dx, c.y - r * 0.85f), Offset(c.x + r * dx, c.y - r * 0.55f),
            strokeWidth = r * 0.1f, cap = StrokeCap.Round,
        )
    }

    // Глаза: открытые точки или довольные дуги.
    for (side in listOf(-1f, 1f)) {
        val eye = Offset(c.x + side * r * 0.45f, c.y - r * 0.08f)
        if (eyesOpen) {
            drawCircle(PicPacColors.CatLine, radius = r * 0.11f, center = eye)
            drawCircle(PicPacColors.CatCream, radius = r * 0.035f, center = eye + Offset(r * 0.03f, -r * 0.03f))
        } else {
            drawArc(
                PicPacColors.CatLine, startAngle = 200f, sweepAngle = 140f, useCenter = false,
                topLeft = eye - Offset(r * 0.18f, r * 0.12f), size = Size(r * 0.36f, r * 0.3f),
                style = Stroke(width = r * 0.07f, cap = StrokeCap.Round),
            )
        }
    }

    // Носик и ротик «ω».
    val nose = Offset(c.x, c.y + r * 0.2f)
    drawCircle(PicPacColors.CatPink, radius = r * 0.08f, center = nose)
    for (side in listOf(-1f, 1f)) {
        drawArc(
            PicPacColors.CatLine, startAngle = 20f, sweepAngle = 140f, useCenter = false,
            topLeft = Offset(nose.x + (if (side < 0) -r * 0.22f else 0f), nose.y), size = Size(r * 0.22f, r * 0.18f),
            style = Stroke(width = r * 0.05f, cap = StrokeCap.Round),
        )
    }
}
