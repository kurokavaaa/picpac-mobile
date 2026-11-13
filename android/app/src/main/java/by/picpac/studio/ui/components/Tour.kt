package by.picpac.studio.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import by.picpac.studio.data.Placement
import kotlin.math.roundToInt

/** Шаг обучения: какой элемент подсветить (key у Modifier.tourTarget) и что про него сказать. */
data class TourStep(val key: String, val title: String, val text: String)

val TourSteps = listOf(
    TourStep("hero-catalog", "Начнём с каталога", "Здесь все товары: фотопечать, фото на документы, альбомы и фотокниги."),
    TourStep("product-album", "Выберите товар", "Например, фотоальбом: его можно собрать самому или взять готовый макет."),
    TourStep("album-mode", "Как собрать альбом", "«Сам» — раскладываете фото по страницам. «Готовый макет» — страницы уже размечены, остаётся добавить фото."),
    TourStep("add-photos", "Добавьте фото", "Откроется галерея телефона — отметьте только нужные снимки, остальные приложение не увидит."),
    TourStep("to-cart", "В корзину", "Цена пересчитывается сразу. Оформить заказ можно после входа в аккаунт."),
)

/** Где на экране лежат элементы, которые обучение умеет подсвечивать. */
class TourRegistry {
    val rects = mutableStateMapOf<String, Rect>()
    var onTap: (String) -> Unit = {}
}

val LocalTour = staticCompositionLocalOf<TourRegistry?> { null }

/**
 * Пометить элемент для обучения: запоминаем его место на экране и слышим нажатие на него
 * (без перехвата — сам элемент нажимается как обычно, а обучение переходит к следующему шагу).
 */
fun Modifier.tourTarget(key: String): Modifier = composed {
    val tour = LocalTour.current ?: return@composed this
    DisposableEffect(key) { onDispose { tour.rects.remove(key) } }
    this
        .onGloballyPositioned { tour.rects[key] = it.boundsInRoot() }
        .pointerInput(key) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                if (waitForUpOrCancellation(PointerEventPass.Initial) != null) tour.onTap(key)
            }
        }
}

/** Прозрачный слой, который глотает все касания: под ним ничего не нажимается. */
private fun Modifier.blockTouches() = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) awaitPointerEvent().changes.forEach { it.consume() }
    }
}

/**
 * Затемнение всего экрана с «окном» вокруг нужного элемента. Касания проходят только в окно:
 * вокруг лежат четыре блокирующие полосы. Если элемента сейчас нет на экране — слой не показываем.
 */
@Composable
fun TourOverlay(registry: TourRegistry, index: Int, onNext: () -> Unit, onSkip: () -> Unit) {
    val step = TourSteps.getOrNull(index) ?: return
    val target = registry.rects[step.key]
    if (target == null) {
        // Нужного элемента нет на этом экране (например, пользователь ушёл в сторону) —
        // подсказку и кнопки всё равно показываем, чтобы обучение не «пропадало».
        Box(Modifier.fillMaxSize().background(Color(0xFF0E1615).copy(alpha = 0.72f)).blockTouches(), contentAlignment = Alignment.BottomCenter) {
            TourCard(step, index, onNext, onSkip, Modifier.navigationBarsPadding().padding(bottom = 24.dp))
        }
        return
    }
    val density = LocalDensity.current
    var origin by remember { mutableStateOf(Offset.Zero) }
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart), label = "pulse",
    )

    BoxWithConstraints(Modifier.fillMaxSize().onGloballyPositioned { origin = it.positionInRoot() }) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val pad = with(density) { 6.dp.toPx() }
        val hole = Rect(target.left - origin.x - pad, target.top - origin.y - pad, target.right - origin.x + pad, target.bottom - origin.y + pad)
        val radius = with(density) { 12.dp.toPx() }

        // Offscreen-слой нужен, чтобы BlendMode.Clear «вырезал» окно в затемнении, а не в том, что под ним.
        Canvas(Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
            drawRect(Color(0xFF0E1615).copy(alpha = 0.72f))
            drawRoundRect(Color.Transparent, hole.topLeft, hole.size, CornerRadius(radius), blendMode = BlendMode.Clear)
            val grow = pulse * 10.dp.toPx()
            drawRoundRect(
                Color(0xFFDD5757).copy(alpha = 1f - pulse),
                Offset(hole.left - grow, hole.top - grow), Size(hole.width + grow * 2, hole.height + grow * 2),
                CornerRadius(radius + grow), style = Stroke(2.dp.toPx()),
            )
        }

        @Composable
        fun blocker(x: Float, y: Float, bw: Float, bh: Float) {
            if (bw <= 0f || bh <= 0f) return
            Box(
                Modifier
                    .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                    .size(with(density) { bw.toDp() }, with(density) { bh.toDp() })
                    .blockTouches(),
            )
        }
        blocker(0f, 0f, w, hole.top)
        blocker(0f, hole.bottom, w, h - hole.bottom)
        blocker(0f, hole.top, hole.left, hole.height)
        blocker(hole.right, hole.top, w - hole.right, hole.height)

        // Подсказка — под окном, если окно в верхней половине, иначе над ним.
        val below = hole.center.y < h / 2
        Box(
            if (below) Modifier.offset { IntOffset(0, (hole.bottom + 14.dp.toPx()).roundToInt()) }
            else Modifier.fillMaxSize().padding(bottom = with(density) { (h - hole.top + 14.dp.toPx()).toDp() }),
            contentAlignment = Alignment.BottomCenter,
        ) {
            TourCard(step, index, onNext, onSkip)
        }
    }
}

