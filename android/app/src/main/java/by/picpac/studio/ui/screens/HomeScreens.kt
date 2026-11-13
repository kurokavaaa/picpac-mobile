package by.picpac.studio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import by.picpac.studio.data.AppViewModel
import by.picpac.studio.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import by.picpac.studio.data.Catalog
import by.picpac.studio.data.Pricing
import by.picpac.studio.data.Loyalty
import by.picpac.studio.data.PROMO_CODE
import by.picpac.studio.ui.components.LoyaltyCard
import by.picpac.studio.ui.components.tourTarget
import androidx.compose.material3.HorizontalDivider
import by.picpac.studio.data.Product
import by.picpac.studio.ui.Screen
import by.picpac.studio.ui.components.ArtKind
import by.picpac.studio.ui.components.CardBox
import by.picpac.studio.ui.components.CatPeek
import by.picpac.studio.ui.components.PrimaryButton
import by.picpac.studio.ui.components.ProductArt
import by.picpac.studio.ui.components.SectionTitle
import by.picpac.studio.ui.components.formatMoney
import by.picpac.studio.ui.components.AppWordmark
import by.picpac.studio.ui.theme.PicPacColors
import kotlinx.coroutines.delay

internal fun artFor(product: Product): ArtKind = when (product.art) {
    "prints" -> ArtKind.PRINTS
    "layflat" -> ArtKind.LAYFLAT
    "premium" -> ArtKind.PREMIUM
    "collage" -> ArtKind.COLLAGE
    else -> ArtKind.BOOK
}

/** Короткие подписи для плиток «Быстрого заказа», чтобы влезали без обрезки. */
internal fun quickTitle(product: Product): String = when (product.id) {
    "print" -> "Печать"
    "collage" -> "Коллаж"
    "docs" -> "Документы"
    "album" -> "Альбом"
    else -> product.title.removePrefix("Фотокнига ")
}

@Composable
internal fun Logo(modifier: Modifier = Modifier) {
    AppWordmark(modifier, height = 44.dp)
}

