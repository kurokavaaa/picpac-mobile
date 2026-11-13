package by.picpac.studio.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import by.picpac.studio.ui.theme.PicPacColors
import kotlin.math.roundToInt

/** Что показывает экран загрузки. id меняется на каждый запуск, чтобы полоска стартовала с нуля. */
data class LoaderState(
    val id: Long,
    val title: String,
    val subtitle: String? = null,
    val durationMs: Long,
)

/** Полноэкранная загрузка: котик бежит за хвостом, под ним полоска прогресса. Перехватывает нажатия. */
@Composable
fun LoaderOverlay(state: LoaderState) {
    val progress = remember(state.id) { Animatable(0f) }
    LaunchedEffect(state.id) {
        progress.animateTo(1f, tween(state.durationMs.toInt(), easing = FastOutSlowInEasing))
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.97f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        // Небольшое название сверху, под строкой состояния телефона.
        AppWordmark(
            Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 28.dp),
            height = 40.dp,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            ChasingCat(size = 112.dp)
            Spacer(Modifier.height(20.dp))
            // Цвет задаём явно: оверлей лежит вне Scaffold, и без этого в тёмной теме текст был чёрным.
            Text(
                state.title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(14.dp))
            CatProgressBar(progress.value, Modifier.width(230.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${(progress.value * 100).roundToInt()}%" + (state.subtitle?.let { " · $it" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun CatProgressBar(progress: Float, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(12.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(PicPacColors.Honey, MaterialTheme.colorScheme.primary))),
        )
    }
}
