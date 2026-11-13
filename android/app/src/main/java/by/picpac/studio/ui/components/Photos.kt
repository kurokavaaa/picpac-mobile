package by.picpac.studio.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.MaterialTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Миниатюры выбранных фото. Грузим уменьшенную копию в фоне и кэшируем,
 * чтобы списки не тормозили и не съедали память полноразмерными снимками с камеры.
 */
private object ThumbCache {
    val cache = LruCache<String, ImageBitmap>(80)
}

@Composable
fun PhotoThumb(uri: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(ThumbCache.cache.get(uri), uri) {
        if (value == null) {
            value = withContext(Dispatchers.IO) { loadThumb(context, uri) }?.also { ThumbCache.cache.put(uri, it) }
        }
    }
    Box(modifier.clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceVariant)) {
        bitmap?.let {
            Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
        }
    }
}

private fun loadThumb(context: Context, uri: String, target: Int = 360): ImageBitmap? = try {
    val parsed = Uri.parse(uri)
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(parsed)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= target && bounds.outHeight / (sample * 2) >= target) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    context.contentResolver.openInputStream(parsed)?.use { BitmapFactory.decodeStream(it, null, options) }?.asImageBitmap()
} catch (e: Exception) {
    // Фото удалили из галереи или доступ истёк — показываем пустую плитку, а не падаем.
    null
}
