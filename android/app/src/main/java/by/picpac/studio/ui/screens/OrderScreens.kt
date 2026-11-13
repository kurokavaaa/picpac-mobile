package by.picpac.studio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import by.picpac.studio.data.AppViewModel
import by.picpac.studio.data.Order
import by.picpac.studio.data.OrderStatus
import by.picpac.studio.ui.Screen
import by.picpac.studio.ui.components.CardBox
import by.picpac.studio.ui.components.NoteBox
import by.picpac.studio.ui.components.PhotoThumb
import by.picpac.studio.ui.components.PrimaryButton
import by.picpac.studio.ui.components.ScreenHeader
import by.picpac.studio.ui.components.SecondaryButton
import by.picpac.studio.ui.components.SectionTitle
import by.picpac.studio.ui.components.SummaryRow
import by.picpac.studio.ui.components.formatMoney
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormat = SimpleDateFormat("d MMM, HH:mm", Locale("ru"))

internal fun formatTime(millis: Long): String = dateFormat.format(Date(millis))

@Composable
fun OrdersScreen(vm: AppViewModel) {
    val user = vm.state.currentUser
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Мои заказы", onBack = { vm.nav.back() })
        if (user == null) {
            EmptyState("Войдите в аккаунт", "Заказы привязаны к аккаунту", "Войти") { vm.nav.replace(Screen.Auth()) }
            return@Column
        }
        val orders = vm.state.orders.filter { it.userId == user.id }
        if (orders.isEmpty()) {
            EmptyState("Заказов пока нет", "Соберите первую фотокнигу или закажите печать фото", "В каталог") { vm.nav.tab(Screen.Catalog) }
            return@Column
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(orders, key = { it.id }) { order ->
                CardBox(onClick = { vm.nav.go(Screen.OrderDetails(order.id)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Заказ ${order.number}", style = MaterialTheme.typography.titleMedium)
                            Text(
                                formatTime(order.createdAt) + " · " + order.lines.joinToString { it.title },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                        Text(formatMoney(order.total), style = MaterialTheme.typography.titleSmall)
                    }
                    Spacer(Modifier.height(8.dp))
                    StatusChip(order.status)
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: OrderStatus) {
    val waiting = status == OrderStatus.AWAITING_PAYMENT
    Text(
        status.title,
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (waiting) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        style = MaterialTheme.typography.labelMedium,
        color = if (waiting) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
    )
}

@Composable
fun OrderDetailsScreen(vm: AppViewModel, orderId: Long) {
    val order = vm.orderById(orderId)
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(order?.let { "Заказ ${it.number}" } ?: "Заказ", onBack = { vm.nav.back() })
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
            CardBox(color = MaterialTheme.colorScheme.primaryContainer) {
                Text(order.status.title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(order.status.description, color = MaterialTheme.colorScheme.onPrimaryContainer)
                order.trackNumber?.let {
                    Spacer(Modifier.height(8.dp))
                    Text("Трек-номер: $it · ${order.delivery.title}", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            if (order.status == OrderStatus.AWAITING_PAYMENT) {
                Spacer(Modifier.height(12.dp))
                order.paymentError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                }
                PrimaryButton(
                    "Оплатить ${formatMoney(order.total)}",
                    onClick = { vm.nav.go(Screen.Payment(order.id)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            SectionTitle("Этапы")
            Timeline(order)

            SectionTitle("Состав заказа")
            CardBox {
                order.lines.forEach { line ->
                    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        line.preview?.let { PhotoThumb(it, Modifier.size(48.dp)) }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(line.title, style = MaterialTheme.typography.titleSmall)
                            Text(line.details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        // Отступ, чтобы «1 фото» из описания не слипалось с ценой в «11,30».
                        Text(formatMoney(line.amount), Modifier.padding(start = 12.dp))
                    }
                }
                Spacer(Modifier.height(6.dp))
                if (order.discount > 0) SummaryRow("Скидка", "−${formatMoney(order.discount)}")
                SummaryRow("Доставка · ${order.delivery.title}", if (order.deliveryCost == 0L) "бесплатно" else formatMoney(order.deliveryCost))
                SummaryRow("Итого", formatMoney(order.total), bold = true)
                Spacer(Modifier.height(4.dp))
                Text("${order.recipient}, ${order.address}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            vm.state.users.firstOrNull { it.id == order.userId }?.let {
                Spacer(Modifier.height(12.dp))
                NoteBox("Уведомление о каждом этапе отправляется на ${it.notifyChannel}")
            }

            if (order.status != OrderStatus.AWAITING_PAYMENT && order.status != OrderStatus.DELIVERED) {
                Spacer(Modifier.height(16.dp))
                SecondaryButton("Следующий этап (демо)", onClick = { vm.advanceOrder(order.id) }, modifier = Modifier.fillMaxWidth())
                Text(
                    "В реальной работе статус меняется сам: на каждом листе штрихкод, его сканируют на производстве",
                    Modifier.padding(top = 6.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            SecondaryButton("Написать в поддержку", onClick = { vm.nav.go(Screen.Chat) }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Вертикальная лента этапов, как у маркетплейсов: пройденные — цветные, будущие — серые. */
@Composable
private fun Timeline(order: Order) {
    val steps = OrderStatus.entries
    CardBox {
        steps.forEachIndexed { index, status ->
            val reached = order.history.containsKey(status)
            val isCurrent = status == order.status
            Row {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(if (isCurrent) 16.dp else 12.dp)
                            .clip(CircleShape)
                            .background(if (reached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                            .border(2.dp, if (reached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape),
                    )
                    if (index < steps.lastIndex) {
                        Box(
                            Modifier
                                .width(2.dp)
                                .height(34.dp)
                                .background(
                                    if (order.history.containsKey(steps[index + 1])) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline,
                                ),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f).padding(bottom = 8.dp)) {
                    Row {
                        Text(
                            status.title,
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            color = if (reached) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        order.history[status]?.let {
                            Text(formatTime(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (isCurrent) {
                        Text(status.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
