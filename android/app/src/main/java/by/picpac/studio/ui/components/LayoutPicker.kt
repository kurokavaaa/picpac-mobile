package by.picpac.studio.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import by.picpac.studio.data.Layouts
import by.picpac.studio.data.PageLayout

/** Миниатюра макета: белый лист и серые ячейки — как превью на сайте PicPac. */
@Composable
fun LayoutThumb(layout: PageLayout, ratio: Float, selected: Boolean, width: Dp, onClick: (() -> Unit)? = null) {
    val shape = RoundedCornerShape(4.dp)
    Canvas(
        Modifier
            .width(width)
            .aspectRatio(ratio)
            .clip(shape)
            .background(Color.White)
            .border(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else Color(0xFF9A9A9A), shape)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
    ) {
        layout.cells.forEach { c ->
            drawRect(
                Color(0xFFCDCDCD),
                topLeft = Offset((c.x - c.w / 2) * size.width, (c.y - c.h / 2) * size.height),
                size = Size(c.w * size.width, c.h * size.height),
            )
        }
    }
}

/**
 * Выбор макета страницы, как на сайте: сначала «Количество фото», под ним все макеты на столько фото.
 * maxPhotos ограничивает число фото (например, сколько их всего в коллаже).
 */
@Composable
fun LayoutPicker(selectedId: String?, ratio: Float, onPick: (PageLayout) -> Unit, modifier: Modifier = Modifier) {
    val selected = selectedId?.let { Layouts.byId(it) }
    var count by remember(selectedId) { mutableIntStateOf(selected?.count ?: 1) }
    Column(modifier) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (1..Layouts.MAX_PHOTOS).forEach { n ->
                SelectChip("$n фото", n == count, { count = n })
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Layouts.byCount(count).forEach { layout ->
                LayoutThumb(layout, ratio, layout.id == selectedId, 60.dp) { onPick(layout) }
            }
        }
    }
}
