package by.picpac.studio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import by.picpac.studio.data.Loyalty
import by.picpac.studio.data.LoyaltyLevel
import by.picpac.studio.ui.theme.PicPacColors

/**
 * Бонусы клиента: баланс, уровень и сколько осталось до следующего.
 * Тёмный блок цвета текста бренда — без градиентов, чтобы карточка выглядела как карта клиента, а не как баннер.
 */
@Composable
fun LoyaltyCard(balance: Long, level: LoyaltyLevel, paidSum: Long, modifier: Modifier = Modifier, compact: Boolean = false, onClick: (() -> Unit)? = null) {
    val next = Loyalty.nextLevel(level)
    val light = Color(0xFFF6EFE6)
    Column(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(PicPacColors.Ink)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 18.dp, vertical = if (compact) 14.dp else 18.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text("БОНУСЫ", style = MaterialTheme.typography.labelSmall, color = light.copy(alpha = 0.7f))
                Text(formatMoney(balance), style = MaterialTheme.typography.headlineSmall, color = light)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(level.title.uppercase(), style = MaterialTheme.typography.labelMedium, color = PicPacColors.Red)
                Text("${level.cashbackPercent}% с заказа", style = MaterialTheme.typography.bodySmall, color = light.copy(alpha = 0.8f))
            }
        }
        if (!compact) {
            Spacer(Modifier.height(14.dp))
            if (next != null) {
                val progress = ((paidSum - level.fromPaid).toFloat() / (next.fromPaid - level.fromPaid)).coerceIn(0f, 1f)
                Box(Modifier.fillMaxWidth().height(4.dp).clip(MaterialTheme.shapes.extraSmall).background(light.copy(alpha = 0.2f))) {
                    Box(Modifier.fillMaxWidth(progress).height(4.dp).background(PicPacColors.Red))
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "До уровня «${next.title}» (${next.cashbackPercent}%) — заказы ещё на ${formatMoney(next.fromPaid - paidSum)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = light.copy(alpha = 0.85f),
                )
            } else {
                Text("Максимальный уровень", style = MaterialTheme.typography.bodySmall, color = light.copy(alpha = 0.85f))
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Бонусами можно оплатить до ${Loyalty.MAX_SPEND_PERCENT}% заказа. Демо: на сайте PicPac такой программы пока нет",
                style = MaterialTheme.typography.labelSmall,
                color = light.copy(alpha = 0.6f),
            )
        }
    }
}
