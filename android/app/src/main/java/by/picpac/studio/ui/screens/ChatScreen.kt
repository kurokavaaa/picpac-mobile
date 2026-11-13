package by.picpac.studio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import by.picpac.studio.data.AppViewModel
import by.picpac.studio.data.ChatAuthor
import by.picpac.studio.data.ChatMessage
import by.picpac.studio.ui.components.NoteBox
import by.picpac.studio.ui.components.ScreenHeader
import by.picpac.studio.ui.components.SelectChip

private val quickQuestions = listOf("Сроки печати", "Доставка", "Где мой заказ?", "Оплата", "Брак печати")

@Composable
fun ChatScreen(vm: AppViewModel) {
    var input by rememberSaveable { mutableStateOf("") }
    val messages = vm.state.chat
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) { vm.ensureGreeting() }
    // Новое сообщение — прокручиваем вниз, как в мессенджерах.
    LaunchedEffect(messages.size, vm.typing) {
        val count = messages.size + if (vm.typing) 1 else 0
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            "Поддержка",
            onBack = { vm.nav.back() },
            subtitle = if (vm.state.managerJoined) "Менеджер maryrose в чате" else "ИИ-помощник · менеджер maryrose",
        )
        NoteBox(
            "Демо: ответы-заглушки. Здесь подключится ИИ-помощник, а сложные вопросы — менеджер",
            Modifier.padding(horizontal = 20.dp),
        )
        LazyColumn(
            Modifier.weight(1f),
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(messages, key = { it.id }) { Bubble(it) }
            if (vm.typing) {
                item {
                    Text(
                        if (vm.state.managerJoined) "maryrose печатает…" else "Помощник печатает…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            quickQuestions.forEach { q -> SelectChip(q, selected = false, onClick = { vm.sendChat(q) }) }
        }
        if (!vm.state.managerJoined) {
            TextButton(onClick = { vm.callManager() }, modifier = Modifier.padding(horizontal = 8.dp)) {
                Text("Позвать менеджера")
            }
        }
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 6.dp) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Сообщение") },
                    maxLines = 4,
                    shape = RoundedCornerShape(24.dp),
                )
                IconButton(
                    onClick = {
                        vm.sendChat(input)
                        input = ""
                    },
                    enabled = input.isNotBlank(),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Отправить", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun Bubble(message: ChatMessage) {
    if (message.author == ChatAuthor.SYSTEM) {
        Text(
            message.text,
            Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        return
    }
    val mine = message.author == ChatAuthor.ME
    val colors = MaterialTheme.colorScheme
    val background = when (message.author) {
        ChatAuthor.ME -> colors.primary
        ChatAuthor.MANAGER -> colors.secondaryContainer
        else -> colors.surface
    }
    val textColor = when (message.author) {
        ChatAuthor.ME -> colors.onPrimary
        ChatAuthor.MANAGER -> colors.onSecondaryContainer
        else -> colors.onSurface
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
        Column(
            Modifier
                .widthIn(max = 290.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp, topEnd = 18.dp,
                        bottomStart = if (mine) 18.dp else 4.dp, bottomEnd = if (mine) 4.dp else 18.dp,
                    ),
                )
                .background(background)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            if (!mine) {
                Text(
                    if (message.author == ChatAuthor.MANAGER) "maryrose · менеджер" else "ИИ-помощник",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor.copy(alpha = 0.75f),
                )
                Spacer(Modifier.padding(top = 2.dp))
            }
            Text(message.text, color = textColor, style = MaterialTheme.typography.bodyMedium)
            Text(
                formatTime(message.time),
                Modifier.align(Alignment.End),
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.6f),
            )
        }
    }
}
