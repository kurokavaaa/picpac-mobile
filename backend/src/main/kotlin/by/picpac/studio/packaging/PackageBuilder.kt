package by.picpac.studio.packaging

import by.picpac.studio.common.TimeSource
import by.picpac.studio.photos.PhotoRecord
import by.picpac.studio.photos.PhotoStorage
import by.picpac.studio.photos.ProjectRecord
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Serializable
data class ManifestSheet(
    val sheet: Int,
    /** Штрихкод листа: по нему лист сканируют на каждом этапе производства. */
    val barcode: String,
    val file: String,
    /** Страница книги (для печати фото — null). */
    val page: Int?,
    val copies: Int,
)

@Serializable
data class PackageManifest(
    val orderNumber: String,
    val product: String,
    val format: String,
    val pages: Int,
    val photoCount: Int,
    val deliveryMethod: String,
    val createdAt: String,
    val sheets: List<ManifestSheet>,
)

/**
 * Пакетирование: после оплаты собираем ZIP для производства — фото + manifest.json со штрихкодами листов.
 * Повторный вызов ничего не пересобирает: пакет уже есть — отдаём его.
 */
class PackageBuilder(dataDir: Path, private val storage: PhotoStorage, private val time: TimeSource) {
    private val root: Path = dataDir.resolve("packages")
    private val json = Json { prettyPrint = true }

    fun build(orderNumber: String, deliveryMethod: String, project: ProjectRecord, photos: List<PhotoRecord>): Path {
        Files.createDirectories(root)
        val target = root.resolve("$orderNumber.zip")
        if (Files.exists(target)) return target

        val sheets = photos.mapIndexed { i, photo ->
            val sheet = i + 1
            val barcode = "${orderNumber.replace("-", "")}-${sheet.toString().padStart(3, '0')}"
            val ext = photo.storedName.substringAfterLast('.', "jpg")
            ManifestSheet(
                sheet = sheet,
                barcode = barcode,
                file = "photos/${sheet.toString().padStart(3, '0')}_$barcode.$ext",
                // Простой автомакет: фото равномерно раскладываются по страницам книги.
                page = if (project.pages > 0) i * project.pages / photos.size + 1 else null,
                copies = 1,
            )
        }
        val manifest = PackageManifest(
            orderNumber = orderNumber,
            product = project.product,
            format = project.format,
            pages = project.pages,
            photoCount = photos.size,
            deliveryMethod = deliveryMethod,
            createdAt = time.now().toString(),
            sheets = sheets,
        )

        // Пишем во временный файл и только потом переименовываем:
        // если сервер упадёт посередине, недоделанный ZIP не попадёт в производство.
        val temp = Files.createTempFile(root, orderNumber, ".zip.part")
        ZipOutputStream(Files.newOutputStream(temp)).use { zip ->
            sheets.zip(photos).forEach { (sheet, photo) ->
                zip.putNextEntry(ZipEntry(sheet.file))
                Files.copy(storage.path(photo.projectId, photo.storedName), zip)
                zip.closeEntry()
            }
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(json.encodeToString(PackageManifest.serializer(), manifest).toByteArray())
            zip.closeEntry()
        }
        Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE)
        return target
    }
}