/** Карточка подсказки: шаг, текст и кнопки «Пропустить» / «Далее» («Готово» на последнем шаге). */
@Composable
private fun TourCard(step: TourStep, index: Int, onNext: () -> Unit, onSkip: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        Text("ШАГ ${index + 1} ИЗ ${TourSteps.size}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(4.dp))
        Text(step.title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(step.text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onSkip) { Text("Пропустить обучение") }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onNext) { Text(if (index == TourSteps.lastIndex) "Готово" else "Далее") }
        }
    }
}

/**
 * Показ жеста «поменять фото местами»: палец удерживает фото ~0,5 с, фото отрывается
 * и едет в соседнюю ячейку. pageRect — где лежит страница (координаты экрана), cells — ячейки её макета.
 */
@Composable
fun DragHint(pageRect: Rect, cells: List<Placement>, onDone: () -> Unit) {
    var origin by remember { mutableStateOf(Offset.Zero) }
    // Если на странице одна ячейка — показываем жест на условных половинках страницы.
    val pair = if (cells.size >= 2) cells.take(2) else listOf(Placement(0.5f, 0.27f, 0.88f, 0.42f), Placement(0.5f, 0.73f, 0.88f, 0.42f))
    val t by rememberInfiniteTransition(label = "drag").animateFloat(
        0f, 1f, infiniteRepeatable(tween(3200, easing = LinearEasing)), label = "drag",
    )

    Box(Modifier.fillMaxSize().onGloballyPositioned { origin = it.positionInRoot() }.blockTouches()) {
        val page = Rect(pageRect.left - origin.x, pageRect.top - origin.y, pageRect.right - origin.x, pageRect.bottom - origin.y)
        fun cellRect(c: Placement) = Rect(
            page.left + (c.x - c.w / 2) * page.width, page.top + (c.y - c.h / 2) * page.height,
            page.left + (c.x + c.w / 2) * page.width, page.top + (c.y + c.h / 2) * page.height,
        )
        val from = cellRect(pair[0])
        val to = cellRect(pair[1])

        Canvas(Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
            drawRect(Color(0xFF0E1615).copy(alpha = 0.6f))
            drawRoundRect(Color.Transparent, page.topLeft, page.size, CornerRadius(6.dp.toPx()), blendMode = BlendMode.Clear)

            // Фазы: 0–0,1 палец опускается; 0,1–0,3 удержание (кольцо заполняется); 0,3–0,7 перенос; 0,7–1 отпустили и пауза.
            val hold = ((t - 0.1f) / 0.2f).coerceIn(0f, 1f)
            val move = FastOutSlowInEasing.transform(((t - 0.3f) / 0.4f).coerceIn(0f, 1f))
            val finger = from.center + (to.center - from.center) * move
            val lifted = t in 0.3f..0.72f

            if (t > 0.3f) {
                // Ячейка-цель подсвечивается, когда палец над ней.
                drawRoundRect(Color(0xFFDD5757), to.topLeft, to.size, CornerRadius(4.dp.toPx()), style = Stroke(3.dp.toPx()), alpha = move)
            }
            if (lifted) {
                val gw = from.width * 0.7f
                val gh = from.height * 0.7f
                drawRect(Color.White.copy(alpha = 0.5f), Offset(finger.x - gw / 2, finger.y - gh / 2), Size(gw, gh))
                drawRect(Color(0xFFDD5757), Offset(finger.x - gw / 2, finger.y - gh / 2), Size(gw, gh), style = Stroke(2.dp.toPx()))
            }
            if (t < 0.8f) {
                val r = 22.dp.toPx()
                drawCircle(Color.White.copy(alpha = 0.92f), r, finger)
                drawCircle(Color(0xFF364C49), r, finger, style = Stroke(2.dp.toPx()))
                if (t in 0.1f..0.3f) {
                    val ring = r + 6.dp.toPx()
                    drawArc(
                        Color(0xFFDD5757), -90f, 360f * hold, false,
                        Offset(finger.x - ring, finger.y - ring), Size(ring * 2, ring * 2), style = Stroke(4.dp.toPx()),
                    )
                }
            }
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp)
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
        ) {
            Text("Как поменять фото местами", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            listOf(
                "Прижмите палец к фото и подержите полсекунды — оно станет полупрозрачным.",
                "Не отпуская, ведите в другую ячейку — она подсветится красным.",
                "Отпустите — фото поменяются местами.",
            ).forEachIndexed { i, line ->
                Row(Modifier.padding(vertical = 3.dp)) {
                    Text("${i + 1}", Modifier.width(20.dp), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Text(line, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(6.dp))
            Row {
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDone) { Text("Понятно") }
            }
        }
    }
}
