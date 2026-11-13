package by.picpac.studio.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import by.picpac.studio.data.AppViewModel
import by.picpac.studio.ui.components.CatalogIcon
import by.picpac.studio.ui.components.LoaderOverlay
import by.picpac.studio.ui.components.LocalTour
import by.picpac.studio.ui.components.TourOverlay
import by.picpac.studio.ui.components.TourRegistry
import by.picpac.studio.ui.components.TourSteps
import by.picpac.studio.ui.components.tourTarget
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import by.picpac.studio.ui.screens.AuthScreen
import by.picpac.studio.ui.screens.CartScreen
import by.picpac.studio.ui.screens.CatalogScreen
import by.picpac.studio.ui.screens.ChatScreen
import by.picpac.studio.ui.screens.CheckoutScreen
import by.picpac.studio.ui.screens.HomeScreen
import by.picpac.studio.ui.screens.OnboardingScreen
import by.picpac.studio.ui.screens.OrderDetailsScreen
import by.picpac.studio.ui.screens.OrdersScreen
import by.picpac.studio.ui.screens.PaymentScreen
import by.picpac.studio.ui.screens.ProductScreen
import by.picpac.studio.ui.screens.ProfileScreen
import by.picpac.studio.ui.screens.SettingsScreen
import by.picpac.studio.ui.screens.EditorScreen
import by.picpac.studio.ui.screens.PreviewScreen
import by.picpac.studio.ui.screens.ThanksScreen

@Composable
fun PicPacApp(vm: AppViewModel) {
    val nav = vm.nav
    val screen = nav.current

    // Системная кнопка «Назад» ходит по нашему стеку экранов, а на корне закрывает приложение.
    // На вкладках «Каталог», «Корзина», «Профиль» «Назад» ведёт на главную, а не закрывает приложение.
    BackHandler(enabled = nav.stack.size > 1 || (screen.isTab && screen != Screen.Home)) {
        if (!nav.back()) nav.tab(Screen.Home)
    }

    // Обучение: экраны отмечают нужные кнопки через Modifier.tourTarget, а слой поверх их подсвечивает.
    val tour = remember { TourRegistry() }
    tour.onTap = { key -> vm.tourTapped(key, TourSteps.map { it.key }) }

    CompositionLocalProvider(LocalTour provides tour) { Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = { if (screen.isTab) BottomBar(vm) },
        ) { padding ->
            Box(
                Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .imePadding()
                    .fillMaxSize(),
            ) {
                when (screen) {
                    Screen.Onboarding -> OnboardingScreen(vm)
                    Screen.Home -> HomeScreen(vm)
                    Screen.Catalog -> CatalogScreen(vm)
                    Screen.Cart -> CartScreen(vm)
                    Screen.Profile -> ProfileScreen(vm)
                    Screen.Orders -> OrdersScreen(vm)
                    Screen.Chat -> ChatScreen(vm)
                    Screen.Checkout -> CheckoutScreen(vm)
                    Screen.Settings -> SettingsScreen(vm)
                    Screen.Editor -> EditorScreen(vm)
                    Screen.Preview -> PreviewScreen(vm)
                    is Screen.Product -> ProductScreen(vm, screen.id)
                    is Screen.Auth -> AuthScreen(vm, screen.thenCheckout, screen.startRegister)
                    is Screen.Payment -> PaymentScreen(vm, screen.orderId)
                    is Screen.Thanks -> ThanksScreen(vm, screen.orderId)
                    is Screen.OrderDetails -> OrderDetailsScreen(vm, screen.orderId)
                }
            }
        }
        if (vm.state.onboardingDone && !vm.state.tourDone && vm.loader == null) {
            TourOverlay(tour, vm.tourStep, onNext = {
                // «Далее» делает то же, что нажатие на подсвеченный элемент: открывает экран следующего шага.
                // Иначе следующей кнопки нет на экране и подсказка пропадает.
                when (TourSteps.getOrNull(vm.tourStep)?.key) {
                    "hero-catalog" -> vm.nav.tab(Screen.Catalog)
                    "product-album" -> vm.nav.go(Screen.Product("album"))
                    "album-mode" -> if (vm.state.draft?.productId == "album") vm.chooseCustomAlbum()
                }
                vm.nextTourStep(TourSteps.size)
            }, onSkip = { vm.finishTour() })
        }
        vm.loader?.let { LoaderOverlay(it) }
    } }
}

@Composable
private fun BottomBar(vm: AppViewModel) {
    val current = vm.nav.current
    val cartCount = vm.state.cart.size
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        TabItem("Главная", Icons.Filled.Home, current == Screen.Home) { vm.nav.tab(Screen.Home) }
        // Первый шаг обучения подсвечивает вкладку «Каталог».
        TabItem("Каталог", CatalogIcon, current == Screen.Catalog, modifier = Modifier.tourTarget("hero-catalog")) { vm.nav.tab(Screen.Catalog) }
        TabItem("Корзина", Icons.Filled.ShoppingCart, current == Screen.Cart, badge = cartCount) { vm.nav.tab(Screen.Cart) }
        TabItem("Профиль", Icons.Filled.Person, current == Screen.Profile) { vm.nav.tab(Screen.Profile) }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TabItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    badge: Int = 0,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        icon = {
            Box {
                Icon(icon, contentDescription = label)
                if (badge > 0) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 8.dp, y = (-6).dp)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (badge > 9) "9+" else badge.toString(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        },
        label = { Text(label) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    )
}
