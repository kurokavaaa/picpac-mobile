package by.picpac.studio.pricing

import by.picpac.studio.common.badRequest
import kotlinx.serialization.Serializable

/*
 * Цены взяты с super.picpac.by 11.11.2026. Деньги храним в копейках (Long):
 * дробные числа (Double) копят ошибки округления, а в деньгах это недопустимо.
 * Где на сайте цены нет, ставим коэффициент и помечаем approximate = true.
 */

enum class ProductKind { BOOK, PRINT }

@Serializable
data class FormatOption(
    val code: String,
    val title: String,
    /** Наценка формата в процентах от базовой цены книги (100 = как 20×20). */
    val percent: Int = 100,
    /** Цена за одно фото — только для печати фото. */
    val unitPrice: Long? = null,
    val approximate: Boolean = false,
)

@Serializable
data class ProductInfo(
    val code: String,
    val title: String,
    val description: String,
    val kind: ProductKind,
    val basePages: Int? = null,
    val maxPages: Int? = null,
    val basePrice: Long? = null,
    /** Сколько стоят каждые следующие 2 страницы. */
    val pageStepPrice: Long? = null,
    val formats: List<FormatOption>,
)

@Serializable
data class DeliveryOption(val code: String, val title: String, val price: Long, val freeFrom: Long? = null)

@Serializable
data class Catalog(
    val currency: String,
    val source: String,
    val products: List<ProductInfo>,
    val delivery: List<DeliveryOption>,
)

@Serializable
data class QuoteRequest(
    val product: String,
    val format: String,
    val pages: Int? = null,
    val quantity: Int? = null,
    val delivery: String? = null,
)

@Serializable
data class QuoteLine(val title: String, val amount: Long)

@Serializable
data class Quote(
    val product: String,
    val format: String,
    val pages: Int,
    val quantity: Int,
    val lines: List<QuoteLine>,
    val itemsTotal: Long,
    val deliveryCost: Long,
    val total: Long,
    val totalDisplay: String,
    val currency: String = "BYN",
    /** true — в расчёте есть условные цены (на сайте их нет). */
    val approximate: Boolean,
)

object PriceCatalog {
    const val MAX_BOOK_PAGES = 100 // на сайте максимум не указан — условное ограничение
    const val MAX_PRINT_QUANTITY = 1000

    val catalog = Catalog(
        currency = "BYN",
        source = "super.picpac.by, 11.11.2026. Цены форматов с approximate=true условные.",
        products = listOf(
            ProductInfo(
                code = "LITE", title = "Lite", kind = ProductKind.BOOK,
                description = "Фотокнига с тонкими страницами: годовые альбомы, детские рисунки, фото с телефона",
                basePages = 24, maxPages = MAX_BOOK_PAGES, basePrice = 7000, pageStepPrice = 200,
                formats = listOf(
                    FormatOption("20x20", "20×20 см"),
                    FormatOption("26x26", "26×26 см", percent = 130, approximate = true),
                    FormatOption("21x30", "21×30 см (как A4)", percent = 120, approximate = true),
                ),
            ),
            ProductInfo(
                code = "CLASSIC", title = "Classic", kind = ProductKind.BOOK,
                description = "Layflat-книга с плотными разворотами: семейные альбомы, путешествия, выпускной",
                basePages = 20, maxPages = MAX_BOOK_PAGES, basePrice = 8100, pageStepPrice = 300,
                formats = listOf(
                    FormatOption("20x20", "20×20 см"),
                    FormatOption("21x30", "21×30 см (как A4)", percent = 120, approximate = true),
                ),
            ),
            ProductInfo(
                code = "PREMIUM", title = "Premium", kind = ProductKind.BOOK,
                description = "Книга на фотобумаге: свадьбы, юбилеи, подарки к важным датам",
                basePages = 10, maxPages = MAX_BOOK_PAGES, basePrice = 10050, pageStepPrice = 700,
                formats = listOf(
                    FormatOption("20x20", "20×20 см"),
                    FormatOption("30x20", "30×20 см (альбомный)", percent = 120, approximate = true),
                    FormatOption("25x25", "25×25 см", percent = 125, approximate = true),
                    FormatOption("30x30", "30×30 см", percent = 150, approximate = true),
                    FormatOption("21x30", "21×30 см (как A4)", percent = 120, approximate = true),
                ),
            ),
            ProductInfo(
                code = "PRINT", title = "Печать фото", kind = ProductKind.PRINT,
                description = "Классическая печать фотографий, цена за штуку",
                formats = listOf(
                    FormatOption("10x15", "10×15 см", unitPrice = 130),
                    FormatOption("15x20", "15×20 см", unitPrice = 160),
                    FormatOption("20x30", "20×30 см", unitPrice = 320),
                    FormatOption("30x45", "30×45 см (постер)", unitPrice = 2000),
                ),
            ),
        ),
        delivery = listOf(
            DeliveryOption("BELPOST", "Белпочта", price = 590, freeFrom = 15000),
            DeliveryOption("EUROPOCHTA", "Европочта", price = 590),
            DeliveryOption("COURIER", "Курьер EMS", price = 590),
        ),
    )
}