@Composable
fun OnboardingScreen(vm: AppViewModel) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Logo()
        Spacer(Modifier.height(24.dp))
        Text("Фотопродукция для ваших воспоминаний", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(10.dp))
        Text(
            "Печать фото, фотокниг и подарков в высоком качестве. Не нужно быть дизайнером — макет соберётся сам.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(28.dp))
        ProductArt(ArtKind.BOOK, Modifier.fillMaxWidth().weight(1f))
        Spacer(Modifier.height(28.dp))
        PrimaryButton(
            "Начать →",
            onClick = {
                vm.finishOnboarding()
                vm.nav.tab(Screen.Home)
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Баннер по макету команды: текст и кнопка — настоящие, справа — иллюстрация с котиком (светлая и тёмная версии). */
private data class Banner(
    val title: String,
    val subtitle: String,
    val action: String,
    val lightArt: Int,
    val darkArt: Int,
    val target: Screen,
)

@Composable
fun HomeScreen(vm: AppViewModel) {
    val user = vm.state.currentUser
    val lastOrder = user?.let { u -> vm.state.orders.firstOrNull { it.userId == u.id } }

    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Logo()
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { vm.nav.go(if (user != null) Screen.Orders else Screen.Auth()) }) {
                    Icon(Icons.Filled.Notifications, contentDescription = "Уведомления о заказах")
                }
            }
        }
        item { Banners(vm) }
        item {
            SectionTitle("Быстрый заказ", Modifier.padding(horizontal = 20.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(Catalog.products, key = { it.id }) { product ->
                    QuickTile(product, Modifier.width(86.dp)) { vm.nav.go(Screen.Product(product.id)) }
                }
            }
        }
        item {
            Spacer(Modifier.height(20.dp))
            PromoCard(Modifier.padding(horizontal = 20.dp)) { vm.nav.go(Screen.Product("print")) }
        }
        if (lastOrder != null) {
            item {
                SectionTitle("Ваш последний заказ", Modifier.padding(horizontal = 20.dp))
                CardBox(Modifier.padding(horizontal = 20.dp), onClick = { vm.nav.go(Screen.OrderDetails(lastOrder.id)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Заказ ${lastOrder.number}", style = MaterialTheme.typography.titleMedium)
                            Text(lastOrder.status.title, color = MaterialTheme.colorScheme.primary)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(20.dp))
            CardBox(Modifier.padding(horizontal = 20.dp), onClick = { vm.nav.go(Screen.Chat) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Есть вопрос?", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "ИИ-помощник ответит сразу, менеджер maryrose — если нужно",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun Banners(vm: AppViewModel) {
    val banners = listOf(
        Banner("Печатаем ваши истории быстро и красиво", "Фото, фотокниги и подарки", "Создать заказ", R.drawable.banner_1_light, R.drawable.banner_1_dark, Screen.Catalog),
        Banner("Создайте свою фотокнигу", "Готовые шаблоны или полный конструктор", "Собрать книгу", R.drawable.banner_2_light, R.drawable.banner_2_dark, Screen.Product("lite")),
        Banner("Стильные коллажи из ваших фото", "Готовые макеты на любой случай", "Создать коллаж", R.drawable.banner_3_light, R.drawable.banner_3_dark, Screen.Product("collage")),
        // Подарков в каталоге пока нет — кнопка ведёт в каталог.
        Banner("Подарки, которые хранят эмоции", "Фотосувениры для близких", "Выбрать подарок", R.drawable.banner_4_light, R.drawable.banner_4_dark, Screen.Catalog),
    )
    val pager = rememberPagerState { banners.size }
    LaunchedEffect(pager) {
        while (true) {
            delay(5000)
            pager.animateScrollToPage((pager.currentPage + 1) % banners.size)
        }
    }
    // Тему берём по фону приложения, а не системы: в настройках её можно выбрать вручную.
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    // Цвета сняты с макета, чтобы фон карточки совпал с фоном иллюстрации.
    val cardColor = if (dark) Color(0xFF2A1B14) else Color(0xFFFEECDE)
    val titleColor = if (dark) Color(0xFFF6EDE6) else Color(0xFF231A15)
    val subColor = if (dark) Color(0xFFD9CCC3) else Color(0xFF4A3F38)
    val buttonColor = if (dark) Color(0xFFDF6438) else Color(0xFFCF5A2E)

    Column {
        HorizontalPager(
            state = pager,
            contentPadding = PaddingValues(horizontal = 20.dp),
            pageSpacing = 12.dp,
            modifier = Modifier.padding(top = 8.dp),
        ) { page ->
            val b = banners[page]
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(184.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(cardColor)
                    .clickable { vm.nav.go(b.target) },
            ) {
                Box(Modifier.align(Alignment.BottomEnd).fillMaxWidth(0.5f)) {
                    Image(
                        painterResource(if (dark) b.darkArt else b.lightArt),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.FillWidth,
                    )
                    // Картинка вырезана из макета прямоугольником — левый край плавно растворяем в фон карточки.
                    Box(
                        Modifier
                            .matchParentSize()
                            .background(Brush.horizontalGradient(0f to cardColor, 0.22f to Color.Transparent)),
                    )
                }
                Column(
                    Modifier.fillMaxHeight().fillMaxWidth(0.58f).padding(start = 18.dp, top = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(b.title, color = titleColor, fontSize = 20.sp, lineHeight = 23.sp, fontWeight = FontWeight.Bold, maxLines = 3)
                        Spacer(Modifier.height(4.dp))
                        Text(b.subtitle, color = subColor, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                    }
                    Text(
                        "${b.action} →",
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(buttonColor)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                    )
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(banners.size) { i ->
                Box(
                    Modifier
                        .padding(3.dp)
                        .size(if (i == pager.currentPage) 18.dp else 7.dp, 7.dp)
                        .clip(CircleShape)
                        .background(
                            if (i == pager.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        ),
                )
            }
        }
    }
}

@Composable
private fun QuickTile(product: Product, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ProductArt(artFor(product), Modifier.fillMaxWidth().aspectRatio(1f))
        Spacer(Modifier.height(6.dp))
        Text(
            quickTitle(product),
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun PromoCard(modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(Brush.linearGradient(listOf(Color(0xFFC25E39), Color(0xFFD2925C))))
            .clickable(onClick = onClick)
            .padding(start = 20.dp, top = 16.dp, bottom = 16.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Фотопечать", color = Color.White, style = MaterialTheme.typography.titleLarge)
            Text(
                "от ${formatMoney(Pricing.startPrice(Catalog.byId("print")))} за фото",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        ProductArt(ArtKind.PRINTS, Modifier.width(96.dp).height(80.dp))
    }
}
