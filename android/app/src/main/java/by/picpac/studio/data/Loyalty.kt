package by.picpac.studio.data

/**
 * Уровни клиента: чем больше оплачено заказов, тем больший процент возвращается бонусами.
 * На сайте PicPac программы лояльности нет — правила придуманы для демо.
 */
enum class LoyaltyLevel(val title: String, val fromPaid: Long, val cashbackPercent: Int) {
    START("Старт", 0, 3),
    SILVER("Серебро", 15000, 5),
    GOLD("Золото", 40000, 7),
}

/**
 * Бонусы считаем в копейках, как и деньги: 1 бонус = 1 копейка, показываем в рублях.
 * Баланс = приветственные + начисленные за оплаченные заказы − списанные.
 */
object Loyalty {
    /** Приветственные бонусы каждому новому аккаунту — чтобы списание было видно на демо. */
    const val WELCOME: Long = 500

    /** Бонусами можно оплатить не больше этой доли товаров. */
    const val MAX_SPEND_PERCENT = 20

    private fun userOrders(state: AppState, userId: Long) = state.orders.filter { it.userId == userId }

    private fun isPaid(o: Order) = o.status != OrderStatus.AWAITING_PAYMENT

    /** Сколько денег клиент уже заплатил за оплаченные заказы. */
    fun paidSum(state: AppState, userId: Long): Long = userOrders(state, userId).filter(::isPaid).sumOf { it.total }

    fun level(paidSum: Long): LoyaltyLevel = LoyaltyLevel.entries.last { paidSum >= it.fromPaid }

    fun nextLevel(level: LoyaltyLevel): LoyaltyLevel? = LoyaltyLevel.entries.getOrNull(level.ordinal + 1)

    fun balance(state: AppState, userId: Long): Long {
        val orders = userOrders(state, userId)
        return (WELCOME + orders.sumOf { it.bonusEarned } - orders.sumOf { it.bonusSpent }).coerceAtLeast(0)
    }

    /** Сколько бонусов можно списать на заказ с такими товарами (после промокода). */
    fun maxSpend(balance: Long, itemsAfterDiscount: Long): Long =
        minOf(balance, itemsAfterDiscount * MAX_SPEND_PERCENT / 100).coerceAtLeast(0)

    /** Начисление за оплату: процент уровня от суммы, оплаченной деньгами (без доставки). */
    fun earned(level: LoyaltyLevel, paidForItems: Long): Long = paidForItems * level.cashbackPercent / 100
}
