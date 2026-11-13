package by.picpac.studio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import by.picpac.studio.data.ThemeMode
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import by.picpac.studio.data.AppViewModel
import by.picpac.studio.ui.Screen
import by.picpac.studio.ui.components.CardBox
import by.picpac.studio.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.res.painterResource
import by.picpac.studio.ui.components.LoyaltyCard
import by.picpac.studio.data.Loyalty
import by.picpac.studio.ui.components.CatPeek
import by.picpac.studio.ui.components.NoteBox
import by.picpac.studio.ui.components.PrimaryButton
import by.picpac.studio.ui.components.ScreenHeader
import by.picpac.studio.ui.components.SecondaryButton

@Composable
fun ProfileScreen(vm: AppViewModel) {
    val user = vm.state.currentUser
    var linkEmail by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenHeader("Профиль", onBack = { vm.nav.tab(Screen.Home) })

        if (user == null) {
            Column(
                Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Котик выглядывает из-за края — как на макете «Спасибо, что создаёте воспоминания».
                Image(painterResource(R.drawable.cat_peek), contentDescription = null, modifier = Modifier.width(260.dp).aspectRatio(330f / 118f))
                Spacer(Modifier.height(12.dp))
                Text("Войдите в аккаунт", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Чтобы оформлять заказы, следить за печатью и получать уведомления",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(20.dp))
                PrimaryButton("Войти", onClick = { vm.nav.go(Screen.Auth()) }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                SecondaryButton(
                    "Зарегистрироваться",
                    onClick = { vm.nav.go(Screen.Auth(startRegister = true)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                CardBox {
                    MenuRow("Настройки", null) { vm.nav.go(Screen.Settings) }
                }
            }
        } else {
            val orders = vm.state.orders.count { it.userId == user.id }
            Column(Modifier.padding(horizontal = 20.dp)) {
                CardBox {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                (user.email ?: user.phone ?: "?").first().uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            user.email?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
                            user.phone?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                            Text(
                                "Уведомления: ${user.notifyChannel}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                LoyaltyCard(vm.bonusBalance(), vm.loyaltyLevel(), Loyalty.paidSum(vm.state, user.id))
                if (user.email == null) {
                    Spacer(Modifier.height(10.dp))
                    NoteBox("Email не привязан — уведомления приходят по SMS")
                    TextButton(onClick = { linkEmail = true }) { Text("Привязать email") }
                }
                Spacer(Modifier.height(12.dp))
                CardBox {
                    MenuRow("Мои заказы", if (orders > 0) orders.toString() else null) { vm.nav.go(Screen.Orders) }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    MenuRow("Поддержка и менеджер", null) { vm.nav.go(Screen.Chat) }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    MenuRow("Настройки", null) { vm.nav.go(Screen.Settings) }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    MenuRow("Как устроено демо", null) { showAbout = true }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    MenuRow("Выйти", null) { vm.logout() }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        TextButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Сбросить демо-данные", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(16.dp))
    }

    if (linkEmail) {
        LinkEmailDialog(
            vm = vm,
            title = "Привязать email",
            text = "Статусы заказов будут приходить на почту вместо SMS.",
            onDone = { linkEmail = false },
        )
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Сбросить демо-данные?") },
            text = { Text("Удалятся аккаунты, корзина, заказы и чат на этом телефоне. Удобно перед показом следующему проверяющему.") },
            confirmButton = { TextButton(onClick = { confirmReset = false; vm.resetDemo() }) { Text("Сбросить") } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Отмена") } },
        )
    }
    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("Как устроено демо") },
            text = {
                Text(
                    "Прототип работает без сервера: данные хранятся на телефоне и сохраняются после перезапуска. " +
                        "Оплата тестовая, деньги не списываются. Статусы заказа можно двигать кнопкой «Следующий этап». " +
                        "Ответы в чате — заглушки, вместо них подключится ИИ-помощник и менеджер maryrose.",
                )
            },
            confirmButton = { TextButton(onClick = { showAbout = false }) { Text("Понятно") } },
        )
    }
}

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val current = vm.state.themeMode
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader("Настройки", onBack = { vm.nav.back() })
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text("Тема оформления", Modifier.padding(top = 12.dp, bottom = 10.dp), style = MaterialTheme.typography.titleMedium)
            CardBox {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { vm.setTheme(mode) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = mode == current, onClick = { vm.setTheme(mode) })
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(mode.title, style = MaterialTheme.typography.bodyLarge)
                            if (mode == ThemeMode.SYSTEM) {
                                Text(
                                    "Светлая днём, тёмная — если так настроен телефон",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            NoteBox("Выбор сохраняется на телефоне и применяется сразу")
            Text("Обучение", Modifier.padding(top = 24.dp, bottom = 10.dp), style = MaterialTheme.typography.titleMedium)
            CardBox {
                MenuRow("Пройти обучение заново", null) { vm.restartTour() }
            }
        }
    }
}

@Composable
private fun MenuRow(title: String, badge: String?, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        badge?.let { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp)) }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
    }
}
