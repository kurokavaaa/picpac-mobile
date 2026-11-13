package by.picpac.studio.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Сохраняет всё состояние в один JSON-файл на телефоне. Без сервера и без библиотек:
 * org.json встроен в Android. Пишем во временный файл и переименовываем,
 * чтобы при обрыве записи не остаться с полуфайлом.
 */
class Storage(private val file: File) {

    fun load(): AppState = try {
        if (file.exists()) fromJson(JSONObject(file.readText())) else AppState()
    } catch (e: Exception) {
        // Битый файл не должен ронять приложение на старте: начинаем с чистого состояния.
        AppState()
    }

    @Synchronized
    fun save(state: AppState) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(toJson(state).toString())
        if (!tmp.renameTo(file)) {
            file.writeText(tmp.readText())
            tmp.delete()
        }
    }

    fun clear() {
        file.delete()
    }

    private fun toJson(s: AppState) = JSONObject().apply {
        put("onboardingDone", s.onboardingDone)
        put("tourDone", s.tourDone)
        put("dragHintDone", s.dragHintDone)
        put("currentUserId", s.currentUserId ?: JSONObject.NULL)
        put("cartToken", s.cartToken)
        put("promo", s.promo ?: JSONObject.NULL)
        put("managerJoined", s.managerJoined)
        put("themeMode", s.themeMode.name)
        put("users", JSONArray(s.users.map { u ->
            JSONObject().apply {
                put("id", u.id); put("email", u.email ?: JSONObject.NULL); put("phone", u.phone ?: JSONObject.NULL)
                put("passwordHash", u.passwordHash); put("salt", u.salt); put("createdAt", u.createdAt)
            }
        }))
        put("cart", JSONArray(s.cart.map { itemToJson(it) }))
        put("draft", s.draft?.let { itemToJson(it) } ?: JSONObject.NULL)
        put("orders", JSONArray(s.orders.map { o ->
            JSONObject().apply {
                put("id", o.id); put("number", o.number); put("userId", o.userId)
                put("lines", JSONArray(o.lines.map { l ->
                    JSONObject().apply {
                        put("title", l.title); put("details", l.details); put("photos", l.photos)
                        put("preview", l.preview ?: JSONObject.NULL); put("amount", l.amount)
                    }
                }))
                put("delivery", o.delivery.name); put("address", o.address); put("recipient", o.recipient)
                put("itemsTotal", o.itemsTotal); put("discount", o.discount); put("deliveryCost", o.deliveryCost); put("total", o.total)
                put("status", o.status.name)
                put("history", JSONObject().apply { o.history.forEach { (k, v) -> put(k.name, v) } })
                put("trackNumber", o.trackNumber ?: JSONObject.NULL); put("paymentError", o.paymentError ?: JSONObject.NULL)
                put("checkoutToken", o.checkoutToken); put("createdAt", o.createdAt)
                put("bonusSpent", o.bonusSpent); put("bonusEarned", o.bonusEarned)
            }
        }))
        put("chat", JSONArray(s.chat.map { m ->
            JSONObject().apply { put("id", m.id); put("author", m.author.name); put("text", m.text); put("time", m.time) }
        }))
    }

    private fun fromJson(j: JSONObject) = AppState(
        onboardingDone = j.optBoolean("onboardingDone"),
        tourDone = j.optBoolean("tourDone"),
        dragHintDone = j.optBoolean("dragHintDone"),
        currentUserId = j.optLongOrNull("currentUserId"),
        cartToken = j.optString("cartToken", newToken()),
        promo = j.optStringOrNull("promo"),
        managerJoined = j.optBoolean("managerJoined"),
        themeMode = runCatching { ThemeMode.valueOf(j.getString("themeMode")) }.getOrDefault(ThemeMode.SYSTEM),
        users = j.optJSONArray("users").objects().map { u ->
            User(
                id = u.getLong("id"), email = u.optStringOrNull("email"), phone = u.optStringOrNull("phone"),
                passwordHash = u.getString("passwordHash"), salt = u.getString("salt"), createdAt = u.getLong("createdAt"),
            )
        },
        cart = j.optJSONArray("cart").objects().map { itemFromJson(it) },
        draft = j.optJSONObject("draft")?.let { runCatching { itemFromJson(it) }.getOrNull() },
        orders = j.optJSONArray("orders").objects().map { o ->
            Order(
                id = o.getLong("id"), number = o.getString("number"), userId = o.getLong("userId"),
                lines = o.optJSONArray("lines").objects().map { l ->
                    OrderLine(l.getString("title"), l.getString("details"), l.getInt("photos"), l.optStringOrNull("preview"), l.getLong("amount"))
                },
                delivery = Delivery.valueOf(o.getString("delivery")), address = o.getString("address"), recipient = o.getString("recipient"),
                itemsTotal = o.getLong("itemsTotal"), discount = o.getLong("discount"), deliveryCost = o.getLong("deliveryCost"), total = o.getLong("total"),
                status = OrderStatus.valueOf(o.getString("status")),
                history = o.optJSONObject("history")?.let { h -> h.keys().asSequence().associate { OrderStatus.valueOf(it) to h.getLong(it) } } ?: emptyMap(),
                trackNumber = o.optStringOrNull("trackNumber"), paymentError = o.optStringOrNull("paymentError"),
                checkoutToken = o.getString("checkoutToken"), createdAt = o.getLong("createdAt"),
                bonusSpent = o.optLong("bonusSpent", 0), bonusEarned = o.optLong("bonusEarned", 0),
            )
        },
        chat = j.optJSONArray("chat").objects().map { m ->
            ChatMessage(m.getLong("id"), ChatAuthor.valueOf(m.getString("author")), m.getString("text"), m.getLong("time"))
        },
    )
}

