package by.picpac.studio.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import by.picpac.studio.data.AppViewModel
import by.picpac.studio.data.AuthResult
import by.picpac.studio.data.EmailCheck
import by.picpac.studio.data.checkEmail
import by.picpac.studio.ui.Screen
import by.picpac.studio.ui.components.NoteBox
import by.picpac.studio.ui.components.PrimaryButton
import by.picpac.studio.ui.components.ScreenHeader
import by.picpac.studio.ui.components.SelectChip
import kotlin.random.Random

/** Простая капча-пример. Своя, без внешних сервисов: в прототипе нечего отправлять наружу. */
private data class Captcha(val a: Int, val b: Int, val plus: Boolean) {
    val question get() = "$a ${if (plus) "+" else "−"} $b"
    val answer get() = if (plus) a + b else a - b

    companion object {
        fun next(): Captcha {
            val a = Random.nextInt(6, 20)
            val b = Random.nextInt(1, 6)
            return Captcha(a, b, Random.nextBoolean())
        }
    }
}

@Composable
fun AuthScreen(vm: AppViewModel, thenCheckout: Boolean, startRegister: Boolean) {
    var isRegister by rememberSaveable { mutableStateOf(startRegister) }
    var byPhone by rememberSaveable { mutableStateOf(false) }
    var login by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var captcha by remember { mutableStateOf(Captcha.next()) }
    var captchaInput by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var notRegistered by remember { mutableStateOf(false) }
    var askEmail by remember { mutableStateOf(false) }

    // Подсказка по email прямо при вводе — когда адрес уже похож на законченный (есть «@» и точка после неё).
    val emailHint = if (isRegister && !byPhone && login.substringAfter("@", "").contains(".")) {
        (checkEmail(login) as? EmailCheck.Error)?.message
    } else {
        null
    }

    fun finish() {
        when {
            thenCheckout -> vm.nav.replace(Screen.Checkout)
            !vm.nav.back() -> vm.nav.tab(Screen.Profile)
        }
    }

    fun submit() {
        error = null
        notRegistered = false
        if (isRegister && captchaInput.trim().toIntOrNull() != captcha.answer) {
            error = "Неверный ответ на проверку. Попробуйте ещё раз"
            captcha = Captcha.next()
            captchaInput = ""
            return
        }
        val result = if (isRegister) vm.register(byPhone, login, password) else vm.login(login, password)
        when (result) {
            is AuthResult.Error -> {
                error = result.message
                notRegistered = result.notRegistered
            }
            is AuthResult.Ok -> if (result.askEmail) askEmail = true else finish()
        }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(if (isRegister) "Регистрация" else "Вход", onBack = { vm.nav.back() })
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Logo()
            if (thenCheckout) {
                Spacer(Modifier.height(8.dp))
                NoteBox("Войдите или зарегистрируйтесь, чтобы оформить заказ и следить за его статусом")
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectChip("Вход", !isRegister, { isRegister = false; error = null })
                SelectChip("Регистрация", isRegister, { isRegister = true; error = null })
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectChip("Email", !byPhone, { byPhone = false; login = ""; error = null })
                SelectChip("Телефон", byPhone, { byPhone = true; login = "+375 "; error = null })
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = login,
                onValueChange = { login = it; error = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(if (byPhone) "Телефон" else "Email") },
                placeholder = { Text(if (byPhone) "+375 29 123-45-67" else "name@gmail.com") },
                isError = emailHint != null,
                supportingText = if (emailHint != null) ({ Text(emailHint) }) else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = if (byPhone) KeyboardType.Phone else KeyboardType.Email),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; error = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Пароль") },
                supportingText = if (isRegister) ({ Text("Не короче 6 символов") }) else null,
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            )

            if (isRegister) {
                Spacer(Modifier.height(12.dp))
                Text("Проверка, что вы не робот", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CaptchaImage(captcha.question)
                    IconButton(onClick = { captcha = Captcha.next(); captchaInput = "" }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Другой пример")
                    }
                    OutlinedTextField(
                        value = captchaInput,
                        onValueChange = { captchaInput = it.filter { c -> c.isDigit() || c == '-' }.take(3); error = null },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ответ") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
                if (byPhone) {
                    Spacer(Modifier.height(10.dp))
                    NoteBox("После регистрации предложим привязать email для уведомлений. Без email уведомления придут по SMS")
                }
            }

            error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            if (notRegistered && !isRegister) {
                Spacer(Modifier.height(4.dp))
                NoteBox("Такого аккаунта ещё нет. Зарегистрируйтесь — это займёт минуту")
                TextButton(onClick = {
                    isRegister = true
                    notRegistered = false
                    error = null
                }) { Text("Зарегистрироваться с этими данными") }
            }
            Spacer(Modifier.height(16.dp))
        }
        PrimaryButton(
            if (isRegister) "Зарегистрироваться" else "Войти",
            onClick = ::submit,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        )
    }

    if (askEmail) {
        LinkEmailDialog(
            vm = vm,
            title = "Привязать email?",
            text = "Пришлём на почту статусы заказа и чеки. Можно пропустить — тогда уведомления придут по SMS.",
            onDone = {
                askEmail = false
                finish()
            },
        )
    }
}

/** Пример на «зашумлённой» плашке, как у настоящих капч. */
@Composable
private fun CaptchaImage(text: String) {
    val lineColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    Box(
        Modifier
            .size(120.dp, 52.dp)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val random = Random(text.hashCode())
            repeat(6) {
                drawLine(
                    lineColor,
                    Offset(random.nextFloat() * size.width, random.nextFloat() * size.height),
                    Offset(random.nextFloat() * size.width, random.nextFloat() * size.height),
                    strokeWidth = 3f,
                )
            }
        }
        Text(
            "$text = ?",
            Modifier.rotate(-6f),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic,
            letterSpacing = 3.sp,
        )
    }
}

/** Диалог привязки email: после регистрации по телефону и из профиля. */
@Composable
internal fun LinkEmailDialog(vm: AppViewModel, title: String, text: String, onDone: () -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val currentError = error
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text(title) },
        text = {
            Column {
                Text(text)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; error = null },
                    label = { Text("Email") },
                    singleLine = true,
                    isError = currentError != null,
                    supportingText = if (currentError != null) ({ Text(currentError) }) else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                error = vm.linkEmail(email)
                if (error == null) onDone()
            }) { Text("Привязать") }
        },
        dismissButton = { TextButton(onClick = onDone) { Text("Позже") } },
    )
}
