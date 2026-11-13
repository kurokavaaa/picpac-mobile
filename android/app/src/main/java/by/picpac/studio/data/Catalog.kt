package by.picpac.studio.data

/**
 * Каталог и цены с super.picpac.by (проверено 11–12.11.2026).
 * Цены в копейках. approximate = true — такого размера/товара на сайте нет, цена ориентировочная.
 */
object Catalog {
    val products = listOf(
        Product(
            id = "print",
            kind = ProductKind.PRINT,
            category = Category.PHOTO,
            title = "Фотопечать",
            short = "Снимки на фотобумаге",
            description = "Качественная печать ваших фотографий. Выберите размер, вид печати, поверхность бумаги и сколько копий каждого снимка нужно.",
            art = "prints",
            formats = listOf(
                // Цены 10×15, 15×20, 20×30 и 30×45 — с сайта. Остальных размеров на сайте нет.
                FormatOption("9x13", "9×13", printPrice = 120, approximate = true),
                FormatOption("10x15", "10×15", printPrice = 130),
                FormatOption("13x18", "13×18", printPrice = 150, approximate = true),
                FormatOption("15x20", "15×20", printPrice = 160),
                FormatOption("20x30", "20×30", printPrice = 320),
                FormatOption("21x30", "21×30 (A4)", printPrice = 340, approximate = true),
                FormatOption("30x40", "30×40", printPrice = 1800, approximate = true),
                FormatOption("30x45", "30×45", printPrice = 2000),
            ),
            finishTitle = "Поверхность",
            photosHint = "Каждое фото печатается отдельно",
        ),
        Product(
            id = "collage",
            kind = ProductKind.COLLAGE,
            category = Category.PHOTO,
            title = "Коллаж",
            short = "Несколько фото на одном листе",
            description = "Соберите коллаж из 2–6 фото: выберите сетку, и снимки сами встанут по местам. " +
                "Печатается как одно фото выбранного размера.",
            art = "collage",
            formats = listOf(
                FormatOption("15x20", "15×20", printPrice = 160),
                FormatOption("20x30", "20×30", printPrice = 320),
                FormatOption("30x45", "30×45", printPrice = 2000),
            ),
            finishTitle = "Поверхность",
            photosHint = "Нажмите на пустую ячейку, чтобы добавить фото",
        ),
        Product(
            id = "docs",
            kind = ProductKind.DOCS,
            category = Category.PHOTO,
            title = "Фото на документы",
            short = "Паспорт, виза, медкнижка",
            description = "Фото нужного размера, разложенные на листе 10×15 — останется только разрезать. " +
                "Требования к фото уточняйте в учреждении, куда подаёте документы.",
            art = "prints",
            formats = listOf(
                // Размер паспорта РБ — постановление МВД № 251 от 12.07.2024 (трафарет 40×50 мм).
                FormatOption("40x50-by", "Паспорт РБ", printPrice = 130, approximate = true, note = "40×50 мм", widthMm = 40, heightMm = 50),
                FormatOption("35x45-ru", "Паспорт РФ, загранпаспорт", printPrice = 130, approximate = true, note = "35×45 мм", widthMm = 35, heightMm = 45),
                FormatOption("35x45-schengen", "Шенгенская виза", printPrice = 130, approximate = true, note = "35×45 мм", widthMm = 35, heightMm = 45),
                FormatOption("50x50-us", "Виза США", printPrice = 130, approximate = true, note = "50×50 мм (2×2 дюйма)", widthMm = 50, heightMm = 50),
                FormatOption("30x40", "Медкнижка, пропуск, студенческий", printPrice = 130, approximate = true, note = "30×40 мм (3×4 см)", widthMm = 30, heightMm = 40),
            ),
            finishTitle = "Поверхность",
            finishOptions = listOf("Матовая", "Глянцевая"),
            photosHint = "Одно фото — один лист 10×15 с несколькими копиями",
            priceNote = "Услуги нет на сайте PicPac: считаем как один отпечаток 10×15 (1,30) за лист",
        ),
        Product(
            id = "album",
            kind = ProductKind.BOOK,
            category = Category.ALBUMS,
            title = "Фотоальбом",
            short = "Сами или по готовому макету",
            description = "Альбом со страницами по макетам PicPac: на каждой странице от 1 до 7 фото. " +
                "Соберите его сами или возьмите готовый макет — останется только добавить фото.",
            art = "book",
            formats = listOf(
                FormatOption("20x20", "20×20 · квадрат"),
                FormatOption("21x30", "21×30 · вертикальный", percent = 125, approximate = true),
                FormatOption("30x21", "30×21 · горизонтальный", percent = 125, approximate = true),
            ),
            basePrice = 7000, basePages = 24, pageStepPrice = 200, maxPages = 100,
            photosHint = "От 20 до 200 фото",
            minPhotos = 20, maxPhotos = 200,
            priceNote = "Альбома нет на сайте PicPac: цена ориентировочная, по фотокниге Lite",
        ),
        Product(
            id = "lite",
            kind = ProductKind.BOOK,
            category = Category.BOOKS,
            title = "Фотокнига Lite",
            short = "Твёрдая глянцевая обложка",
            description = "Для годовых альбомов, детских рисунков и фото с телефона. Твёрдая глянцевая обложка с вашим фото.",
            art = "book",
            formats = listOf(
                FormatOption("20x20", "20×20"),
                FormatOption("26x26", "26×26", percent = 135, approximate = true),
                FormatOption("21x30", "21×30", percent = 125, approximate = true),
            ),
            basePrice = 7000, basePages = 24, pageStepPrice = 200, maxPages = 160,
            photosHint = "От 25 до 400 фото",
            minPhotos = 25, maxPhotos = 400,
            specs = listOf(
                "Количество фото" to "25–400", "Обложка" to "твёрдая глянцевая", "Листы" to "мелованная бумага",
                "Покрытие страниц" to "нет", "Толщина листа" to "0,12 мм, как глянцевый журнал", "Количество страниц" to "до 160",
                "Цветопередача" to "более естественные цвета", "Форзац" to "есть",
            ),
        ),
        Product(
            id = "classic",
            kind = ProductKind.BOOK,
            category = Category.BOOKS,
            title = "Фотокнига Classic",
            short = "Плотные развороты без шва",
            description = "Layflat-книга: разворот раскрывается ровно, фото не теряются в сгибе. Плотная бумага под ламинацией.",
            art = "layflat",
            formats = listOf(
                FormatOption("20x20", "20×20"),
                FormatOption("21x30", "21×30", percent = 125, approximate = true),
            ),
            basePrice = 8100, basePages = 20, pageStepPrice = 300, maxPages = 60,
            finishTitle = "Обложка",
            photosHint = "От 20 до 200 фото",
            minPhotos = 20, maxPhotos = 200,
            finishOptions = listOf("Глянцевая", "Зернистая"),
            specs = listOf(
                "Количество фото" to "20–200", "Обложка" to "твёрдая глянцевая или твёрдая с зернистым покрытием", "Листы" to "мелованная бумага",
                "Покрытие страниц" to "глянцевая или зернистая ламинация", "Толщина листа" to "0,28 мм, как тонкий картон", "Количество страниц" to "до 60",
                "Цветопередача" to "более естественные цвета", "Форзац" to "нет",
            ),
        ),
        Product(
            id = "premium",
            kind = ProductKind.BOOK,
            category = Category.BOOKS,
            title = "Фотокнига Premium",
            short = "Настоящая фотобумага",
            description = "Страницы из фотобумаги — для свадеб, юбилеев и подарков. Твёрдая глянцевая обложка с вашим фото.",
            art = "premium",
            formats = listOf(
                FormatOption("20x20", "20×20"),
                FormatOption("30x20", "30×20", percent = 130, approximate = true),
                FormatOption("25x25", "25×25", percent = 140, approximate = true),
                FormatOption("30x30", "30×30", percent = 170, approximate = true),
                FormatOption("21x30", "21×30", percent = 130, approximate = true),
            ),
            basePrice = 10050, basePages = 10, pageStepPrice = 700, maxPages = 60,
            photosHint = "От 15 до 200 фото",
            minPhotos = 15, maxPhotos = 200,
            specs = listOf(
                "Количество фото" to "15–200", "Обложка" to "твёрдая глянцевая", "Листы" to "матовая фотобумага",
                "Покрытие страниц" to "матовое", "Толщина листа" to "1 мм, как плотный картон", "Количество страниц" to "до 60",
                "Цветопередача" to "более точные цвета", "Форзац" to "нет",
            ),
        ),
    )

