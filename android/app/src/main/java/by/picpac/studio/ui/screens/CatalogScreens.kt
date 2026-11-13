package by.picpac.studio.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import by.picpac.studio.data.AppViewModel
import by.picpac.studio.data.AlbumMode
import by.picpac.studio.data.Albums
import by.picpac.studio.data.Layouts
import by.picpac.studio.data.PrintType
import by.picpac.studio.ui.components.CardBox
import by.picpac.studio.ui.components.DocPhoto
import by.picpac.studio.ui.components.DocSheet
import by.picpac.studio.ui.components.LayoutPicker
import by.picpac.studio.ui.components.LayoutThumb
import by.picpac.studio.ui.components.SummaryRow
import by.picpac.studio.ui.components.tourTarget
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import by.picpac.studio.data.BookPage
import by.picpac.studio.data.Catalog
import by.picpac.studio.data.Category
import by.picpac.studio.data.FREE_COLLAGE_MAX
import by.picpac.studio.data.Placement
import by.picpac.studio.data.Pricing
import by.picpac.studio.data.Product
import by.picpac.studio.data.ProductKind
import by.picpac.studio.data.Template
import by.picpac.studio.ui.Screen
import by.picpac.studio.ui.components.AddTile
import by.picpac.studio.ui.components.BookPageView
import by.picpac.studio.ui.components.CollageView
import by.picpac.studio.ui.components.EditableStepper
import by.picpac.studio.ui.components.NoteBox
import by.picpac.studio.ui.components.PhotoThumb
import by.picpac.studio.ui.components.PrimaryButton
import by.picpac.studio.ui.components.ProductArt
import by.picpac.studio.ui.components.ScreenHeader
import by.picpac.studio.ui.components.SecondaryButton
import by.picpac.studio.ui.components.SectionTitle
import by.picpac.studio.ui.components.SelectChip
import by.picpac.studio.ui.components.Stepper
import by.picpac.studio.ui.components.formatMoney
import by.picpac.studio.ui.components.formatRatio

@Composable
fun CatalogScreen(vm: AppViewModel) {
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<String?>(null) }

    val products = Catalog.products.filter { p ->
        (category == null || p.category.name == category) &&
            (query.isBlank() || p.title.contains(query.trim(), ignoreCase = true) || p.short.contains(query.trim(), ignoreCase = true))
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Каталог", onBack = { vm.nav.tab(Screen.Home) })
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            placeholder = { Text("Поиск товаров") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(50),
        )
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SelectChip("Все", category == null, { category = null })
            Category.entries.forEach { c -> SelectChip(c.title, category == c.name, { category = c.name }) }
        }
        if (products.isEmpty()) {
            Text(
                "Ничего не нашли. Попробуйте «книга» или «печать».",
                Modifier.padding(20.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(products, key = { it.id }) { product ->
                ProductCard(product, Modifier.tourTarget("product-${product.id}")) { vm.nav.go(Screen.Product(product.id)) }
            }
        }
    }
}

