package by.picpac.studio

import by.picpac.studio.auth.Contacts
import by.picpac.studio.auth.PasswordHasher
import by.picpac.studio.common.ApiException
import by.picpac.studio.pricing.PriceCalculator
import by.picpac.studio.pricing.QuoteRequest
import by.picpac.studio.pricing.formatByn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PricingTest {
    private val calc = PriceCalculator()

    @Test
    fun `Classic 24 страницы — 81 рубль плюс две пары страниц по 3 рубля`() {
        val q = calc.quote(QuoteRequest("CLASSIC", "20x20", pages = 24))
        assertEquals(8700, q.itemsTotal)
        assertEquals("87,00 BYN", q.totalDisplay)
        assertFalse(q.approximate)
    }

    @Test
    fun `Белпочта бесплатна от 150 рублей, ниже — 5,90`() {
        val cheap = calc.quote(QuoteRequest("LITE", "20x20", delivery = "BELPOST"))
        assertEquals(590, cheap.deliveryCost)
        assertEquals(7590, cheap.total)

        val expensive = calc.quote(QuoteRequest("PREMIUM", "30x30", delivery = "BELPOST"))
        assertEquals(15075, expensive.itemsTotal) // 100,50 × 150% (условный коэффициент)
        assertEquals(0, expensive.deliveryCost)
        assertTrue(expensive.approximate)
    }

    @Test
    fun `печать фото считается по штукам`() {
        val q = calc.quote(QuoteRequest("PRINT", "10x15", quantity = 10, delivery = "EUROPOCHTA"))
        assertEquals(1300, q.itemsTotal)
        assertEquals(1890, q.total)
    }

    @Test
    fun `неверные страницы и формат дают понятную ошибку`() {
        assertEquals("PAGES_INVALID", assertFailsWith<ApiException> { calc.quote(QuoteRequest("LITE", "20x20", pages = 25)) }.code)
        assertEquals("PAGES_INVALID", assertFailsWith<ApiException> { calc.quote(QuoteRequest("LITE", "20x20", pages = 10)) }.code)
        assertEquals("FORMAT_UNKNOWN", assertFailsWith<ApiException> { calc.quote(QuoteRequest("LITE", "99x99")) }.code)
        assertEquals("QUANTITY_REQUIRED", assertFailsWith<ApiException> { calc.quote(QuoteRequest("PRINT", "10x15")) }.code)
    }

    @Test
    fun `формат денег`() {
        assertEquals("100,50 BYN", formatByn(10050))
        assertEquals("0,05 BYN", formatByn(5))
    }
}

class AuthUnitTest {
    @Test
    fun `пароль проверяется, а хеш каждый раз разный из-за соли`() {
        val hasher = PasswordHasher(iterations = 1_000)
        val h1 = hasher.hash("secret123")
        val h2 = hasher.hash("secret123")
        assertNotEquals(h1, h2)
        assertTrue(hasher.verify("secret123", h1))
        assertFalse(hasher.verify("Secret123", h1))
        assertFalse(hasher.verify("secret123", "мусор"))
    }

    @Test
    fun `телефоны Беларуси приводятся к одному виду`() {
        assertEquals("+375291234567", Contacts.normalizePhone("80291234567"))
        assertEquals("+375291234567", Contacts.normalizePhone("+375 (29) 123-45-67"))
        assertNull(Contacts.normalizePhone("12345"))
        assertNull(Contacts.normalizePhone("+7 999 123-45-67"))
    }

    @Test
    fun `email приводится к нижнему регистру, мусор отклоняется`() {
        assertEquals("anna@example.com", Contacts.normalizeEmail("  Anna@Example.COM "))
        assertNull(Contacts.normalizeEmail("anna@"))
        assertNull(Contacts.normalizeEmail("не почта"))
    }

    @Test
    fun `в логах контакты маскируются`() {
        assertEquals("a***@example.com", Contacts.mask("anna@example.com"))
        assertEquals("+375*****67", Contacts.mask("+375291234567"))
    }
}
