package by.picpac.studio.chat

import by.picpac.studio.audit.AuditLog
import by.picpac.studio.common.badRequest
import kotlinx.serialization.Serializable

@Serializable
data class ChatRequest(val message: String)

@Serializable
data class ChatReply(
    val reply: String,
    /** stub — ответ из заготовок. Когда подключат настоящий ИИ, здесь будет имя модели. */
    val source: String,
    /** true — приложение показывает кнопку «Позвать оператора». */
    val suggestHandoff: Boolean,
)

@Serializable
data class HandoffResponse(val ticketId: String, val message: String)

/**
 * Место для подключения ИИ-помощника. Сейчас — заглушка с готовыми ответами.
 * Чтобы подключить настоящую модель, достаточно новой реализации этого интерфейса.
 */
fun interface ChatAssistant {
    fun reply(message: String): ChatReply
}

class StubChatAssistant : ChatAssistant {
    private val answers = listOf(
        listOf("доставк", "почт", "курьер") to
            "Доставляем Белпочтой, Европочтой и курьером EMS. От 5,90 BYN, Белпочтой бесплатно при заказе от 150 BYN. Печать занимает 2–3 дня.",
        listOf("цен", "стоим", "сколько") to
            "Lite — от 70 BYN, Classic — от 81 BYN, Premium — от 100,50 BYN. Печать фото 10×15 — 1,30 BYN за штуку. Точная сумма считается при оформлении.",
        listOf("статус", "где заказ", "трек") to
            "Статус заказа виден в личном кабинете: там каждый этап — от печати до доставки, а после отправки появится трек-номер.",
        listOf("оплат", "карт") to
            "Оплатить можно картой онлайн. Заказ уходит в печать только после оплаты.",
        listOf("брак", "плох", "испорч") to
            "Если с печатью что-то не так, перепечатаем бесплатно. Нажмите «Позвать оператора» — он оформит перепечатку.",
    )

    override fun reply(message: String): ChatReply {
        val text = message.lowercase()
        val match = answers.firstOrNull { (keys, _) -> keys.any { it in text } }
        return if (match != null) {
            ChatReply(match.second, "stub", suggestHandoff = false)
        } else {
            ChatReply("Пока я знаю ответы не на всё. Позвать оператора? Он ответит в рабочее время.", "stub", suggestHandoff = true)
        }
    }
}

class ChatService(private val assistant: ChatAssistant, private val audit: AuditLog) {
    fun reply(req: ChatRequest): ChatReply {
        val message = req.message.trim()
        if (message.isEmpty() || message.length > 1000) {
            throw badRequest("MESSAGE_INVALID", "Сообщение должно быть от 1 до 1000 символов")
        }
        return assistant.reply(message)
    }

    /** Передача диалога живому сотруднику, если ИИ не справился. Сейчас — только номер обращения. */
    fun handoff(userId: Long): HandoffResponse {
        val id = audit.record(userId, "chat_handoff", "support", null, "queued")
        return HandoffResponse("SUP-$id", "Оператор подключится к чату. Номер обращения: SUP-$id")
    }
}
