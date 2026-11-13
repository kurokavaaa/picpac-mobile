package by.picpac.studio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import by.picpac.studio.data.BookPage
import by.picpac.studio.data.CartItem
import by.picpac.studio.data.DecorType
import by.picpac.studio.data.Layouts
import by.picpac.studio.data.Decoration
import by.picpac.studio.data.Placement
import by.picpac.studio.data.Template
import by.picpac.studio.data.TemplateFont
import by.picpac.studio.ui.theme.family
import kotlin.math.roundToInt

fun Template.fontFamily(): FontFamily = when (font) {
    TemplateFont.SANS -> FontFamily.SansSerif
    TemplateFont.SERIF -> FontFamily.Serif
    TemplateFont.CURSIVE -> FontFamily.Cursive
}

/** Пропорции листа из формата «20x30» или «35x45-ru» → ширина / высота. */
fun formatRatio(formatId: String): Float {
    val m = Regex("""^(\d+(?:\.\d+)?)x(\d+(?:\.\d+)?)""").find(formatId) ?: return 1f
    val w = m.groupValues[1].toFloat()
    val h = m.groupValues[2].toFloat()
    return if (h > 0f) w / h else 1f
}

/**
 * «Без рамки» для всех ячеек и фото внутри листа. CompositionLocal — способ Compose передать значение
 * всем вложенным элементам, не протаскивая параметр через каждую функцию.
 */
val LocalFrameless = staticCompositionLocalOf { false }

/** Что можно делать с украшениями на листе. null — лист только для просмотра. */
class DecorActions(
    val selectedId: String?,
    val onSelect: (String?) -> Unit,
    val onChange: (Decoration) -> Unit,
    val onEditText: (Decoration) -> Unit,
)

/** Лист страницы или коллажа с фоном шаблона. w и h — размер листа в пикселях. */
@Composable
fun Sheet(
    template: Template,
    ratio: Float,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(w: Float, h: Float) -> Unit,
) {
    BoxWithConstraints(
        modifier
            .aspectRatio(ratio)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(template.background))
            .border(1.dp, Color(0x22000000), RoundedCornerShape(4.dp)),
    ) {
        content(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
    }
}

/**
 * Ячейки с фото по макету из библиотеки [Layouts]: каждая ячейка стоит там, где её задал макет (в долях листа).
 * onSwap != null — фото можно перетаскивать между ячейками: удержать палец на фото и тянуть.
 * onDragActive сообщает экрану, что идёт перетаскивание (чтобы страницы книги в это время не листались).
 */
