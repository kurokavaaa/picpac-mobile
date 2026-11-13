package by.picpac.studio.moderation

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.nio.FloatBuffer
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.exp

/**
 * NSFW-классификатор на ONNX Runtime (модель Falconsai/nsfw_image_detection, Apache 2.0).
 * Модель работает локально: без интернета, ключей и оплаты.
 *
 * Ожидаемая папка модели (как в репозитории на Hugging Face):
 *   model.onnx (или onnx/model.onnx), config.json (id2label), preprocessor_config.json (размер, mean, std).
 */
class OnnxNsfwModerator private constructor(
    private val env: OrtEnvironment,
    private val session: OrtSession,
    private val inputName: String,
    private val width: Int,
    private val height: Int,
    private val mean: FloatArray,
    private val std: FloatArray,
    private val nsfwIndex: Int,
    private val threshold: Double,
) : ImageModerator, AutoCloseable {

    override fun check(image: BufferedImage): ModerationResult {
        val input = preprocess(image)
        val score = OnnxTensor.createTensor(env, input, longArrayOf(1, 3, height.toLong(), width.toLong())).use { tensor ->
            session.run(mapOf(inputName to tensor)).use { result ->
                @Suppress("UNCHECKED_CAST")
                val logits = (result.get(0).value as Array<FloatArray>)[0]
                softmax(logits)[nsfwIndex].toDouble()
            }
        }
        return if (score >= threshold) {
            ModerationResult(Verdict.NEEDS_REVIEW, score, "Фото отправлено на проверку сотруднику")
        } else {
            ModerationResult(Verdict.APPROVED, score, "Автопроверка пройдена")
        }
    }

    /** Картинка → массив чисел в формате модели: 1×3×H×W, каналы RGB, нормализация (x/255 − mean) / std. */
    private fun preprocess(image: BufferedImage): FloatBuffer {
        val resized = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        resized.createGraphics().apply {
            setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            drawImage(image, 0, 0, width, height, null)
            dispose()
        }
        val plane = width * height
        val data = FloatArray(3 * plane)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val rgb = resized.getRGB(x, y)
                val i = y * width + x
                data[i] = (((rgb shr 16) and 0xFF) / 255f - mean[0]) / std[0]
                data[plane + i] = (((rgb shr 8) and 0xFF) / 255f - mean[1]) / std[1]
                data[2 * plane + i] = ((rgb and 0xFF) / 255f - mean[2]) / std[2]
            }
        }
        return FloatBuffer.wrap(data)
    }

    override fun close() {
        session.close()
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun load(dir: Path, threshold: Double): OnnxNsfwModerator {
            // Полная модель точнее, квантованная (int8) в 4 раза меньше — берём ту, что скачана.
            val modelFile = listOf("model.onnx", "onnx/model.onnx", "onnx/model_quantized.onnx")
                .map { dir.resolve(it) }
                .firstOrNull { Files.exists(it) }
                ?: error("В $dir нет model.onnx или onnx/model_quantized.onnx")

            val config = readJson(dir.resolve("config.json"))
            val nsfwIndex = config?.get("id2label")?.jsonObject?.entries
                ?.firstOrNull { it.value.jsonPrimitive.content.lowercase().contains("nsfw") }
                ?.key?.toInt()
                ?: 1

            val pre = readJson(dir.resolve("preprocessor_config.json"))
            val mean = pre?.get("image_mean")?.toFloats() ?: floatArrayOf(0.5f, 0.5f, 0.5f)
            val std = pre?.get("image_std")?.toFloats() ?: floatArrayOf(0.5f, 0.5f, 0.5f)
            val (w, h) = pre?.get("size")?.toSize() ?: (224 to 224)

            val env = OrtEnvironment.getEnvironment()
            val session = env.createSession(modelFile.toString(), OrtSession.SessionOptions())
            return OnnxNsfwModerator(env, session, session.inputNames.first(), w, h, mean, std, nsfwIndex, threshold)
        }

        private fun readJson(file: Path): JsonObject? =
            if (Files.exists(file)) json.parseToJsonElement(Files.readString(file)).jsonObject else null

        private fun JsonElement.toFloats() = jsonArray.map { it.jsonPrimitive.content.toFloat() }.toFloatArray()

        /** size бывает числом (224) или объектом ({"height": 224, "width": 224}). */
        private fun JsonElement.toSize(): Pair<Int, Int> = when (this) {
            is JsonPrimitive -> int to int
            is JsonObject -> {
                val h = this["height"]?.jsonPrimitive?.int ?: this["shortest_edge"]?.jsonPrimitive?.int ?: 224
                val w = this["width"]?.jsonPrimitive?.int ?: h
                w to h
            }
            else -> 224 to 224
        }

        private fun softmax(logits: FloatArray): FloatArray {
            val max = logits.max()
            val exps = logits.map { exp((it - max).toDouble()) }
            val sum = exps.sum()
            return exps.map { (it / sum).toFloat() }.toFloatArray()
        }
    }
}
