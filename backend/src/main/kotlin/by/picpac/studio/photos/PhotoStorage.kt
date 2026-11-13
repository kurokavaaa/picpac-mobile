package by.picpac.studio.photos

import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

/** Файлы фото лежат на диске: data/uploads/project-<id>/<случайное имя>. Оригинальное имя — только в базе. */
class PhotoStorage(dataDir: Path) {
    private val root: Path = dataDir.resolve("uploads")

    fun save(projectId: Long, extension: String, bytes: ByteArray): String {
        val dir = Files.createDirectories(root.resolve("project-$projectId"))
        // Случайное имя: пользователь не может подсунуть путь вида ../../secret.
        val name = "${UUID.randomUUID()}.$extension"
        Files.write(dir.resolve(name), bytes)
        return name
    }

    fun path(projectId: Long, storedName: String): Path = root.resolve("project-$projectId").resolve(storedName)
}
