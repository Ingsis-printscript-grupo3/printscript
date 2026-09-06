package printscript.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LanguageVersionTest {
    @Test
    fun `parses 1_0`() {
        assertEquals(LanguageVersion.V1_0, LanguageVersion.parse("1.0"))
    }

    @Test
    fun `parses 1_1`() {
        assertEquals(LanguageVersion.V1_1, LanguageVersion.parse("1.1"))
    }

    @Test
    fun `rejects an unknown version`() {
        val exception = assertFailsWith<IllegalArgumentException> { LanguageVersion.parse("2.0") }
        assert(exception.message!!.contains("2.0"))
    }

    @Test
    fun `label round-trips back to the same version`() {
        LanguageVersion.entries.forEach { version ->
            assertEquals(version, LanguageVersion.parse(version.label))
        }
    }
}
