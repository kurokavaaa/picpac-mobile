package by.picpac.studio.ui

import androidx.compose.runtime.mutableStateListOf

/** Все экраны приложения. Свой простой стек вместо библиотеки навигации: меньше зависимостей. */
sealed interface Screen {
    data object Onboarding : Screen
    data object Home : Screen
    data object Catalog : Screen
    data object Cart : Screen
    data object Profile : Screen
    data object Orders : Screen
    data object Chat : Screen
    data object Checkout : Screen
    data object Settings : Screen
    data object Editor : Screen
    data object Preview : Screen
    data class Product(val id: String) : Screen
    data class Auth(val thenCheckout: Boolean = false, val startRegister: Boolean = false) : Screen
    data class Payment(val orderId: Long) : Screen
    data class Thanks(val orderId: Long) : Screen
    data class OrderDetails(val orderId: Long) : Screen
}

val Screen.isTab: Boolean
    get() = this == Screen.Home || this == Screen.Catalog || this == Screen.Cart || this == Screen.Profile

class Navigator(start: Screen) {
    val stack = mutableStateListOf(start)

    val current: Screen get() = stack.last()

    fun go(screen: Screen) {
        stack.add(screen)
    }

    /** Заменить текущий экран: например, после оплаты «Назад» не должен вести обратно в оплату. */
    fun replace(screen: Screen) {
        stack[stack.lastIndex] = screen
    }

    fun back(): Boolean {
        if (stack.size <= 1) return false
        stack.removeAt(stack.lastIndex)
        return true
    }

    /** Переход на вкладку нижнего меню сбрасывает историю. */
    fun tab(screen: Screen) {
        stack.clear()
        stack.add(screen)
    }
}
