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

    @Test
    fun `version 1_1 adds its own keywords`() {
        val added = LanguageVersion.V1_1.keywords - LanguageVersion.V1_0.keywords.keys

        assertEquals(
            setOf("const", "boolean", "true", "false", "if", "else", "readInput", "readEnv"),
            added.keys,
        )
    }

    // con estas palabras afuera, el lexer de 1.0 las lee como identificadores comunes
    @Test
    fun `version 1_0 only knows its own keywords`() {
        assertEquals(setOf("let", "println", "number", "string"), LanguageVersion.V1_0.keywords.keys)
    }

    @Test
    fun `version 1_1 keeps every 1_0 keyword`() {
        val v10 = LanguageVersion.V1_0.keywords

        assertEquals(v10, LanguageVersion.V1_1.keywords.filterKeys { it in v10 })
    }

    // el default es el conservador a proposito: si nadie elige, una feature de 1.1 falla
    // con un mensaje claro en vez de colarse sin que nadie la haya pedido
    @Test
    fun `the default version is the conservative one`() {
        assertEquals(LanguageVersion.V1_0, LanguageVersion.DEFAULT)
    }
}
