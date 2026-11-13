package by.picpac.studio

import by.picpac.studio.photos.ProjectService
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.zip.CRC32
import java.util.zip.Deflater
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Защита от картинок-«бомб»: размер в пикселях читается из заголовка, без распаковки. */
class BombTest {

    /** PNG, у которого в заголовке указан размер width×height, а данных — на одну строку. */
    private fun pngWithHeader(width: Int, height: Int): ByteArray {
        val out = ByteArrayOutputStream()
        fun chunk(type: String, data: ByteArray) {
            val t = type.toByteArray()
            out.write(ByteBuffer.allocate(4).putInt(data.size).array())
            out.write(t)
            out.write(data)
            val crc = CRC32().apply { update(t); update(data) }
            out.write(ByteBuffer.allocate(4).putInt(crc.value.toInt()).array())
        }
        out.write(byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(), 13, 10, 26, 10))
        chunk("IHDR", ByteBuffer.allocate(13).putInt(width).putInt(height).put(8).put(2).put(0).put(0).put(0).array())
        val deflater = Deflater().apply { setInput(ByteArray(1 + width * 3)); finish() }
        val buf = ByteArray(1024)
        val packed = ByteArrayOutputStream()
        while (!deflater.finished()) packed.write(buf, 0, deflater.deflate(buf))
        chunk("IDAT", packed.toByteArray())
        chunk("IEND", ByteArray(0))
        return out.toByteArray()
    }

    @Test
    fun `обычное фото читается по заголовку`() {
        assertEquals(64L * 48, ProjectService.imagePixels(pngWithHeader(64, 48)))
    }

    @Test
    fun `бомба 100 Мп весит килобайты, но не проходит порог`() {
        val bomb = pngWithHeader(10_000, 10_000)
        assertTrue(bomb.size < 200_000, "файл-бомба маленький: ${bomb.size} байт")
        val pixels = ProjectService.imagePixels(bomb)!!
        assertTrue(pixels > ProjectService.MAX_PIXELS, "100 Мп должны быть больше порога ${ProjectService.MAX_PIXELS}")
    }

    @Test
    fun `не картинка — null`() {
        assertNull(ProjectService.imagePixels("это не картинка".toByteArray()))
    }
}
