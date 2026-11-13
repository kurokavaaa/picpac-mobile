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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import by.picpac.studio.data.AppViewModel
import by.picpac.studio.data.BookPage
import by.picpac.studio.data.Books
import by.picpac.studio.data.Catalog
import by.picpac.studio.data.DecorType
import by.picpac.studio.data.Decoration
import by.picpac.studio.data.Placement
import by.picpac.studio.data.ProductKind
import by.picpac.studio.data.StickerKind
import by.picpac.studio.data.Template
import by.picpac.studio.data.TextFont
import by.picpac.studio.data.FREE_PAGE_MAX
import by.picpac.studio.data.newToken
import by.picpac.studio.ui.theme.family
import by.picpac.studio.ui.Screen
import by.picpac.studio.ui.components.AddTile
import by.picpac.studio.ui.components.BookPageView
import by.picpac.studio.ui.components.CollageView
import by.picpac.studio.ui.components.CoverView
import by.picpac.studio.ui.components.DecorActions
import by.picpac.studio.ui.components.NoteBox
import by.picpac.studio.ui.components.PhotoThumb
import by.picpac.studio.ui.components.PrimaryButton
import by.picpac.studio.ui.components.ScreenHeader
import by.picpac.studio.ui.components.SecondaryButton
import by.picpac.studio.ui.components.SelectChip
import by.picpac.studio.ui.components.StickerView
import by.picpac.studio.ui.components.formatRatio
import by.picpac.studio.ui.components.DragHint
import by.picpac.studio.ui.components.LayoutPicker
import by.picpac.studio.data.Layouts
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

/** Цвета для подписей: тёмный, белый, фирменный красный, зелёный, золотой, голубой. */
private val textColors = listOf(0xFF292316, 0xFFFFFFFF, 0xFFDD5757, 0xFF52645D, 0xFF8A6A3B, 0xFF568CDE)

/**
 * Конструктор: редактор страниц фотокниги или листа коллажа.
 * Все правки сразу пишутся в черновик проекта, поэтому их видно в предпросмотре и они не теряются.
 */
