package by.picpac.studio.data

/**
 * Макет страницы: где на листе стоят ячейки под фото.
 * Ячейки — в долях листа (0..1) в формате Placement (центр + ширина/высота),
 * поэтому один и тот же макет подходит и квадратной, и вертикальной, и горизонтальной странице.
 */
data class PageLayout(val id: String, val cells: List<Placement>) {
    val count: Int get() = cells.size
}

/**
 * Библиотека макетов, как в редакторе super.picpac.by: на 1–7 фото, с белыми полями и «в край».
 * Координаты сняты со скриншотов сайта на глаз — близко к оригиналу, но не до миллиметра.
 */
object Layouts {
    /** Ячейка в процентах листа: левый край, верх, ширина, высота. Так удобнее переносить со скриншота. */
    private fun c(l: Float, t: Float, w: Float, h: Float) = Placement((l + w / 2) / 100f, (t + h / 2) / 100f, w / 100f, h / 100f)

    private fun l(id: String, vararg cells: Placement) = PageLayout(id, cells.toList())

    /** Ровная сетка rows × cols внутри рамки (в процентах), зазор между ячейками — gap. */
    private fun grid(id: String, rows: Int, cols: Int, left: Float, top: Float, width: Float, height: Float, gap: Float = 2f): PageLayout {
        val cw = (width - gap * (cols - 1)) / cols
        val ch = (height - gap * (rows - 1)) / rows
        return PageLayout(id, List(rows * cols) { i -> c(left + (i % cols) * (cw + gap), top + (i / cols) * (ch + gap), cw, ch) })
    }

