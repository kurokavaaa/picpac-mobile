package by.picpac.studio.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import by.picpac.studio.R

/** Деньги храним в копейках (целое число), чтобы не ловить ошибки округления дробей. */
fun formatMoney(kopecks: Long): String =
    String.format(Locale("ru"), "%d,%02d руб.", kopecks / 100, kotlin.math.abs(kopecks % 100))

@Composable
fun ScreenHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = if (onBack != null) 4.dp else 20.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
            }
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        actions()
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 24.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * Плашка выбора: формат, бумага, фильтр. Выбранная закрашивается цветом текста бренда —
 * так выбор виден сразу, а красный остаётся только для главных кнопок.
 */
@Composable
fun SelectChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier
            .clip(shape)
            .background(if (selected) colors.onBackground else colors.surface)
            .border(1.dp, if (selected) colors.onBackground else colors.outline, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (selected) colors.background else colors.onSurface,
        )
    }
}

@Composable
fun Stepper(
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    minusEnabled: Boolean = true,
    plusEnabled: Boolean = true,
) {
    Row(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperButton("−", minusEnabled, onMinus)
        Text(value, Modifier.width(44.dp), style = MaterialTheme.typography.titleMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        StepperButton("+", plusEnabled, onPlus)
    }
}

/**
 * Счётчик, в который число можно и нажимать кнопками, и вписать с клавиатуры.
 * Вписанное значение применяется по «Готово» или когда поле теряет фокус:
 * приводится в диапазон [min, max] и через [normalize] (например, к чётному).
 */
@Composable
fun EditableStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    min: Int,
    max: Int,
    step: Int = 1,
    normalize: (Int) -> Int = { it },
) {
    // Текст поля живёт отдельно от числа, пока человек печатает; сбрасывается, если число поменяли кнопками.
    var text by remember(value) { mutableStateOf(value.toString()) }
    val focusManager = LocalFocusManager.current

    fun commit() {
        val fixed = normalize((text.toIntOrNull() ?: value).coerceIn(min, max)).coerceIn(min, max)
        text = fixed.toString()
        if (fixed != value) onValueChange(fixed)
    }

    Row(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperButton("−", value - step >= min) { onValueChange(value - step) }
        BasicTextField(
            value = text,
            onValueChange = { input -> text = input.filter { it.isDigit() }.take(4) },
            modifier = Modifier
                .width(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(vertical = 6.dp)
                .onFocusChanged { if (!it.isFocused) commit() },
            singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium.copy(
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                commit()
                focusManager.clearFocus()
            }),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        )
        StepperButton("+", value + step <= max) { onValueChange(value + step) }
    }
}

@Composable
private fun StepperButton(sign: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            sign,
            style = MaterialTheme.typography.titleLarge,
            color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        )
    }
}

/** Заголовок раздела: мелкие прописные буквы с разрядкой, как подписи в каталоге-журнале. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        modifier.padding(top = 24.dp, bottom = 10.dp),
        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = androidx.compose.ui.unit.TextUnit(0.12f, androidx.compose.ui.unit.TextUnitType.Em)),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Светлая карточка без обводки — отделяется от кремового фона только цветом. */
@Composable
fun CardBox(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth().let { if (onClick != null) it.clip(MaterialTheme.shapes.medium).clickable(onClick = onClick) else it },
        shape = MaterialTheme.shapes.medium,
        color = color,
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

/** Плашка-подсказка («демо», «уведомления придут…»). */
@Composable
fun NoteBox(text: String, modifier: Modifier = Modifier) {
    // Полоска слева вместо цветного «пузыря»: подсказка заметна, но не кричит.
    Row(modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min)) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
        Text(
            text,
            Modifier.padding(start = 12.dp, top = 2.dp, bottom = 2.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun SummaryRow(label: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            label,
            Modifier.padding(end = 16.dp),
            style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = if (bold) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // Длинное значение переносится справа, а не сжимает подпись слева.
        Text(
            value,
            Modifier.weight(1f, fill = false),
            style = if (bold) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
    }
}

@Composable
fun AddTile(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

/**
 * Название приложения обычным текстом. Фирменного логотипа в публичной версии нет:
 * это чужой товарный знак, а проект выложен как портфолио.
 */
@Composable
fun AppWordmark(modifier: Modifier = Modifier, height: Dp = 40.dp) {
    Text(
        stringResource(R.string.app_name),
        modifier = modifier,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        // Размер текста подстраиваем под высоту, которую раньше занимала картинка логотипа.
        fontSize = (height.value * 0.6f).sp,
    )
}

/** Иконка каталога «четыре квадрата»: в базовом наборе Material её нет, рисуем сами. */
val CatalogIcon: ImageVector by lazy {
    ImageVector.Builder("catalog", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.Black)) {
            listOf(3f to 3f, 13f to 3f, 3f to 13f, 13f to 13f).forEach { (x, y) ->
                moveTo(x + 1.5f, y)
                horizontalLineTo(x + 6.5f)
                quadTo(x + 8f, y, x + 8f, y + 1.5f)
                verticalLineTo(y + 6.5f)
                quadTo(x + 8f, y + 8f, x + 6.5f, y + 8f)
                horizontalLineTo(x + 1.5f)
                quadTo(x, y + 8f, x, y + 6.5f)
                verticalLineTo(y + 1.5f)
                quadTo(x, y, x + 1.5f, y)
                close()
            }
        }
    }.build()
}
