package by.picpac.studio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import by.picpac.studio.data.AppViewModel
import by.picpac.studio.R
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import by.picpac.studio.data.CartItem
import by.picpac.studio.data.Catalog
import by.picpac.studio.data.Delivery
import by.picpac.studio.data.Loyalty
import by.picpac.studio.data.OrderStatus
import by.picpac.studio.data.PROMO_CODE
import by.picpac.studio.data.Pricing
import by.picpac.studio.ui.Screen
import by.picpac.studio.ui.components.CardBox
import by.picpac.studio.ui.components.CatPeek
import by.picpac.studio.ui.components.CatInBox
import by.picpac.studio.ui.components.NoteBox
import by.picpac.studio.ui.components.PhotoThumb
import by.picpac.studio.ui.components.PrimaryButton
import by.picpac.studio.ui.components.ProductArt
import by.picpac.studio.ui.components.ScreenHeader
import by.picpac.studio.ui.components.SecondaryButton
import by.picpac.studio.ui.components.SectionTitle
import by.picpac.studio.ui.components.SelectChip
import by.picpac.studio.ui.components.Stepper
import by.picpac.studio.ui.components.SummaryRow
import by.picpac.studio.ui.components.formatMoney

@Composable
fun CartScreen(vm: AppViewModel) {
    val cart = vm.state.cart
    var confirmClear by remember { mutableStateOf(false) }
    var notRegisteredWarning by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Корзина", onBack = { vm.nav.tab(Screen.Home) }) {
            if (cart.isNotEmpty()) {
                IconButton(onClick = { confirmClear = true }) { Icon(Icons.Filled.Delete, contentDescription = "Очистить корзину") }
            }
        }

        if (cart.isEmpty()) {
            EmptyState(
                title = "Корзина пока пуста",
                text = "Выберите фотопечать или фотокнигу — котик уже ждёт ваши снимки",
                action = "В каталог",
                art = { CatInBox(width = 290.dp) },
                onAction = { vm.nav.tab(Screen.Catalog) },
            )
            return@Column
        }

        val itemsTotal = Pricing.itemsTotal(cart)
        val discount = Pricing.discount(itemsTotal, vm.state.promo)

        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(cart, key = { it.id }) { item -> CartRow(vm, item) }
            item { PromoBlock(vm) }
            item {
                CardBox {
                    SummaryRow("Товары", formatMoney(itemsTotal))
                    if (discount > 0) SummaryRow("Скидка по промокоду", "−${formatMoney(discount)}")
                    SummaryRow("Итого (${cart.size} ${goodsWord(cart.size)})", formatMoney(itemsTotal - discount), bold = true)
                    Text(
                        "Доставка рассчитается при оформлении",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        PrimaryButton(
            "Оформить заказ →",
            onClick = {
                // Заказ можно оформить только после входа в аккаунт — без него сначала предупреждаем.
                if (vm.state.currentUser == null) notRegisteredWarning = true else vm.nav.go(Screen.Checkout)
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Очистить корзину?") },
            text = { Text("Все товары и выбранные фото будут убраны из корзины.") },
            confirmButton = { TextButton(onClick = { vm.clearCart(); confirmClear = false }) { Text("Очистить") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Отмена") } },
        )
    }

    if (notRegisteredWarning) {
        AlertDialog(
            onDismissRequest = { notRegisteredWarning = false },
            title = { Text("Вы не зарегистрированы") },
            text = {
                Text(
                    "Оформить заказ может только зарегистрированный пользователь: так мы сохраним заказ " +
                        "и сможем присылать статусы печати. Корзина не пропадёт.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    notRegisteredWarning = false
                    vm.nav.go(Screen.Auth(thenCheckout = true, startRegister = true))
                }) { Text("Зарегистрироваться") }
            },
            dismissButton = {
                TextButton(onClick = {
                    notRegisteredWarning = false
                    vm.nav.go(Screen.Auth(thenCheckout = true))
                }) { Text("У меня есть аккаунт") }
            },
        )
    }
}

private fun goodsWord(n: Int) = when {
    n % 10 == 1 && n % 100 != 11 -> "товар"
    n % 10 in 2..4 && n % 100 !in 12..14 -> "товара"
    else -> "товаров"
}

@Composable
private fun CartRow(vm: AppViewModel, item: CartItem) {
    val product = Catalog.byId(item.productId)
    CardBox {
        Row(verticalAlignment = Alignment.Top) {
            val preview = item.photos.firstOrNull()
            if (preview != null) {
                PhotoThumb(preview, Modifier.size(72.dp))
            } else {
                ProductArt(artFor(product), Modifier.size(72.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(product.title, style = MaterialTheme.typography.titleMedium)
                Text(Pricing.details(item), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Stepper(
                        value = item.copies.toString(),
                        onMinus = { vm.changeCopies(item.id, -1) },
                        onPlus = { vm.changeCopies(item.id, +1) },
                        minusEnabled = item.copies > 1,
                        plusEnabled = item.copies < 99,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(formatMoney(Pricing.lineTotal(item)), style = MaterialTheme.typography.titleMedium)
                }
            }
            IconButton(onClick = { vm.removeFromCart(item.id) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Убрать из корзины", Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun PromoBlock(vm: AppViewModel) {
    var code by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    CardBox {
        if (vm.state.promo != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Промокод ${vm.state.promo} · −10%", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = { vm.removePromo() }) { Text("Убрать") }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it; error = null },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Промокод") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    isError = error != null,
                )
                TextButton(onClick = { if (!vm.applyPromo(code)) error = "Такого промокода нет" }) { Text("Применить") }
            }
            Text(
                error ?: "Демо-промокод: $PROMO_CODE",
                style = MaterialTheme.typography.bodySmall,
                color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun EmptyState(title: String, text: String, action: String, art: (@Composable () -> Unit)? = null, onAction: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Своя иллюстрация (например, кот в корзинке для пустой корзины) или нарисованный кодом котик.
        if (art != null) art() else CatPeek(size = 150.dp)
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(text, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        PrimaryButton(action, onClick = onAction)
    }
}

@Composable
fun CheckoutScreen(vm: AppViewModel) {
    val user = vm.state.currentUser
    val cart = vm.state.cart
    var recipient by rememberSaveable { mutableStateOf("") }
    var delivery by rememberSaveable { mutableStateOf(Delivery.BELPOST) }
    var postIndex by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var useBonus by rememberSaveable { mutableStateOf(false) }

    if (user == null || cart.isEmpty()) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader("Оформление", onBack = { vm.nav.back() })
            EmptyState("Нечего оформлять", "Корзина пуста или вы вышли из аккаунта", "В каталог") { vm.nav.tab(Screen.Catalog) }
        }
        return
    }

    val itemsTotal = Pricing.itemsTotal(cart)
    val discount = Pricing.discount(itemsTotal, vm.state.promo)
    val deliveryCost = Pricing.deliveryCost(delivery, itemsTotal - discount)
    val bonusAvailable = Loyalty.maxSpend(vm.bonusBalance(), itemsTotal - discount)
    val bonus = if (useBonus) bonusAvailable else 0L

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Оформление", onBack = { vm.nav.back() })
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            SectionTitle("Получатель")
            OutlinedTextField(
                value = recipient,
                onValueChange = { recipient = it; error = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Имя и фамилия") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            )

            SectionTitle("Доставка")
            Delivery.entries.forEach { d ->
                val cost = Pricing.deliveryCost(d, itemsTotal - discount)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .border(
                            1.dp,
                            if (d == delivery) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            MaterialTheme.shapes.medium,
                        )
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { delivery = d }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = d == delivery, onClick = { delivery = d })
                    Column(Modifier.weight(1f)) {
                        Text(d.title, style = MaterialTheme.typography.titleSmall)
                        Text(
                            d.eta + (d.freeFrom?.let { " · бесплатно от ${formatMoney(it)}" } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(if (cost == 0L) "бесплатно" else formatMoney(cost), fontWeight = FontWeight.SemiBold)
                }
            }

            // Индекс — только цифры: буквы и символы просто не попадают в поле, максимум 6 цифр (формат РБ).
            OutlinedTextField(
                value = postIndex,
                onValueChange = { input -> postIndex = input.filter { it.isDigit() }.take(6); error = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Почтовый индекс") },
                placeholder = { Text("220030") },
                supportingText = { Text("6 цифр") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            )
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = address,
                onValueChange = { address = it; error = null },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(
                        when (delivery) {
                            Delivery.BELPOST -> "Город и адрес отделения Белпочты"
                            Delivery.EUROPOST -> "Город и адрес отделения Европочты"
                            Delivery.COURIER -> "Город, улица, дом, квартира"
                        },
                    )
                },
                minLines = 2,
            )

            if (bonusAvailable > 0) {
                SectionTitle("Бонусы")
                CardBox(onClick = { useBonus = !useBonus }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Списать ${formatMoney(bonusAvailable)}", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "На счёте ${formatMoney(vm.bonusBalance())} · можно оплатить до ${Loyalty.MAX_SPEND_PERCENT}% товаров",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        androidx.compose.material3.Switch(checked = useBonus, onCheckedChange = { useBonus = it })
                    }
                }
            }

            SectionTitle("Итого")
            CardBox {
                SummaryRow("Товары", formatMoney(itemsTotal))
                if (discount > 0) SummaryRow("Скидка", "−${formatMoney(discount)}")
                if (bonus > 0) SummaryRow("Бонусами", "−${formatMoney(bonus)}")
                SummaryRow("Доставка", if (deliveryCost == 0L) "бесплатно" else formatMoney(deliveryCost))
                SummaryRow("К оплате", formatMoney(itemsTotal - discount - bonus + deliveryCost), bold = true)
            }
            Spacer(Modifier.height(10.dp))
            NoteBox("Уведомления о заказе придут на ${user.notifyChannel}")
            Spacer(Modifier.height(8.dp))
            NoteBox("В производство заказ уйдёт только после оплаты")
            Spacer(Modifier.height(16.dp))
        }

        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(6.dp))
            }
            PrimaryButton(
                "Перейти к оплате",
                onClick = {
                    error = when {
                        recipient.trim().length < 2 -> "Укажите имя получателя"
                        postIndex.length != 6 -> "Почтовый индекс — ровно 6 цифр"
                        address.trim().length < 5 -> "Укажите адрес доставки"
                        else -> null
                    }
                    if (error == null) {
                        vm.createOrder(recipient, delivery, "$postIndex, ${address.trim()}", useBonus)?.let { vm.nav.replace(Screen.Payment(it.id)) }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun PaymentScreen(vm: AppViewModel, orderId: Long) {
    val order = vm.orderById(orderId)
    var method by rememberSaveable { mutableStateOf("Карта") }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Оплата", onBack = { vm.nav.back() })
        if (order == null) {
            EmptyState("Заказ не найден", "Возможно, демо-данные были сброшены", "На главную") { vm.nav.tab(Screen.Home) }
            return@Column
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            CardBox {
                Text("Заказ ${order.number}", style = MaterialTheme.typography.titleMedium)
                order.lines.forEach { line ->
                    Text("${line.title} — ${line.details}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(8.dp))
                SummaryRow("К оплате", formatMoney(order.total), bold = true)
            }

            if (order.status != OrderStatus.AWAITING_PAYMENT) {
                Spacer(Modifier.height(16.dp))
                NoteBox("Заказ уже оплачен — повторно деньги не списываются")
                Spacer(Modifier.height(16.dp))
                PrimaryButton("Отследить заказ", onClick = { vm.nav.replace(Screen.OrderDetails(order.id)) }, modifier = Modifier.fillMaxWidth())
                return@Column
            }

            SectionTitle("Способ оплаты")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Карта", "ЕРИП").forEach { m -> SelectChip(m, method == m, { method = m }) }
            }
            Spacer(Modifier.height(12.dp))
            NoteBox("Тестовый режим: деньги не списываются, данные карты вводить не нужно")
            order.paymentError?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (order.status == OrderStatus.AWAITING_PAYMENT) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                PrimaryButton(
                    "Оплатить ${formatMoney(order.total)}",
                    onClick = { vm.pay(order.id, approve = true) { ok -> if (ok) vm.nav.replace(Screen.Thanks(order.id)) } },
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = { vm.pay(order.id, approve = false) {} }, modifier = Modifier.fillMaxWidth()) {
                    Text("Проверить отказ банка (демо)")
                }
            }
        }
    }
}

@Composable
fun ThanksScreen(vm: AppViewModel, orderId: Long) {
    val order = vm.orderById(orderId)
    // Цвета по макету команды «Спасибо за заказ» (светлая и тёмная версии).
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val sheet = if (dark) Color(0xFF1D140F) else Color(0xFFFDF0E8)
    val card = if (dark) Color(0xFF2B1E16) else Color(0xFFFFF6EF)
    val cardBorder = if (dark) Color(0xFF514539) else Color(0xFFE3D2C0)
    val ink = if (dark) Color(0xFFF6EDE6) else Color(0xFF1F1A17)
    val muted = if (dark) Color(0xFFA89A8F) else Color(0xFF8C8079)
    val accent = if (dark) Color(0xFFEE7A4E) else Color(0xFFCD4E22)
    val note = if (dark) Color(0xFF403F2B) else Color(0xFFE9E2C8)
    val outline = if (dark) Color(0xFF6B5E52) else Color(0xFFCDBBAA)

    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(sheet)
                .padding(horizontal = 18.dp, vertical = 20.dp),
        ) {
            // Котик лежит на верхнем краю карточки: картинку рисуем после карточки, с нахлёстом.
            Box {
                Column(
                    Modifier
                        .padding(top = 150.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(card)
                        .border(1.dp, cardBorder, RoundedCornerShape(20.dp))
                        .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 20.dp),
                ) {
                    Text("Спасибо", color = ink, fontSize = 32.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold)
                    Text("за заказ!", color = accent, fontSize = 32.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Text("Мы уже начали его готовить.", color = ink, style = MaterialTheme.typography.bodyLarge)
                    if (order != null) {
                        Spacer(Modifier.height(4.dp))
                        Text("Номер заказа: ${order.number}", color = muted, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Image(
                    painterResource(if (dark) R.drawable.thanks_cat_dark else R.drawable.thanks_cat_light),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.BottomCenter,
                )
            }
            vm.state.currentUser?.let { user ->
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(note).padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(accent))
                    Spacer(Modifier.width(14.dp))
                    Text("Статусы заказа будем присылать на ${user.notifyChannel}", color = ink, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(50))
                    .background(accent)
                    .clickable { vm.nav.replace(Screen.OrderDetails(orderId)) },
                contentAlignment = Alignment.Center,
            ) {
                Text("Отследить заказ →", color = Color.White, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, outline, RoundedCornerShape(50))
                    .clickable { vm.nav.tab(Screen.Home) },
                contentAlignment = Alignment.Center,
            ) {
                Text("На главную", color = ink, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