    val all: List<PageLayout> = listOf(
        // ---------- 1 фото ----------
        l("1-full", c(0f, 0f, 100f, 100f)),
        l("1-margin", c(6f, 6f, 88f, 88f)),
        l("1-portrait", c(21f, 8f, 58f, 84f)),
        l("1-landscape", c(8f, 20f, 84f, 58f)),
        l("1-square", c(20f, 20f, 60f, 60f)),
        l("1-square-big", c(12f, 12f, 76f, 76f)),
        l("1-portrait-small", c(28f, 14f, 44f, 72f)),
        l("1-landscape-small", c(14f, 28f, 72f, 44f)),

        // ---------- 2 фото ----------
        grid("2-rows", 2, 1, 14f, 6f, 72f, 88f),
        grid("2-cols", 1, 2, 8f, 20f, 84f, 60f),
        grid("2-rows-narrow", 2, 1, 22f, 8f, 56f, 84f),
        grid("2-squares", 1, 2, 12f, 30f, 76f, 40f),
        grid("2-squares-stack", 2, 1, 30f, 8f, 40f, 84f),
        l("2-steps-up", c(0f, 26f, 49f, 74f), c(51f, 0f, 49f, 74f)),
        l("2-steps-down", c(0f, 0f, 49f, 74f), c(51f, 26f, 49f, 74f)),

        // ---------- 3 фото ----------
        l("3-big-left", c(0f, 8f, 49f, 70f), c(51f, 8f, 49f, 34f), c(51f, 44f, 49f, 34f)),
        l("3-big-right", c(0f, 8f, 49f, 34f), c(0f, 44f, 49f, 34f), c(51f, 8f, 49f, 70f)),
        l("3-two-one", c(22f, 0f, 27f, 52f), c(51f, 0f, 27f, 52f), c(22f, 54f, 56f, 46f)),
        l("3-one-two", c(22f, 0f, 56f, 46f), c(22f, 48f, 27f, 52f), c(51f, 48f, 27f, 52f)),
        grid("3-cols", 1, 3, 0f, 26f, 100f, 48f),
        grid("3-rows", 3, 1, 25f, 0f, 50f, 100f),
        l("3-stairs-down", c(0f, 16f, 32f, 34f), c(34f, 30f, 32f, 34f), c(68f, 44f, 32f, 34f)),
        l("3-stairs-up", c(0f, 44f, 32f, 34f), c(34f, 30f, 32f, 34f), c(68f, 16f, 32f, 34f)),
        l("3-diagonal", c(16f, 0f, 48f, 30f), c(26f, 35f, 48f, 30f), c(36f, 70f, 48f, 30f)),
        l("3-diagonal-back", c(36f, 0f, 48f, 30f), c(26f, 35f, 48f, 30f), c(16f, 70f, 48f, 30f)),

        // ---------- 4 фото ----------
        grid("4-grid", 2, 2, 0f, 0f, 100f, 100f),
        grid("4-grid-margin", 2, 2, 6f, 6f, 88f, 88f),
        grid("4-grid-small", 2, 2, 16f, 16f, 68f, 68f),
        l("4-mix-a", c(0f, 0f, 27f, 49f), c(29f, 0f, 71f, 49f), c(0f, 51f, 64f, 49f), c(66f, 51f, 34f, 49f)),
        l("4-mix-b", c(0f, 0f, 64f, 49f), c(66f, 0f, 34f, 49f), c(0f, 51f, 34f, 49f), c(36f, 51f, 64f, 49f)),
        l("4-cols-a", c(0f, 0f, 48f, 32f), c(0f, 34f, 48f, 66f), c(50f, 0f, 50f, 62f), c(50f, 64f, 50f, 36f)),
        l("4-cols-b", c(0f, 0f, 48f, 62f), c(0f, 64f, 48f, 36f), c(50f, 0f, 50f, 32f), c(50f, 34f, 50f, 66f)),
        grid("4-landscape", 2, 2, 12f, 26f, 76f, 48f),
        grid("4-portrait", 2, 2, 26f, 6f, 48f, 88f),
        l("4-big-left", c(0f, 0f, 64f, 100f), c(66f, 0f, 34f, 32f), c(66f, 34f, 34f, 32f), c(66f, 68f, 34f, 32f)),
        l("4-big-right", c(0f, 0f, 34f, 32f), c(0f, 34f, 34f, 32f), c(0f, 68f, 34f, 32f), c(36f, 0f, 64f, 100f)),
        l("4-big-top", c(0f, 0f, 100f, 64f), c(0f, 66f, 32f, 34f), c(34f, 66f, 32f, 34f), c(68f, 66f, 32f, 34f)),
        l("4-big-bottom", c(0f, 0f, 32f, 34f), c(34f, 0f, 32f, 34f), c(68f, 0f, 32f, 34f), c(0f, 36f, 100f, 64f)),
        l("4-wide-a", c(0f, 0f, 32f, 49f), c(34f, 0f, 66f, 49f), c(0f, 51f, 66f, 49f), c(68f, 51f, 32f, 49f)),
        l("4-wide-b", c(0f, 0f, 66f, 49f), c(68f, 0f, 32f, 49f), c(0f, 51f, 32f, 49f), c(34f, 51f, 66f, 49f)),

        // ---------- 5 фото ----------
        l("5-scatter", c(6f, 6f, 26f, 42f), c(6f, 52f, 26f, 42f), c(37f, 29f, 26f, 42f), c(68f, 6f, 26f, 42f), c(68f, 52f, 26f, 42f)),
        l("5-two-three", c(6f, 6f, 43f, 42f), c(51f, 6f, 43f, 42f), c(6f, 50f, 28f, 44f), c(36f, 50f, 28f, 44f), c(66f, 50f, 28f, 44f)),
        l("5-three-two", c(6f, 6f, 28f, 42f), c(36f, 6f, 28f, 42f), c(66f, 6f, 28f, 42f), c(6f, 50f, 43f, 44f), c(51f, 50f, 43f, 44f)),
        l("5-col-left", c(6f, 6f, 43f, 28f), c(6f, 36f, 43f, 28f), c(6f, 66f, 43f, 28f), c(51f, 6f, 43f, 58f), c(51f, 66f, 43f, 28f)),
        l("5-big-top-left", c(6f, 6f, 28f, 42f), c(36f, 6f, 58f, 42f), c(6f, 50f, 28f, 44f), c(36f, 50f, 28f, 44f), c(66f, 50f, 28f, 44f)),
        l("5-big-top-right", c(6f, 6f, 58f, 42f), c(66f, 6f, 28f, 42f), c(6f, 50f, 28f, 44f), c(36f, 50f, 28f, 44f), c(66f, 50f, 28f, 44f)),
        l("5-big-bottom", c(6f, 6f, 28f, 42f), c(36f, 6f, 28f, 42f), c(66f, 6f, 28f, 42f), c(6f, 50f, 58f, 44f), c(66f, 50f, 28f, 44f)),
        l("5-col-right", c(6f, 6f, 43f, 28f), c(51f, 6f, 43f, 28f), c(6f, 36f, 43f, 58f), c(51f, 36f, 43f, 28f), c(51f, 66f, 43f, 28f)),

        // ---------- 6 фото ----------
        l("6-big-left", c(0f, 0f, 66f, 66f), c(68f, 0f, 32f, 32f), c(68f, 34f, 32f, 32f), c(0f, 68f, 32f, 32f), c(34f, 68f, 32f, 32f), c(68f, 68f, 32f, 32f)),
        l("6-big-right", c(0f, 0f, 32f, 32f), c(0f, 34f, 32f, 32f), c(34f, 0f, 66f, 66f), c(0f, 68f, 32f, 32f), c(34f, 68f, 32f, 32f), c(68f, 68f, 32f, 32f)),
        l("6-big-center-a", c(0f, 0f, 32f, 32f), c(34f, 0f, 32f, 32f), c(68f, 0f, 32f, 32f), c(0f, 34f, 32f, 32f), c(34f, 34f, 66f, 66f), c(0f, 68f, 32f, 32f)),
        l("6-big-center-b", c(0f, 0f, 32f, 32f), c(34f, 0f, 32f, 32f), c(68f, 0f, 32f, 32f), c(0f, 34f, 66f, 66f), c(68f, 34f, 32f, 32f), c(68f, 68f, 32f, 32f)),
        grid("6-grid-3x2", 2, 3, 0f, 0f, 100f, 100f),
        grid("6-grid-2x3", 3, 2, 0f, 0f, 100f, 100f),
        grid("6-margin-3x2", 2, 3, 10f, 18f, 80f, 64f),
        grid("6-margin-2x3", 3, 2, 16f, 10f, 68f, 80f),

        // ---------- 7 фото ----------
        l(
            "7-mosaic",
            c(0f, 0f, 32f, 49f), c(34f, 0f, 32f, 32f), c(68f, 0f, 32f, 49f), c(34f, 34f, 32f, 32f),
            c(0f, 51f, 32f, 49f), c(34f, 68f, 32f, 32f), c(68f, 51f, 32f, 49f),
        ),
        l(
            "7-rows",
            c(0f, 0f, 49f, 32f), c(51f, 0f, 49f, 32f), c(0f, 34f, 32f, 32f), c(34f, 34f, 32f, 32f),
            c(68f, 34f, 32f, 32f), c(0f, 68f, 49f, 32f), c(51f, 68f, 49f, 32f),
        ),
        l(
            "7-four-three",
            c(0f, 12f, 23.5f, 37f), c(25.5f, 12f, 23.5f, 37f), c(51f, 12f, 23.5f, 37f), c(76.5f, 12f, 23.5f, 37f),
            c(0f, 51f, 23.5f, 37f), c(25.5f, 51f, 49f, 37f), c(76.5f, 51f, 23.5f, 37f),
        ),
        l(
            "7-three-four",
            c(0f, 12f, 23.5f, 37f), c(25.5f, 12f, 49f, 37f), c(76.5f, 12f, 23.5f, 37f),
            c(0f, 51f, 23.5f, 37f), c(25.5f, 51f, 23.5f, 37f), c(51f, 51f, 23.5f, 37f), c(76.5f, 51f, 23.5f, 37f),
        ),
        l(
            "7-center",
            c(8f, 8f, 26f, 26f), c(37f, 8f, 26f, 26f), c(66f, 8f, 26f, 26f), c(28f, 37f, 44f, 26f),
            c(8f, 66f, 26f, 26f), c(37f, 66f, 26f, 26f), c(66f, 66f, 26f, 26f),
        ),
        l(
            "7-columns",
            c(8f, 8f, 26f, 26f), c(8f, 37f, 26f, 26f), c(8f, 66f, 26f, 26f), c(37f, 28f, 26f, 44f),
            c(66f, 8f, 26f, 26f), c(66f, 37f, 26f, 26f), c(66f, 66f, 26f, 26f),
        ),
    )

    private val byIdMap = all.associateBy { it.id }

    const val MAX_PHOTOS = 7

    fun byCount(n: Int): List<PageLayout> = all.filter { it.count == n }

    /** Макет по умолчанию на n фото — тот, что ближе всего к прежней простой сетке. */
    fun default(n: Int): PageLayout = when (n.coerceIn(1, MAX_PHOTOS)) {
        1 -> byIdMap.getValue("1-margin")
        2 -> byIdMap.getValue("2-rows")
        3 -> byIdMap.getValue("3-one-two")
        4 -> byIdMap.getValue("4-grid-margin")
        5 -> byIdMap.getValue("5-two-three")
        6 -> byIdMap.getValue("6-grid-2x3")
        else -> byIdMap.getValue("7-rows")
    }

    /** Неизвестный id (например, из будущей версии) — не падаем, а берём макет на 1 фото. */
    fun byId(id: String): PageLayout = byIdMap[id] ?: default(1)
}
