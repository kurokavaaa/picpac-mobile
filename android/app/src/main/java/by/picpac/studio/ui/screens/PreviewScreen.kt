package by.picpac.studio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import by.picpac.studio.data.AppViewModel
import by.picpac.studio.data.BookPage
import by.picpac.studio.data.Books
import by.picpac.studio.data.CartItem
import by.picpac.studio.data.Catalog
import by.picpac.studio.data.Pricing
import by.picpac.studio.data.ProductKind
import by.picpac.studio.ui.Screen
import by.picpac.studio.ui.components.BookPageView
import by.picpac.studio.ui.components.CollageView
import by.picpac.studio.ui.components.CoverView
import by.picpac.studio.ui.components.NoteBox
import by.picpac.studio.ui.components.PhotoThumb
import by.picpac.studio.ui.components.PrimaryButton
import by.picpac.studio.ui.components.ScreenHeader
import by.picpac.studio.ui.components.SecondaryButton
import by.picpac.studio.ui.components.formatMoney
import by.picpac.studio.ui.components.formatRatio
import by.picpac.studio.ui.components.DocSheet
import kotlin.math.abs

/** Предпросмотр готового товара перед корзиной: книга листается, коллаж и печать — как будут выглядеть. */
@Composable
fun PreviewScreen(vm: AppViewModel) {
    val draft = vm.state.draft
    if (draft == null) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader("Предпросмотр", onBack = { vm.nav.back() })
            EmptyState("Проект не найден", "Выберите товар в каталоге и добавьте фото", "В каталог") { vm.nav.tab(Screen.Catalog) }
        }
        return
    }
    val product = Catalog.byId(draft.productId)
    val ratio = formatRatio(draft.formatId)
    val total = Pricing.unitPrice(product, draft.formatId, draft.pages, draft.photos.size, draft.printType) * draft.copies
    var error by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Предпросмотр", onBack = { vm.nav.back() }, subtitle = product.title)
        Box(Modifier.weight(1f).fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant)) {
            when (product.kind) {
                ProductKind.BOOK -> BookFlip(draft, ratio)
                ProductKind.COLLAGE -> Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
                    CollageView(draft, ratio, Modifier.shadow(10.dp))
                }
                ProductKind.PRINT -> PrintsPreview(draft)
                // Фото на документы: каждый снимок — свой лист 10×15 с копиями.
                ProductKind.DOCS -> androidx.compose.foundation.lazy.LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    items(draft.photos.size) { i ->
                        DocSheet(draft.photos[i], Pricing.format(product, draft.formatId), Modifier.shadow(6.dp))
                    }
                }
            }
        }

        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text(Pricing.details(draft) + if (draft.copies > 1) " × ${draft.copies}" else "", style = MaterialTheme.typography.bodyMedium)
                if (product.kind == ProductKind.BOOK) {
                    val empty = Books.emptyPages(draft.bookPages)
                    val unplaced = Books.unplaced(draft.photos, draft.bookPages).size
                    if (empty > 0) {
                        Spacer(Modifier.height(6.dp))
                        NoteBox("Пустых страниц: $empty. Их можно заполнить в конструкторе или оставить для записей")
                    }
                    if (unplaced > 0) {
                        Spacer(Modifier.height(6.dp))
                        NoteBox("Фото не на страницах: $unplaced. Добавьте страниц или разместите их в конструкторе")
                    }
                }
                error?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(formatMoney(total), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    SecondaryButton("Изменить", onClick = { vm.nav.back() })
                    PrimaryButton("В корзину", onClick = {
                        val problem = vm.addDraftToCart()
                        if (problem != null) error = problem else vm.nav.tab(Screen.Cart)
                    })
                }
            }
        }
    }
}

/**
 * Книга по разворотам: обложка, затем страницы парами. При перелистывании разворот
 * поворачивается в 3D вокруг корешка — получается эффект настоящей книги.
 */
@Composable
private fun BookFlip(draft: CartItem, ratio: Float) {
    val spreads: List<List<Int>> = listOf(listOf(-1)) + (0 until draft.pages step 2).map { listOf(it, it + 1) }
    val pager = rememberPagerState { spreads.size }
    Column(Modifier.fillMaxSize()) {
        HorizontalPager(state = pager, modifier = Modifier.weight(1f).fillMaxWidth()) { s ->
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val offset = (pager.currentPage - s) + pager.currentPageOffsetFraction
                        cameraDistance = 14f * density
                        // Уходящая страница поворачивается вокруг левого края (корешка), приходящая — вокруг правого.
                        transformOrigin = TransformOrigin(if (offset > 0f) 0f else 1f, 0.5f)
                        rotationY = (-offset * 75f).coerceIn(-90f, 90f)
                        alpha = 1f - (abs(offset) * 0.4f).coerceIn(0f, 0.6f)
                    }
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                val pages = spreads[s]
                if (pages.first() < 0) {
                    CoverView(draft.coverTitle, draft.photos.firstOrNull(), draft.template, ratio, Modifier.fillMaxWidth(0.62f).shadow(10.dp))
                } else {
                    Row(Modifier.fillMaxWidth().shadow(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        pages.forEach { i ->
                            BookPageView(draft.bookPages.getOrElse(i) { BookPage() }, draft.template, ratio, Modifier.weight(1f), frameless = draft.noFrame)
                        }
                    }
                    // Тень корешка посередине разворота.
                    Box(
                        Modifier
                            .width(18.dp)
                            .fillMaxHeight(0.6f)
                            .background(Brush.horizontalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.18f), Color.Transparent))),
                    )
                }
            }
        }
        val label = spreads.getOrNull(pager.currentPage)?.let { p ->
            if (p.first() < 0) "Обложка" else "Страницы ${p.first() + 1}–${p.last() + 1} из ${draft.pages}"
        }.orEmpty()
        Text(
            "$label · листайте в сторону",
            Modifier.align(Alignment.CenterHorizontally).padding(bottom = 10.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Печать: каждое фото — карточка в пропорциях формата, в уголке — сколько копий. */
@Composable
private fun PrintsPreview(draft: CartItem) {
    val ratio = formatRatio(draft.formatId).let { if (it > 1f) 1f / it else it }
    LazyVerticalGrid(
        GridCells.Fixed(3),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(draft.photos, key = { it }) { uri ->
            Box(
                Modifier
                    .aspectRatio(ratio)
                    .shadow(4.dp, RoundedCornerShape(2.dp))
                    .background(Color.White)
                    .padding(5.dp),
            ) {
                PhotoThumb(uri, Modifier.fillMaxSize())
                if (draft.copies > 1) {
                    Text(
                        "×${draft.copies}",
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}
