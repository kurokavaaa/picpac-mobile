package by.picpac.studio.photos

import by.picpac.studio.audit.AuditLog
import by.picpac.studio.common.TimeSource
import by.picpac.studio.common.badRequest
import by.picpac.studio.common.conflict
import by.picpac.studio.common.notFound
import by.picpac.studio.moderation.ImageModerator
import by.picpac.studio.pricing.PriceCalculator
import by.picpac.studio.pricing.ProductKind
import by.picpac.studio.pricing.PriceCatalog
import by.picpac.studio.pricing.QuoteRequest
import kotlinx.serialization.Serializable
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

@Serializable
data class CreateProjectRequest(
    val product: String,
    val format: String,
    val pages: Int? = null,
    val title: String = "Мой альбом",
)

@Serializable
data class PhotoDto(
    val id: Long,
    val originalName: String,
    val sizeBytes: Long,
    val verdict: String,
    val nsfwScore: Double?,
    val note: String,
)

@Serializable
data class ProjectDto(
    val id: Long,
    val product: String,
    val format: String,
    val pages: Int,
    val title: String,
    val createdAt: String,
    val photos: List<PhotoDto>,
)

/** Проект — то, что человек собирает до заказа: продукт, формат и загруженные фото. */
class ProjectService(
    private val projects: ProjectRepository,
    private val storage: PhotoStorage,
    private val moderator: ImageModerator,
    private val calculator: PriceCalculator,
    private val audit: AuditLog,
    private val time: TimeSource,
    /** Вернёт true, если по проекту уже есть заказ: тогда фото менять нельзя. */
    private val isOrdered: (projectId: Long) -> Boolean,
) {
    fun create(userId: Long, req: CreateProjectRequest): ProjectDto {
        val title = req.title.trim().ifEmpty { "Мой альбом" }.take(100)
        val isPrint = PriceCatalog.catalog.products.find { it.code == req.product.uppercase() }?.kind == ProductKind.PRINT
        // Проверяем продукт, формат и страницы тем же калькулятором, что и цену.
        val quote = calculator.quote(QuoteRequest(req.product, req.format, req.pages, quantity = if (isPrint) 1 else null))
        val id = projects.create(userId, quote.product, quote.format, quote.pages, title, time.now().toString())
        audit.record(userId, "create_project", "project", id, "ok")
        return get(userId, id)
    }

    fun list(userId: Long): List<ProjectDto> = projects.listByUser(userId).map { toDto(it) }

    fun get(userId: Long, projectId: Long): ProjectDto = toDto(owned(userId, projectId))

    fun addPhoto(userId: Long, projectId: Long, originalName: String, contentType: String, bytes: ByteArray): PhotoDto {
        val project = owned(userId, projectId)
        if (isOrdered(projectId)) throw conflict("PROJECT_LOCKED", "Заказ уже оформлен — фото в нём менять нельзя")

        val extension = ALLOWED_TYPES[contentType.lowercase()]
            ?: throw badRequest("PHOTO_TYPE", "Поддерживаются JPEG и PNG. Фото с iPhone (HEIC) сохраните как JPEG")
        if (bytes.isEmpty()) throw badRequest("PHOTO_EMPTY", "Файл пустой")
        if (bytes.size > MAX_PHOTO_BYTES) throw badRequest("PHOTO_TOO_LARGE", "Фото больше 20 МБ")
        if (projects.photos(projectId).size >= MAX_PHOTOS) {
            throw badRequest("PHOTO_LIMIT", "В одном проекте до $MAX_PHOTOS фото")
        }

        // Защита от «бомбы»: маленький файл может распаковаться в гигабайты пикселей.
        // Сначала читаем только размер из заголовка, без распаковки.
        val pixels = imagePixels(bytes) ?: throw badRequest("PHOTO_UNREADABLE", "Не получилось открыть фото. Попробуйте другой файл")
        if (pixels > MAX_PIXELS) throw badRequest("PHOTO_TOO_BIG", "Фото слишком большое по размеру в пикселях (больше 50 Мп)")

        // Проверяем, что это действительно картинка, а не файл с подменённым типом.
        val image = ImageIO.read(ByteArrayInputStream(bytes))
            ?: throw badRequest("PHOTO_UNREADABLE", "Не получилось открыть фото. Попробуйте другой файл")

        val result = moderator.check(image)
        val storedName = storage.save(project.id, extension, bytes)
        val safeName = originalName.substringAfterLast('/').substringAfterLast('\\').take(200).ifEmpty { "photo.$extension" }
        val id = projects.addPhoto(
            project.id, storedName, safeName, contentType.lowercase(), bytes.size.toLong(),
            result.verdict.name, result.nsfwScore, result.note, time.now().toString(),
        )
        audit.record(userId, "upload_photo", "photo", id, result.verdict.name)
        return PhotoDto(id, safeName, bytes.size.toLong(), result.verdict.name, result.nsfwScore, result.note)
    }

    /** Чужой проект отдаём как «не найден», а не «запрещено»: так не видно, что он вообще существует. */
    fun owned(userId: Long, projectId: Long): ProjectRecord =
        projects.find(projectId)?.takeIf { it.userId == userId } ?: throw notFound("Проект не найден")

    private fun toDto(p: ProjectRecord) = ProjectDto(
        id = p.id,
        product = p.product,
        format = p.format,
        pages = p.pages,
        title = p.title,
        createdAt = p.createdAt,
        photos = projects.photos(p.id).map { PhotoDto(it.id, it.originalName, it.sizeBytes, it.verdict, it.nsfwScore, it.note) },
    )

    companion object {
        const val MAX_PHOTO_BYTES = 20 * 1024 * 1024
        /** 50 Мп — больше любой камеры телефона, но далеко до «бомбы». */
        const val MAX_PIXELS = 50_000_000L

        /** Ширина × высота из заголовка картинки, без распаковки пикселей. null — не картинка. */
        fun imagePixels(bytes: ByteArray): Long? {
            val input = ImageIO.createImageInputStream(ByteArrayInputStream(bytes)) ?: return null
            input.use {
                val reader = ImageIO.getImageReaders(it).asSequence().firstOrNull() ?: return null
                return try {
                    reader.input = it
                    reader.getWidth(0).toLong() * reader.getHeight(0)
                } catch (e: Exception) {
                    null
                } finally {
                    reader.dispose()
                }
            }
        }
        const val MAX_PHOTOS = 500
        private val ALLOWED_TYPES = mapOf("image/jpeg" to "jpg", "image/jpg" to "jpg", "image/png" to "png")
    }
}