@Composable
fun EditorScreen(vm: AppViewModel) {
    val draft = vm.state.draft
    if (draft == null) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader("Конструктор", onBack = { vm.nav.back() })
            EmptyState("Проект не найден", "Выберите товар в каталоге и добавьте фото", "В каталог") { vm.nav.tab(Screen.Catalog) }
        }
        return
    }
    val product = Catalog.byId(draft.productId)
    val isBook = product.kind == ProductKind.BOOK
    val ratio = formatRatio(draft.formatId)
    val context = LocalContext.current

    var selectedDecor by remember { mutableStateOf<String?>(null) }
    var selectedSlot by remember { mutableStateOf<Int?>(null) }
    var selectedPhoto by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<Decoration?>(null) }
    var stickerPicker by remember { mutableStateOf(false) }
    var confirmAi by remember { mutableStateOf(false) }
    // Пока фото тащат между ячейками, страницы книги не должны листаться.
    var dragging by remember { mutableStateOf(false) }
    // Где на экране текущая страница — для показа жеста перетаскивания поверх неё.
    var pageRect by remember { mutableStateOf<Rect?>(null) }
    var showDragHint by remember { mutableStateOf(false) }

    val pager = rememberPagerState { if (isBook) draft.pages + 1 else 1 }
    // Для книги: −1 — обложка, 0..pages−1 — страницы.
    val pageIndex = if (isBook) pager.currentPage - 1 else 0
    LaunchedEffect(pager.currentPage) {
        selectedDecor = null
        selectedSlot = null
    }

    /** Изменить украшения текущего листа (страницы книги или коллажа) на свежем черновике внутри VM. */
    fun editDecorations(change: (List<Decoration>) -> List<Decoration>) {
        val target = pageIndex
        vm.updateDraft { d ->
            if (isBook) {
                d.copy(layoutEdited = true, bookPages = d.bookPages.mapIndexed { i, p -> if (i == target) p.copy(decorations = change(p.decorations)) else p })
            } else {
                d.copy(decorations = change(d.decorations))
            }
        }
    }

    fun editPage(change: (BookPage) -> BookPage) {
        val target = pageIndex
        vm.updateDraft { d -> d.copy(layoutEdited = true, bookPages = d.bookPages.mapIndexed { i, p -> if (i == target) change(p) else p }) }
    }

    /** Поставить фото в ячейку. В коллаже-сетке ячейки — это просто порядок фото. */
    fun putIntoSlot(slot: Int, uri: String) {
        if (isBook) {
            // slot < 0 — «добавить на свободную страницу», там нет ячеек.
            if (slot < 0) editPage { it.addFree(uri) } else editPage { it.withSlot(slot, uri) }
        } else {
            val photos = vm.state.draft?.photos.orEmpty().toMutableList()
            val from = photos.indexOf(uri)
            when {
                from >= 0 && slot < photos.size -> { photos[from] = photos[slot]; photos[slot] = uri }
                slot < photos.size -> photos[slot] = uri
                from < 0 -> photos.add(uri)
            }
            vm.setDraftPhotos(photos.distinct())
        }
    }

    fun clearSlot(slot: Int) {
        if (isBook) {
            if (slot >= 0) editPage { it.withSlot(slot, "") }
        } else draft.photos.getOrNull(slot)?.let { vm.setDraftPhotos(draft.photos - it) }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(20)) { uris: List<Uri> ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        uris.forEach { runCatching { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) } }
        val added = uris.map { it.toString() }
        val slot = selectedSlot
        vm.setDraftPhotos((vm.state.draft?.photos.orEmpty() + added).distinct())
        if (slot != null) {
            putIntoSlot(slot, added.first())
            selectedSlot = null
        }
    }

    val actions = DecorActions(
        selectedId = selectedDecor,
        onSelect = { id ->
            selectedDecor = id
            selectedSlot = null
            selectedPhoto = null
        },
        onChange = { changed -> editDecorations { list -> list.map { if (it.id == changed.id) changed else it } } },
        onEditText = { editing = it },
    )
    val selected = (if (isBook) draft.bookPages.getOrNull(pageIndex)?.decorations.orEmpty() else draft.decorations)
        .firstOrNull { it.id == selectedDecor }

    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Конструктор", onBack = { vm.nav.back() }, subtitle = product.title) {
            TextButton(onClick = { vm.nav.go(Screen.Preview) }) { Text("Просмотр") }
        }

        // Рабочая область: листаем страницы книги или работаем с листом коллажа.
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (isBook) {
                HorizontalPager(
                    state = pager,
                    contentPadding = PaddingValues(horizontal = 36.dp),
                    pageSpacing = 14.dp,
                    userScrollEnabled = selectedDecor == null && selectedPhoto == null && !dragging,
                    modifier = Modifier.fillMaxSize(),
                ) { p ->
                    val current = p == pager.currentPage
                    Box(Modifier.fillMaxSize().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                        if (p == 0) {
                            CoverView(draft.coverTitle, draft.photos.firstOrNull(), draft.template, ratio)
                        } else {
                            BookPageView(
                                page = draft.bookPages.getOrElse(p - 1) { BookPage() },
                                template = draft.template,
                                ratio = ratio,
                                modifier = if (current) Modifier.onGloballyPositioned { pageRect = it.boundsInRoot() } else Modifier,
                                selectedSlot = if (current) selectedSlot else null,
                                onSlotClick = if (current) { i -> selectedDecor = null; selectedSlot = i } else null,
                                decor = if (current) actions else null,
                                frameless = draft.noFrame,
                                // Сетка: удержать фото и перетащить в другую ячейку — поменяются местами.
                                onSwap = if (current) { a, b -> editPage { it.swapSlots(a, b) } } else null,
                                onDragActive = if (current) { active -> dragging = active } else null,
                                // Свободная страница: фото двигаются пальцем, касание поднимает наверх.
                                selectedPhoto = if (current) selectedPhoto else null,
                                onPhotoSelect = if (current) { uri ->
                                    selectedPhoto = uri
                                    selectedDecor = null
                                    selectedSlot = null
                                    editPage { it.bringToFront(uri) }
                                } else null,
                                onPlacementChange = if (current) { uri, pl -> editPage { it.withPlacement(uri, pl) } } else null,
                                onPhotoRemove = if (current) { uri ->
                                    editPage { it.withoutPhoto(uri) }
                                    selectedPhoto = null
                                } else null,
                            )
                        }
                    }
                }
            } else {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    CollageView(
                        item = draft,
                        ratio = ratio,
                        selectedSlot = selectedSlot,
                        onSlotClick = if (!draft.freeLayout) { i -> selectedDecor = null; selectedSlot = i } else null,
                        selectedPhoto = selectedPhoto,
                        onPhotoSelect = { uri ->
                            selectedPhoto = uri
                            selectedDecor = null
                            // Выбранное фото поднимаем наверх: последнее в списке рисуется поверх.
                            val photos = vm.state.draft?.photos.orEmpty()
                            if (photos.lastOrNull() != uri) vm.setDraftPhotos(photos - uri + uri)
                        },
                        onPlacementChange = { uri, p ->
                            vm.updateDraft { d ->
                                val i = d.photos.indexOf(uri)
                                if (i < 0) d else d.copy(
                                    placements = d.photos.indices.map { j -> if (j == i) p else d.placements.getOrNull(j) ?: Placement.default(j) },
                                )
                            }
                        },
                        onPhotoRemove = { uri ->
                            vm.setDraftPhotos(vm.state.draft?.photos.orEmpty() - uri)
                            selectedPhoto = null
                        },
                        decor = actions,
                    )
                }
            }
        }

        // Панель инструментов: меняется в зависимости от того, что выбрано.
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 330.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                val slot = selectedSlot
                when {
                    slot != null -> PhotoChooser(
                        photos = draft.photos,
                        unplaced = if (isBook) Books.unplaced(draft.photos, draft.bookPages) else emptyList(),
                        onPick = { uri -> putIntoSlot(slot, uri); selectedSlot = null },
                        onClear = { clearSlot(slot); selectedSlot = null },
                        onGallery = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        onClose = { selectedSlot = null },
                    )
                    selected != null -> DecorTools(
                        decoration = selected,
                        onEditText = { editing = selected },
                        onColor = { c -> actions.onChange(selected.copy(color = c)) },
                        onFont = { f -> actions.onChange(selected.copy(font = f)) },
                        onResize = { k -> actions.onChange(selected.copy(size = (selected.size * k).coerceIn(0.03f, 0.9f))) },
                        onDelete = {
                            editDecorations { list -> list.filterNot { it.id == selected.id } }
                            selectedDecor = null
                        },
                        onDone = { selectedDecor = null },
                    )
                    else -> {
                        val title = when {
                            !isBook -> if (draft.freeLayout) "Свободный коллаж — двигайте фото пальцем" else "Коллаж — нажмите на ячейку, чтобы сменить фото"
                            pageIndex < 0 -> "Обложка"
                            else -> "Страница ${pageIndex + 1} из ${draft.pages} — нажмите на ячейку, чтобы сменить фото"
                        }
                        Text(title, style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(10.dp))
                        Text("Оформление", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SelectChip("Без рамки", draft.noFrame, { vm.updateDraft { it.copy(noFrame = !it.noFrame) } })
                            Template.entries.forEach { t -> TemplateChip(t, draft.template == t) { vm.updateDraft { it.copy(template = t) } } }
                        }

                        if (isBook && pageIndex >= 0) {
                            Spacer(Modifier.height(10.dp))
                            Text("Раскладка страницы", style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.height(6.dp))
                            val page = draft.bookPages.getOrNull(pageIndex) ?: BookPage()
                            LayoutPicker(if (page.free) null else page.layoutId, ratio, { l -> editPage { it.withLayout(l.id) } })
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SelectChip("Свободно", page.free, { editPage { it.toFree() } })
                                if (!page.free) SelectChip("Как перетаскивать?", false, { showDragHint = true })
                            }
                            Text(
                                if (page.free) "Тяните фото пальцем, размер — щипком или за оранжевый уголок. Касание поднимает фото наверх"
                                else "Удержите палец на фото и перетащите в другую ячейку — фото поменяются местами",
                                Modifier.padding(top = 6.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (page.free && page.photos.size < FREE_PAGE_MAX) {
                                Spacer(Modifier.height(8.dp))
                                SelectChip("Добавить фото на страницу", false, {
                                    selectedDecor = null
                                    selectedSlot = -1
                                })
                            }
                        }
                        if (isBook && pageIndex < 0) {
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(
                                value = draft.coverTitle,
                                onValueChange = { t -> vm.updateDraft { it.copy(coverTitle = t.take(40)) } },
                                label = { Text("Название на обложке") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        Spacer(Modifier.height(12.dp))
                        val onCover = isBook && pageIndex < 0
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SelectChip("Текст", false, {
                                if (!onCover) {
                                    val d = Decoration(newToken(), DecorType.TEXT, text = "Ваш текст", color = draft.template.accent, size = 0.08f)
                                    editDecorations { it + d }
                                    selectedDecor = d.id
                                    editing = d
                                }
                            })
                            SelectChip("Стикер", false, { if (!onCover) stickerPicker = true })
                            SelectChip("Собрать с ИИ", false, { confirmAi = true })
                        }
                        if (onCover) {
                            Spacer(Modifier.height(8.dp))
                            NoteBox("Тексты и стикеры добавляются на страницы, а на обложке — название")
                        }
                        if (isBook) {
                            val empty = Books.emptyPages(draft.bookPages)
                            val unplaced = Books.unplaced(draft.photos, draft.bookPages).size
                            if (empty > 0 || unplaced > 0) {
                                Spacer(Modifier.height(8.dp))
                                NoteBox(
                                    listOfNotNull(
                                        if (empty > 0) "пустых страниц: $empty" else null,
                                        if (unplaced > 0) "фото не на страницах: $unplaced" else null,
                                    ).joinToString(", ").replaceFirstChar { it.uppercase() },
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton("Просмотр", onClick = { vm.nav.go(Screen.Preview) }, modifier = Modifier.weight(1f))
                    PrimaryButton("Готово", onClick = { vm.nav.back() }, modifier = Modifier.weight(1f))
                }
            }
        }
    }

    // Жест перетаскивания: сам при первом открытии книги и по кнопке «Как перетаскивать?».
    val currentPage = draft.bookPages.getOrNull(pageIndex)
    val rect = pageRect
    if (isBook && rect != null && currentPage != null && !currentPage.free && (showDragHint || !vm.state.dragHintDone)) {
        DragHint(rect, Layouts.byId(currentPage.layoutId).cells) {
            showDragHint = false
            vm.finishDragHint()
        }
    }
    }

    editing?.let { e ->
        var text by remember(e.id) { mutableStateOf(e.text) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Текст на странице") },
            text = { OutlinedTextField(value = text, onValueChange = { text = it.take(60) }, maxLines = 3) },
            confirmButton = {
                TextButton(onClick = {
                    editDecorations { list -> list.map { if (it.id == e.id) it.copy(text = text.ifBlank { "Ваш текст" }) else it } }
                    editing = null
                }) { Text("Готово") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Отмена") } },
        )
    }

    if (stickerPicker) {
        AlertDialog(
            onDismissRequest = { stickerPicker = false },
            title = { Text("Стикеры") },
            text = {
                LazyVerticalGrid(GridCells.Fixed(3), Modifier.heightIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(StickerKind.entries) { kind ->
                        Column(
                            Modifier
                                .clip(MaterialTheme.shapes.small)
                                .clickable {
                                    val d = Decoration(newToken(), DecorType.STICKER, sticker = kind, size = 0.2f)
                                    editDecorations { it + d }
                                    selectedDecor = d.id
                                    stickerPicker = false
                                }
                                .padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            StickerView(kind, Modifier.size(52.dp))
                            Text(kind.title, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { stickerPicker = false }) { Text("Закрыть") } },
        )
    }

    if (confirmAi) {
        AlertDialog(
            onDismissRequest = { confirmAi = false },
            title = { Text("Собрать с ИИ?") },
            text = { Text("ИИ подберёт шаблон, раскладку страниц, подписи и стикеры. Ваши ручные правки заменятся. Сейчас это демо-режим.") },
            confirmButton = { TextButton(onClick = { confirmAi = false; vm.aiCompose() }) { Text("Собрать") } },
            dismissButton = { TextButton(onClick = { confirmAi = false }) { Text("Отмена") } },
        )
    }
}

/** Лента фото проекта для выбранной ячейки. Фото, которых ещё нет на страницах, идут первыми. */
@Composable
private fun PhotoChooser(
    photos: List<String>,
    unplaced: List<String>,
    onPick: (String) -> Unit,
    onClear: () -> Unit,
    onGallery: () -> Unit,
    onClose: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Фото для ячейки", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
        TextButton(onClick = onClear) { Text("Очистить") }
        TextButton(onClick = onClose) { Text("Закрыть") }
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item { AddTile("Галерея", onGallery, Modifier.size(76.dp)) }
        items(unplaced + photos.filterNot { it in unplaced }, key = { it }) { uri ->
            Box(Modifier.size(76.dp).clip(MaterialTheme.shapes.small).clickable { onPick(uri) }) {
                PhotoThumb(uri, Modifier.fillMaxSize())
                if (uri in unplaced) {
                    Box(Modifier.align(Alignment.TopStart).padding(4.dp).size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                }
            }
        }
    }
    if (unplaced.isNotEmpty()) {
        Text(
            "Точкой отмечены фото, которых ещё нет на страницах",
            Modifier.padding(top = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FontChip(title: String, font: TextFont?, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .clip(MaterialTheme.shapes.small)
            .background(if (selected) colors.primaryContainer else colors.surface)
            .border(1.dp, if (selected) colors.primary else colors.outline, MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, fontFamily = font?.family(), style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
        Text(font?.note ?: "как в оформлении", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
    }
}

/** Инструменты для выбранного текста или стикера. */
@Composable
private fun DecorTools(
    decoration: Decoration,
    onEditText: () -> Unit,
    onColor: (Long) -> Unit,
    onFont: (TextFont?) -> Unit,
    onResize: (Float) -> Unit,
    onDelete: () -> Unit,
    onDone: () -> Unit,
) {
    val isText = decoration.type == DecorType.TEXT
    Text(
        if (isText) "Текст: двигайте пальцем, размер — щипком или кнопками" else "Стикер: двигайте пальцем, размер — щипком или кнопками",
        style = MaterialTheme.typography.titleSmall,
    )
    Spacer(Modifier.height(10.dp))
    if (isText) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            SelectChip("Изменить текст", false, onEditText)
            textColors.forEach { c ->
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(c))
                        .border(
                            if (decoration.color == c) 3.dp else 1.dp,
                            if (decoration.color == c) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            CircleShape,
                        )
                        .clickable { onColor(c) },
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        // Каждое название написано своим шрифтом — сразу видно, как будет выглядеть надпись.
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FontChip("Шаблон", null, decoration.font == null) { onFont(null) }
            TextFont.entries.forEach { f -> FontChip(f.title, f, decoration.font == f) { onFont(f) } }
        }
        Spacer(Modifier.height(10.dp))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SelectChip("Меньше", false, { onResize(0.85f) })
        SelectChip("Больше", false, { onResize(1.18f) })
        SelectChip("Удалить", false, onDelete)
        SelectChip("Готово", true, onDone)
    }
    if (isText) {
        Text(
            "Двойное касание по тексту — изменить надпись",
            Modifier.padding(top = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