private fun itemToJson(c: CartItem) = JSONObject().apply {
    put("id", c.id); put("productId", c.productId); put("formatId", c.formatId); put("pages", c.pages)
    put("finish", c.finish ?: JSONObject.NULL); put("copies", c.copies); put("photos", JSONArray(c.photos))
    put("layoutId", c.layoutId); put("freeLayout", c.freeLayout)
    put("printType", c.printType.name); put("albumMode", c.albumMode?.name ?: JSONObject.NULL); put("preset", c.preset)
    put("placements", placementsToJson(c.placements))
    put("template", c.template.name); put("noFrame", c.noFrame); put("layoutEdited", c.layoutEdited); put("coverTitle", c.coverTitle)
    put("bookPages", JSONArray(c.bookPages.map { p ->
        JSONObject().apply {
            put("layoutId", p.layoutId); put("photos", JSONArray(p.photos)); put("decorations", decorationsToJson(p.decorations))
            put("free", p.free); put("placements", placementsToJson(p.placements))
        }
    }))
    put("decorations", decorationsToJson(c.decorations))
}

private fun itemFromJson(c: JSONObject) = CartItem(
    id = c.getString("id"), productId = c.getString("productId"), formatId = c.getString("formatId"),
    pages = c.getInt("pages"), finish = c.optStringOrNull("finish"), copies = c.getInt("copies"),
    photos = c.optJSONArray("photos").strings(),
    // Старые сохранения хранили только число фото в сетке — берём макет по умолчанию на столько фото.
    layoutId = c.optStringOrNull("layoutId") ?: c.optInt("layout", 0).let { if (it > 0) Layouts.default(it).id else "" },
    printType = runCatching { PrintType.valueOf(c.getString("printType")) }.getOrDefault(PrintType.PHOTO),
    albumMode = c.optStringOrNull("albumMode")?.let { name -> AlbumMode.entries.firstOrNull { it.name == name } },
    preset = c.optString("preset", ""),
    freeLayout = c.optBoolean("freeLayout", false),
    placements = placementsFromJson(c.optJSONArray("placements")),
    template = runCatching { Template.valueOf(c.getString("template")) }.getOrDefault(Template.MINIMAL),
    layoutEdited = c.optBoolean("layoutEdited", false),
    noFrame = c.optBoolean("noFrame", false),
    coverTitle = c.optString("coverTitle", ""),
    bookPages = c.optJSONArray("bookPages").objects().map { p ->
        BookPage(
            layoutId = p.optStringOrNull("layoutId") ?: Layouts.default(p.optInt("layout", 1)).id,
            photos = p.optJSONArray("photos").strings(),
            decorations = decorationsFromJson(p.optJSONArray("decorations")),
            // Старые сохранения без этих полей открываются как обычная сетка.
            free = p.optBoolean("free", false),
            placements = placementsFromJson(p.optJSONArray("placements")),
        )
    },
    decorations = decorationsFromJson(c.optJSONArray("decorations")),
)

private fun placementsToJson(list: List<Placement>) = JSONArray(list.map { p ->
    JSONObject().apply { put("x", p.x.toDouble()); put("y", p.y.toDouble()); put("w", p.w.toDouble()); put("h", p.h.toDouble()) }
})

private fun placementsFromJson(a: JSONArray?) = a.objects().map { p ->
    Placement(p.getDouble("x").toFloat(), p.getDouble("y").toFloat(), p.getDouble("w").toFloat(), p.getDouble("h").toFloat())
}

private fun decorationsToJson(list: List<Decoration>) = JSONArray(list.map { d ->
    JSONObject().apply {
        put("id", d.id); put("type", d.type.name); put("text", d.text); put("sticker", d.sticker.name); put("color", d.color)
        put("x", d.x.toDouble()); put("y", d.y.toDouble()); put("size", d.size.toDouble())
        put("font", d.font?.name ?: JSONObject.NULL)
    }
})

private fun decorationsFromJson(a: JSONArray?) = a.objects().mapNotNull { d ->
    runCatching {
        Decoration(
            id = d.getString("id"), type = DecorType.valueOf(d.getString("type")), text = d.optString("text", ""),
            sticker = StickerKind.valueOf(d.getString("sticker")), color = d.getLong("color"),
            x = d.getDouble("x").toFloat(), y = d.getDouble("y").toFloat(), size = d.getDouble("size").toFloat(),
            // Неизвестный шрифт (например, из будущей версии) — просто шрифт шаблона.
            font = d.optStringOrNull("font")?.let { name -> TextFont.entries.firstOrNull { it.name == name } },
        )
    }.getOrNull()
}

private fun JSONArray?.objects(): List<JSONObject> = if (this == null) emptyList() else List(length()) { getJSONObject(it) }
private fun JSONArray?.strings(): List<String> = if (this == null) emptyList() else List(length()) { getString(it) }
private fun JSONObject.optStringOrNull(key: String): String? = if (isNull(key) || !has(key)) null else getString(key)
private fun JSONObject.optLongOrNull(key: String): Long? = if (isNull(key) || !has(key)) null else getLong(key)