class PriceCalculator(private val catalog: Catalog = PriceCatalog.catalog) {

    fun quote(req: QuoteRequest): Quote {
        val product = catalog.products.find { it.code == req.product.uppercase() }
            ?: throw badRequest("PRODUCT_UNKNOWN", "Такого продукта нет")
        val format = product.formats.find { it.code == req.format }
            ?: throw badRequest("FORMAT_UNKNOWN", "Такого формата для «${product.title}» нет")

        val lines = mutableListOf<QuoteLine>()
        val pages: Int
        val quantity: Int
        val itemsTotal: Long

        when (product.kind) {
            ProductKind.BOOK -> {
                val base = product.basePages!!
                pages = req.pages ?: base
                if (pages < base || pages > product.maxPages!! || (pages - base) % 2 != 0) {
                    throw badRequest(
                        "PAGES_INVALID",
                        "Страниц: от $base до ${product.maxPages}, добавляются по 2",
                    )
                }
                quantity = 1
                val extraPairs = (pages - base) / 2
                val raw = product.basePrice!! + extraPairs * product.pageStepPrice!!
                // Целочисленное округление до копейки: +50 перед делением на 100.
                itemsTotal = (raw * format.percent + 50) / 100
                lines += QuoteLine("${product.title} ${format.title}, $base стр.", applyPercent(product.basePrice, format.percent))
                if (extraPairs > 0) {
                    lines += QuoteLine("Доп. страницы: ${pages - base}", itemsTotal - applyPercent(product.basePrice, format.percent))
                }
            }
            ProductKind.PRINT -> {
                pages = 0
                quantity = req.quantity ?: throw badRequest("QUANTITY_REQUIRED", "Укажите количество фото")
                if (quantity !in 1..PriceCatalog.MAX_PRINT_QUANTITY) {
                    throw badRequest("QUANTITY_INVALID", "Количество: от 1 до ${PriceCatalog.MAX_PRINT_QUANTITY}")
                }
                itemsTotal = format.unitPrice!! * quantity
                lines += QuoteLine("Печать ${format.title} × $quantity", itemsTotal)
            }
        }

        val deliveryCost = req.delivery?.let { code ->
            val option = catalog.delivery.find { it.code == code.uppercase() }
                ?: throw badRequest("DELIVERY_UNKNOWN", "Такого способа доставки нет")
            val free = option.freeFrom != null && itemsTotal >= option.freeFrom
            val cost = if (free) 0 else option.price
            lines += QuoteLine(if (free) "${option.title} (бесплатно от ${formatByn(option.freeFrom!!)})" else option.title, cost)
            cost
        } ?: 0

        val total = itemsTotal + deliveryCost
        return Quote(
            product = product.code,
            format = format.code,
            pages = pages,
            quantity = quantity,
            lines = lines,
            itemsTotal = itemsTotal,
            deliveryCost = deliveryCost,
            total = total,
            totalDisplay = formatByn(total),
            approximate = format.approximate,
        )
    }

    private fun applyPercent(amount: Long, percent: Int) = (amount * percent + 50) / 100
}

fun formatByn(kopecks: Long): String = "%d,%02d BYN".format(kopecks / 100, kopecks % 100)