@Composable
fun SlotGrid(
    layoutId: String,
    slots: List<String>,
    template: Template,
    sheetW: Float,
    sheetH: Float,
    editable: Boolean,
    selectedSlot: Int? = null,
    onSlotClick: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
    onSwap: ((from: Int, to: Int) -> Unit)? = null,
    onDragActive: ((Boolean) -> Unit)? = null,
) {
    val density = LocalDensity.current
    val cells = Layouts.byId(layoutId).cells

    // Перетаскивание: где на экране лежит каждая ячейка, какую тащим и где сейчас палец (всё в координатах экрана).
    val cellRects = remember { mutableStateMapOf<Int, Rect>() }
    var origin by remember { mutableStateOf(Offset.Zero) }
    var dragFrom by remember { mutableStateOf<Int?>(null) }
    var finger by remember { mutableStateOf(Offset.Zero) }
    // Жест живёт дольше одного кадра — внутри него берём самые свежие данные.
    val latestSlots by rememberUpdatedState(slots)
    val latestSwap by rememberUpdatedState(onSwap)
    val latestActive by rememberUpdatedState(onDragActive)
    val latestCount by rememberUpdatedState(cells.size)

    fun cellUnderFinger(): Int? =
        cellRects.entries.firstOrNull { (i, rect) -> i < latestCount && i != dragFrom && rect.contains(finger) }?.key
    val dropTarget = if (dragFrom != null) cellUnderFinger() else null

    fun stopDrag() {
        dragFrom = null
        latestActive?.invoke(false)
    }

    fun Modifier.dragHandle(i: Int): Modifier = if (onSwap == null) this else this
        .onGloballyPositioned { cellRects[i] = it.boundsInRoot() }
        .pointerInput(i) {
            detectDragGesturesAfterLongPress(
                onDragStart = { local ->
                    val rect = cellRects[i]
                    // Пустую ячейку тащить нечего.
                    if (rect != null && latestSlots.getOrElse(i) { "" }.isNotEmpty()) {
                        dragFrom = i
                        finger = rect.topLeft + local
                        latestActive?.invoke(true)
                    }
                },
                onDrag = { change, amount ->
                    if (dragFrom != null) {
                        change.consume()
                        finger += amount
                    }
                },
                onDragEnd = {
                    val from = dragFrom
                    val to = cellUnderFinger()
                    if (from != null && to != null) latestSwap?.invoke(from, to)
                    stopDrag()
                },
                onDragCancel = { stopDrag() },
            )
        }

    Box(modifier.fillMaxSize().onGloballyPositioned { origin = it.positionInRoot() }) {
        cells.forEachIndexed { i, cell ->
            SlotCell(
                uri = slots.getOrElse(i) { "" },
                template = template,
                sheetW = sheetW,
                editable = editable,
                selected = i == selectedSlot || i == dropTarget,
                dimmed = i == dragFrom,
                onClick = onSlotClick?.let { cb -> { cb(i) } },
                modifier = Modifier
                    .offset { IntOffset(((cell.x - cell.w / 2) * sheetW).roundToInt(), ((cell.y - cell.h / 2) * sheetH).roundToInt()) }
                    .size(with(density) { (cell.w * sheetW).toDp() }, with(density) { (cell.h * sheetH).toDp() })
                    .dragHandle(i),
            )
        }

        // Миниатюра, которая едет за пальцем.
        val from = dragFrom
        val rect = from?.let { cellRects[it] }
        if (from != null && rect != null) {
            val w = rect.width * 0.7f
            val h = rect.height * 0.7f
            Box(
                Modifier
                    .offset { IntOffset((finger.x - origin.x - w / 2).roundToInt(), (finger.y - origin.y - h / 2).roundToInt()) }
                    .size(with(density) { w.toDp() }, with(density) { h.toDp() })
                    .graphicsLayer { alpha = 0.85f; shadowElevation = 16f }
                    .border(2.dp, MaterialTheme.colorScheme.primary),
            ) {
                PhotoThumb(slots.getOrElse(from) { "" }, Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun SlotCell(
    uri: String,
    template: Template,
    sheetW: Float,
    editable: Boolean,
    selected: Boolean,
    dimmed: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier,
) {
    val density = LocalDensity.current
    val frameless = LocalFrameless.current
    val shape = if (template.rounded && !frameless) RoundedCornerShape(10) else RectangleShape
    Box(
        modifier
            .graphicsLayer { alpha = if (dimmed) 0.35f else 1f }
            .clip(shape)
            .let { if (frameless) it else it.background(Color(template.frame)) }
            .let { if (selected) it.border(3.dp, MaterialTheme.colorScheme.primary, shape) else it }
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(if (frameless) 0.dp else with(density) { (sheetW * template.frameWidth).toDp() }),
        contentAlignment = Alignment.Center,
    ) {
        if (uri.isEmpty()) {
            Box(
                Modifier.fillMaxSize().background(Color(template.accent).copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center,
            ) {
                if (editable) Icon(Icons.Filled.Add, contentDescription = "Добавить фото", tint = Color(template.accent))
            }
        } else {
            PhotoThumb(uri, Modifier.fillMaxSize())
        }
    }
}

/**
 * Страница фотокниги: фото (в сетке или свободно) + тексты и стикеры поверх.
 * Все обработчики == null — режим просмотра (предпросмотр, карточка товара).
 */
@Composable
fun BookPageView(
    page: BookPage,
    template: Template,
    ratio: Float,
    modifier: Modifier = Modifier,
    selectedSlot: Int? = null,
    onSlotClick: ((Int) -> Unit)? = null,
    decor: DecorActions? = null,
    onSwap: ((from: Int, to: Int) -> Unit)? = null,
    onDragActive: ((Boolean) -> Unit)? = null,
    selectedPhoto: String? = null,
    onPhotoSelect: ((String) -> Unit)? = null,
    onPlacementChange: ((String, Placement) -> Unit)? = null,
    onPhotoRemove: ((String) -> Unit)? = null,
    frameless: Boolean = false,
) = CompositionLocalProvider(LocalFrameless provides frameless) {
    val density = LocalDensity.current
    Sheet(template, ratio, modifier) { w, h ->
        if (page.free) {
            // Тот же механизм, что у свободного коллажа: двигать пальцем, размер — щипком или за уголок.
            FreePhotos(page.photos, page.placements, w, h, selectedPhoto, onPhotoSelect, onPlacementChange, onPhotoRemove)
        } else {
            SlotGrid(
                page.layoutId, page.slots, template, w, h, editable = onSlotClick != null, selectedSlot, onSlotClick,
                onSwap = onSwap, onDragActive = onDragActive,
            )
        }
        DecorationLayer(page.decorations, w, h, template, decor)
    }
}

/** Обложка: первое фото и название на плашке акцентного цвета шаблона. */
@Composable
fun CoverView(title: String, photo: String?, template: Template, ratio: Float, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    Sheet(template, ratio, modifier) { w, _ ->
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth().padding(with(density) { (w * 0.06f).toDp() })) {
                if (photo != null) {
                    PhotoThumb(photo, Modifier.fillMaxSize())
                } else {
                    Box(Modifier.fillMaxSize().background(Color(template.accent).copy(alpha = 0.1f)))
                }
            }
            Box(
                Modifier.fillMaxWidth().background(Color(template.accent)).padding(vertical = with(density) { (w * 0.04f).toDp() }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    title.ifBlank { "Название книги" },
                    color = Color.White,
                    fontFamily = template.fontFamily(),
                    fontStyle = if (template.font == TemplateFont.SERIF) FontStyle.Italic else FontStyle.Normal,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = with(density) { (w * 0.065f).toSp() },
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }
    }
}

/**
 * Коллаж: сетка или свободная раскладка + украшения.
 * Обработчики == null — режим просмотра (предпросмотр, экран товара).
 */
@Composable
fun CollageView(
    item: CartItem,
    ratio: Float,
    modifier: Modifier = Modifier,
    selectedSlot: Int? = null,
    onSlotClick: ((Int) -> Unit)? = null,
    selectedPhoto: String? = null,
    onPhotoSelect: ((String) -> Unit)? = null,
    onPlacementChange: ((String, Placement) -> Unit)? = null,
    onPhotoRemove: ((String) -> Unit)? = null,
    decor: DecorActions? = null,
) = CompositionLocalProvider(LocalFrameless provides item.noFrame) {
    val density = LocalDensity.current
    Sheet(item.template, ratio, modifier) { w, h ->
        if (item.freeLayout) {
            FreePhotos(item.photos, item.placements, w, h, selectedPhoto, onPhotoSelect, onPlacementChange, onPhotoRemove)
        } else {
            SlotGrid(item.layoutId, List(item.layout) { item.photos.getOrElse(it) { "" } }, item.template, w, h, onSlotClick != null, selectedSlot, onSlotClick)
        }
        DecorationLayer(item.decorations, w, h, item.template, decor)
    }
}

/** Самое маленькое фото на листе — 15% его стороны, чтобы его ещё можно было ухватить пальцем. */
private const val MIN_PHOTO_SIZE = 0.15f

/** Свободная раскладка: фото таскаются пальцем, размер — щипком или за уголок. */
@Composable
private fun FreePhotos(
    photos: List<String>,
    placements: List<Placement>,
    w: Float,
    h: Float,
    selected: String?,
    onSelect: ((String) -> Unit)?,
    onChange: ((String, Placement) -> Unit)?,
    onRemove: ((String) -> Unit)?,
) {
    val density = LocalDensity.current
    // Жесты живут дольше одного кадра — берём из них всегда свежие позиции, а не те, что были при старте.
    val latestPhotos by rememberUpdatedState(photos)
    val latestPlacements by rememberUpdatedState(placements)
    fun current(uri: String): Placement {
        val i = latestPhotos.indexOf(uri)
        return latestPlacements.getOrNull(i) ?: Placement.default(maxOf(i, 0))
    }

    photos.forEachIndexed { index, uri ->
        key(uri) {
            val p = placements.getOrNull(index) ?: Placement.default(index)
            val isSelected = onChange != null && uri == selected
            Box(
                Modifier
                    .offset { IntOffset(((p.x - p.w / 2) * w).roundToInt(), ((p.y - p.h / 2) * h).roundToInt()) }
                    .size(with(density) { (p.w * w).toDp() }, with(density) { (p.h * h).toDp() })
                    .let { m ->
                        when {
                            isSelected -> m.border(2.dp, MaterialTheme.colorScheme.primary)
                            LocalFrameless.current -> m
                            else -> m.border(1.dp, Color.White)
                        }
                    }
                    .let { m ->
                        if (onChange == null) m else m
                            .pointerInput(uri) { detectTapGestures(onPress = { onSelect?.invoke(uri) }) }
                            .pointerInput(uri, w, h) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val cur = current(uri)
                                    onChange(
                                        uri,
                                        cur.copy(
                                            x = (cur.x + pan.x / w).coerceIn(0f, 1f),
                                            y = (cur.y + pan.y / h).coerceIn(0f, 1f),
                                            w = (cur.w * zoom).coerceIn(MIN_PHOTO_SIZE, 1f),
                                            h = (cur.h * zoom).coerceIn(MIN_PHOTO_SIZE, 1f),
                                        ),
                                    )
                                }
                            }
                    },
            ) {
                PhotoThumb(uri, Modifier.fillMaxSize())
                if (isSelected) {
                    if (onRemove != null) {
                        Box(
                            Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp).clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.9f)).clickable { onRemove(uri) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Убрать фото", Modifier.size(14.dp), tint = Color.Black)
                        }
                    }
                    // Уголок для изменения размера одним пальцем или мышкой (в эмуляторе щипок не сделать).
                    Box(
                        Modifier.align(Alignment.BottomEnd).padding(3.dp).size(26.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary).border(2.dp, Color.White, CircleShape)
                            .pointerInput(uri, w, h) {
                                detectDragGestures { change, drag ->
                                    change.consume()
                                    val cur = current(uri)
                                    val nw = (cur.w + drag.x / w).coerceIn(MIN_PHOTO_SIZE, 1f)
                                    val nh = (cur.h + drag.y / h).coerceIn(MIN_PHOTO_SIZE, 1f)
                                    // Левый верхний угол остаётся на месте — фото растёт в сторону пальца.
                                    onChange(uri, cur.copy(x = cur.x + (nw - cur.w) / 2, y = cur.y + (nh - cur.h) / 2, w = nw, h = nh))
                                }
                            },
                    )
                }
            }
        }
    }
}

/** Тексты и стикеры поверх листа. Перетаскивание пальцем, размер — щипком, двойное касание по тексту — правка. */
@Composable
fun DecorationLayer(decorations: List<Decoration>, w: Float, h: Float, template: Template, actions: DecorActions?) {
    val density = LocalDensity.current
    val latest by rememberUpdatedState(decorations)
    // Жест создаётся один раз, а обработчики меняются при перерисовке — внутри жеста берём самые свежие.
    val latestActions by rememberUpdatedState(actions)
    fun current(id: String) = latest.firstOrNull { it.id == id }

    decorations.forEach { d ->
        key(d.id) {
            val selected = actions != null && d.id == actions.selectedId
            Box(
                Modifier
                    .centerAt(d.x * w, d.y * h)
                    .let { m ->
                        if (actions == null) m else m
                            .pointerInput(d.id) {
                                detectTapGestures(
                                    onPress = { latestActions?.onSelect?.invoke(d.id) },
                                    onDoubleTap = { current(d.id)?.takeIf { it.type == DecorType.TEXT }?.let { latestActions?.onEditText?.invoke(it) } },
                                )
                            }
                            .pointerInput(d.id, w, h) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val cur = current(d.id) ?: return@detectTransformGestures
                                    latestActions?.onChange?.invoke(
                                        cur.copy(
                                            x = (cur.x + pan.x / w).coerceIn(0f, 1f),
                                            y = (cur.y + pan.y / h).coerceIn(0f, 1f),
                                            size = (cur.size * zoom).coerceIn(0.03f, 0.9f),
                                        ),
                                    )
                                }
                            }
                    }
                    .let { if (selected) it.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)) else it }
                    .padding(4.dp),
            ) {
                when (d.type) {
                    DecorType.TEXT -> Text(
                        d.text,
                        color = Color(d.color),
                        // Свой шрифт у надписи важнее шрифта шаблона.
                        fontFamily = d.font?.family() ?: template.fontFamily(),
                        fontStyle = if (d.font == null && template.font == TemplateFont.SERIF) FontStyle.Italic else FontStyle.Normal,
                        // У выбранных шрифтов один файл без жирного начертания — не подделываем жирность.
                        fontWeight = if (d.font == null) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = with(density) { (d.size * w).toSp() },
                        textAlign = TextAlign.Center,
                    )
                    DecorType.STICKER -> StickerView(
                        d.sticker,
                        Modifier.size(with(density) { (d.size * w).toDp() }),
                        fontSize = with(density) { (d.size * w * 0.55f).toSp() },
                    )
                }
            }
        }
    }
}

/** Разместить элемент так, чтобы его центр оказался в точке (cx, cy) родителя. */
private fun Modifier.centerAt(cx: Float, cy: Float) = layout { measurable, constraints ->
    val p = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
    layout(p.width, p.height) { p.place((cx - p.width / 2f).roundToInt(), (cy - p.height / 2f).roundToInt()) }
}