@Composable
private fun ProductCard(product: Product, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(10.dp),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1.15f).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceVariant)) {
            ProductArt(artFor(product), Modifier.fillMaxSize().padding(10.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(product.title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
        Text(
            "от ${formatMoney(Pricing.startPrice(product))}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(product.short, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

/** Что пишут под вариантами поверхности — дословно по смыслу со страницы «Печать фото» на сайте. */
private val finishNotes = mapOf(
    "Глянцевая" to "ярче, но видны отпечатки пальцев",
    "Матовая" to "без бликов и отпечатков",
)

/**
 * Экран товара: параметры, фото, оформление. Всё пишется в черновик проекта (AppViewModel.draft),
 * поэтому конструктор и предпросмотр видят те же данные, а проект не теряется при выходе.
 */
@Composable
fun ProductScreen(vm: AppViewModel, productId: String) {
    val product = remember(productId) { Catalog.byId(productId) }
    val draft = vm.state.draft?.takeIf { it.productId == productId }
    if (draft == null) {
        LaunchedEffect(productId) { vm.openProduct(productId) }
        return
    }
    // Альбом: сначала выбираем, как его собирать.
    if (product.id == "album" && draft.albumMode == null) {
        AlbumStart(vm, product)
        return
    }
    val context = LocalContext.current
    var error by remember { mutableStateOf<String?>(null) }
    val isCollage = product.kind == ProductKind.COLLAGE
    val isBook = product.kind == ProductKind.BOOK
    val isDocs = product.kind == ProductKind.DOCS
    val isPreset = draft.albumMode == AlbumMode.PRESET
    val preset = Albums.byId(draft.preset)

    fun limitFor(freeLayout: Boolean, layout: Int) = when {
        isCollage && freeLayout -> FREE_COLLAGE_MAX
        isCollage -> layout
        // Готовый макет: фото не больше, чем ячеек на его страницах.
        isPreset && preset != null -> preset.photoSlots
        product.maxPhotos > 0 -> product.maxPhotos
        else -> Int.MAX_VALUE
    }

    // Системный выбор фото: без разрешений на всю галерею, пользователь отмечает только нужные снимки.
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(50)) { uris: List<Uri> ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        uris.forEach { uri ->
            // Сохраняем доступ к фото, чтобы корзина и заказы показывали их после перезапуска.
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        }
        val duration = (700L + uris.size * 120L).coerceAtMost(2600L)
        vm.showLoader("Загружаем фото", "${uris.size} шт.", duration) {
            val current = vm.state.draft ?: return@showLoader
            val limit = limitFor(current.freeLayout, current.layout)
            val merged = (current.photos + uris.map { it.toString() }).distinct()
            vm.setDraftPhotos(merged.take(limit))
            error = if (merged.size > limit) "Поместилось $limit фото, лишние не добавлены" else null
        }
    }
    fun pickPhotos() = picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

    val format = Pricing.format(product, draft.formatId)
    val total = Pricing.unitPrice(product, draft.formatId, draft.pages, draft.photos.size, draft.printType) * draft.copies
    val ratio = formatRatio(draft.formatId)

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(product.title, onBack = { vm.nav.back() })
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 16.dp)) {
            item {
                Text(product.description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                Text(
                    "от ${formatMoney(Pricing.startPrice(product))}" + if (product.kind == ProductKind.PRINT) " за фото" else "",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                product.priceNote?.let {
                    Spacer(Modifier.height(8.dp))
                    NoteBox(it)
                }
                if (product.specs.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    // Коротко — характеристики из таблицы сравнения на сайте PicPac.
                    CardBox { product.specs.forEach { (k, v) -> SummaryRow(k, v) } }
                }
                if (product.id == "album") {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (isPreset) "Готовый макет «${preset?.title}»" else "Собираете сами",
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        TextButton(onClick = { vm.resetAlbumMode() }) { Text("Сменить") }
                    }
                }
            }

            item {
                SectionTitle(
                    when {
                        isDocs -> "Документ"
                        isBook -> "Формат страниц, см"
                        else -> "Размер, см"
                    },
                )
                if (isDocs) {
                    // Документов много и у каждого своё пояснение — списком, а не плашками в строку.
                    CardBox {
                        product.formats.forEachIndexed { i, f ->
                            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                            Row(
                                Modifier.fillMaxWidth().clickable { vm.updateDraft { it.copy(formatId = f.id) } }.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = f.id == draft.formatId, onClick = { vm.updateDraft { it.copy(formatId = f.id) } })
                                Column(Modifier.weight(1f)) {
                                    Text(f.title, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        "${f.note} · ${Pricing.docsPerSheet(f)} шт. на листе 10×15",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        product.formats.forEach { f ->
                            val label = if (isBook) f.title else "${f.title} · ${formatMoney(Pricing.printPrice(f, draft.printType))}"
                            SelectChip(label + if (f.approximate) "*" else "", f.id == draft.formatId, { vm.updateDraft { it.copy(formatId = f.id) } })
                        }
                    }
                }
                if (format.approximate || draft.printType.approximate && !isBook && !isDocs) {
                    Spacer(Modifier.height(10.dp))
                    NoteBox(
                        when {
                            isDocs -> "Требования к фото уточняйте в учреждении. Размер паспорта РБ — по постановлению МВД № 251"
                            isBook -> "* Цена этого формата ориентировочная: на сайте указана только для 20×20"
                            format.approximate -> "* Такого размера нет на сайте PicPac — цена ориентировочная"
                            else -> "Цена цифровой печати ориентировочная: на сайте указано только «от 0,61 за фото»"
                        },
                    )
                }
            }

            if (product.kind == ProductKind.PRINT || isCollage) {
                item {
                    SectionTitle("Вид печати")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PrintType.entries.forEach { t ->
                            OptionCard(t.title, t.note, t == draft.printType, Modifier.weight(1f)) { vm.updateDraft { it.copy(printType = t) } }
                        }
                    }
                }
            }

            product.finishTitle?.let { finishTitle ->
                item {
                    SectionTitle(finishTitle)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        product.finishOptions.forEach { f ->
                            val note = if (finishTitle == "Поверхность") finishNotes[f] else null
                            if (note != null) {
                                OptionCard(f, note, draft.finish == f, Modifier.weight(1f)) { vm.updateDraft { it.copy(finish = f) } }
                            } else {
                                SelectChip(f, draft.finish == f, { vm.updateDraft { it.copy(finish = f) } })
                            }
                        }
                    }
                }
            }

            if (isBook) {
                item {
                    SectionTitle("Страниц")
                    if (isPreset && preset != null) {
                        Text(
                            "${draft.pages} страниц — задано макетом, на них ${preset.photoSlots} ячеек под фото",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            EditableStepper(
                                value = draft.pages,
                                onValueChange = { n -> vm.updateDraft { it.copy(pages = n) } },
                                min = product.basePages,
                                max = product.maxPages,
                                step = 2,
                                // Страницы печатаются разворотами, поэтому нечётное число округляем вверх до чётного.
                                normalize = { if (it % 2 == 0) it else it + 1 },
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "+${formatMoney(product.pageStepPrice)} за 2 страницы",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            "Можно вписать число: чётное, от ${product.basePages} до ${product.maxPages}",
                            Modifier.padding(top = 6.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (isCollage) {
                item {
                    SectionTitle("Макет коллажа")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SelectChip("По макету", !draft.freeLayout, {
                            vm.updateDraft { it.copy(freeLayout = false, photos = it.photos.take(it.layout)) }
                        })
                        SelectChip("Свободно", draft.freeLayout, {
                            vm.updateDraft { d ->
                                d.copy(freeLayout = true, placements = d.photos.indices.map { i -> d.placements.getOrNull(i) ?: Placement.default(i) })
                            }
                        })
                    }
                    if (!draft.freeLayout) {
                        Spacer(Modifier.height(10.dp))
                        LayoutPicker(draft.layoutId, ratio, { l -> vm.updateDraft { it.copy(layoutId = l.id, photos = it.photos.take(l.count)) } })
                    }
                }
            }

            item {
                val limit = limitFor(draft.freeLayout, draft.layout)
                val counter = when {
                    isCollage -> "${draft.photos.size} из $limit"
                    isPreset && preset != null -> "${draft.photos.size} из ${preset.photoSlots}"
                    product.maxPhotos > 0 -> "${draft.photos.size} · нужно ${product.minPhotos}–${product.maxPhotos}"
                    else -> draft.photos.size.toString()
                }
                SectionTitle("Фото · $counter")
                Text(if (isPreset) "По одному фото в каждую ячейку макета" else product.photosHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { AddTile("Из галереи", ::pickPhotos, Modifier.size(88.dp).tourTarget("add-photos")) }
                    items(draft.photos, key = { it }) { uri ->
                        Box(Modifier.size(88.dp)) {
                            PhotoThumb(uri, Modifier.fillMaxSize())
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                                    .clickable { vm.setDraftPhotos(draft.photos - uri) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Убрать фото", Modifier.size(14.dp))
                            }
                        }
                    }
                }
                val missing = product.minPhotos - draft.photos.size
                when {
                    draft.photos.isEmpty() -> {
                        Spacer(Modifier.height(10.dp))
                        NoteBox(if (isDocs) "Выберите портрет на светлом фоне — мы разложим копии на листе" else "Выберите фото из галереи телефона — макет соберётся автоматически")
                    }
                    missing > 0 && !isPreset -> {
                        Spacer(Modifier.height(10.dp))
                        NoteBox("На сайте для этой книги — от ${product.minPhotos} фото. Добавьте ещё $missing, чтобы страницы не пустовали")
                    }
                }
            }

            if (isDocs && draft.photos.isNotEmpty()) {
                item {
                    SectionTitle("Как получится")
                    Row(verticalAlignment = Alignment.Top) {
                        DocPhoto(draft.photos.first(), format, 96.dp)
                        Spacer(Modifier.width(14.dp))
                        Text(
                            "Лицо — внутри овала, макушка и подбородок — между красными линиями. " +
                                "На листе будет ${Pricing.docsPerSheet(format)} копий.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { DocSheet(draft.photos.first(), format) }
                }
            }

            if (isBook || isCollage) {
                item {
                    SectionTitle("Оформление")
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SelectChip("Без рамки", draft.noFrame, { vm.updateDraft { it.copy(noFrame = !it.noFrame) } })
                        Template.entries.forEach { t ->
                            TemplateChip(t, draft.template == t) { vm.updateDraft { it.copy(template = t) } }
                        }
                    }
                    SectionTitle("Ваш проект")
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        if (isCollage) {
                            CollageView(draft, ratio, Modifier.fillMaxWidth(0.6f))
                        } else {
                            BookPageView(draft.bookPages.firstOrNull() ?: BookPage(), draft.template, ratio, Modifier.fillMaxWidth(0.6f), frameless = draft.noFrame)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SecondaryButton("Конструктор", onClick = {
                            if (draft.photos.isEmpty()) error = "Сначала добавьте фото" else vm.nav.go(Screen.Editor)
                        }, modifier = Modifier.weight(1f))
                        SecondaryButton("Собрать с ИИ", onClick = {
                            if (draft.photos.isEmpty()) error = "Сначала добавьте фото" else vm.aiCompose { vm.nav.go(Screen.Editor) }
                        }, modifier = Modifier.weight(1f))
                    }
                    Text(
                        "ИИ работает в демо-режиме: подбирает шаблон, раскладку, подписи и стикеры",
                        Modifier.padding(top = 6.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                SectionTitle(
                    when (product.kind) {
                        ProductKind.PRINT -> "Копий каждого фото"
                        ProductKind.COLLAGE -> "Количество коллажей"
                        ProductKind.BOOK -> if (product.id == "album") "Количество альбомов" else "Количество книг"
                        ProductKind.DOCS -> "Комплектов"
                    },
                )
                Stepper(
                    value = draft.copies.toString(),
                    onMinus = { vm.updateDraft { it.copy(copies = it.copies - 1) } },
                    onPlus = { vm.updateDraft { it.copy(copies = it.copies + 1) } },
                    minusEnabled = draft.copies > 1,
                    plusEnabled = draft.copies < 99,
                )
                Spacer(Modifier.height(16.dp))
                SecondaryButton("Предпросмотр", onClick = {
                    if (draft.photos.isEmpty()) error = "Сначала добавьте фото" else vm.nav.go(Screen.Preview)
                }, modifier = Modifier.fillMaxWidth())
            }
        }

        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(6.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Итого", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatMoney(total), style = MaterialTheme.typography.titleLarge)
                    }
                    PrimaryButton("В корзину", onClick = {
                        val problem = vm.addDraftToCart()
                        if (problem != null) error = problem else vm.nav.tab(Screen.Cart)
                    }, modifier = Modifier.tourTarget("to-cart"))
                }
            }
        }
    }
}

/** Карточка-вариант с пояснением: вид печати, поверхность. Выбранная — с тёмной рамкой. */
@Composable
private fun OptionCard(title: String, note: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier
            .clip(MaterialTheme.shapes.small)
            .background(colors.surface)
            .border(if (selected) 2.dp else 1.dp, if (selected) colors.onBackground else colors.outline, MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(note, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
    }
}

/** Альбом: первый шаг — собрать самому или взять готовый макет. */
@Composable
private fun AlbumStart(vm: AppViewModel, product: Product) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(product.title, onBack = { vm.nav.back() })
        LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp)) {
            item {
                Text(product.description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                product.priceNote?.let { NoteBox(it) }
                SectionTitle("Как будем собирать")
                Column(Modifier.tourTarget("album-mode"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CardBox(onClick = { vm.chooseCustomAlbum() }) {
                        Text("Собрать самому", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Сами выбираете число страниц и макет каждой — от 1 до 7 фото на странице",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        "или возьмите готовый макет:",
                        Modifier.padding(top = 6.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    // Первый макет — внутри подсветки обучения, чтобы его тоже можно было нажать.
                    PresetCard(vm, Albums.presets.first())
                }
            }
            items(Albums.presets.drop(1), key = { it.id }) { preset ->
                Spacer(Modifier.height(10.dp))
                PresetCard(vm, preset)
            }
        }
    }
}

@Composable
private fun PresetCard(vm: AppViewModel, preset: by.picpac.studio.data.AlbumPreset) {
                CardBox(onClick = { vm.applyPreset(preset.id) }) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text(preset.title, style = MaterialTheme.typography.titleMedium)
                            Text(preset.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${preset.pages} страниц · ${preset.photoSlots} фото",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    // Первые страницы макета — чтобы сразу было видно характер альбома.
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        preset.pattern.take(5).forEach { id -> LayoutThumb(Layouts.byId(id), 1f, false, 48.dp) }
                    }
                }
}

/** Плашка шаблона: кружок с его фоном и рамкой + название. */
@Composable
internal fun TemplateChip(template: Template, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Color(template.background))
                .border(3.dp, Color(template.frame), CircleShape)
                .padding(7.dp)
                .clip(CircleShape)
                .background(Color(template.accent)),
        )
        Spacer(Modifier.width(8.dp))
        Text(template.title, style = MaterialTheme.typography.bodyMedium)
    }
}