    fun byId(id: String): Product = products.first { it.id == id }
}

const val PROMO_CODE = "PICPAC10"
private const val PROMO_PERCENT = 10

object Pricing {
    fun format(product: Product, formatId: String): FormatOption =
        product.formats.firstOrNull { it.id == formatId } ?: product.formats.first()

    /** «от …» в каталоге. */
    fun startPrice(product: Product): Long = when (product.kind) {
        // «от 0,61 за фото», как на главной сайта, — это цифровая печать 10×15.
        ProductKind.PRINT -> Catalog.byId("print").formats.first { it.id == "10x15" }.printPrice * PrintType.DIGITAL.percent / 100
        ProductKind.COLLAGE, ProductKind.DOCS -> product.formats.minOf { it.printPrice }
        ProductKind.BOOK -> product.basePrice
    }

    /** Цена одного отпечатка с учётом вида печати. */
    fun printPrice(format: FormatOption, type: PrintType): Long = format.printPrice * type.percent / 100

    /** Сколько фото для документов влезает на лист 10×15 (без зазоров, лист можно повернуть). */
    fun docsPerSheet(format: FormatOption): Int {
        if (format.widthMm <= 0 || format.heightMm <= 0) return 1
        val upright = (100 / format.widthMm) * (150 / format.heightMm)
        val rotated = (150 / format.widthMm) * (100 / format.heightMm)
        return maxOf(upright, rotated, 1)
    }

    /** Цена одного экземпляра: для печати — набор из всех выбранных фото, для книги — одна книга. */
    fun unitPrice(product: Product, formatId: String, pages: Int, photos: Int, type: PrintType = PrintType.PHOTO): Long {
        val format = format(product, formatId)
        return when (product.kind) {
            ProductKind.PRINT -> printPrice(format, type) * photos.coerceAtLeast(1)
            // Коллаж печатается одним листом, сколько бы фото в нём ни было.
            ProductKind.COLLAGE -> printPrice(format, type)
            // Каждое фото на документы — отдельный лист 10×15.
            ProductKind.DOCS -> format.printPrice * photos.coerceAtLeast(1)
            ProductKind.BOOK -> {
                val extraSpreads = ((pages - product.basePages).coerceAtLeast(0)) / 2
                (product.basePrice + extraSpreads * product.pageStepPrice) * format.percent / 100
            }
        }
    }

    fun lineTotal(item: CartItem): Long {
        val product = Catalog.byId(item.productId)
        return unitPrice(product, item.formatId, item.pages, item.photos.size, item.printType) * item.copies
    }

    fun itemsTotal(cart: List<CartItem>): Long = cart.sumOf { lineTotal(it) }

    fun discount(itemsTotal: Long, promo: String?): Long =
        if (promo == PROMO_CODE) itemsTotal * PROMO_PERCENT / 100 else 0

    fun deliveryCost(delivery: Delivery, itemsAfterDiscount: Long): Long =
        if (delivery.freeFrom != null && itemsAfterDiscount >= delivery.freeFrom) 0 else delivery.price

    fun details(item: CartItem): String {
        val product = Catalog.byId(item.productId)
        val format = format(product, item.formatId).title
        return buildList {
            add(format)
            if (product.kind == ProductKind.BOOK) add("${item.pages} стр.")
            if (product.kind == ProductKind.COLLAGE) add(if (item.freeLayout) "свободная раскладка" else "макет на ${item.layout} фото")
            if (product.kind == ProductKind.PRINT || product.kind == ProductKind.COLLAGE) add(item.printType.title.lowercase() + " печать")
            Albums.byId(item.preset)?.takeIf { item.albumMode == AlbumMode.PRESET }?.let { add("макет «${it.title}»") }
            item.finish?.let { add(it.lowercase()) }
            if (product.kind == ProductKind.BOOK || product.kind == ProductKind.COLLAGE) add("«${item.template.title}»")
            add("${item.photos.size} фото")
        }.joinToString(", ")
    }
}
