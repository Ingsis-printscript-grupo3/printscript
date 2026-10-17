package printscript.formatter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

// los rangos los declara cada regla en el registry, y se validan al cargar la config
class FormatterRulesTest {
    @Test
    fun `no rule is active by default`() {
        assertTrue(FormatterRules().values.isEmpty())
    }

    @Test
    fun `accepts every valid value for line breaks after println`() {
        listOf(0, 1, 2).forEach { value ->
            val rules = FormatterRulesLoader.fromJson("""{ "line-breaks-after-println": $value }""")

            assertEquals(FormatterRules.of("line-breaks-after-println" to value), rules)
        }
    }

    @Test
    fun `rejects line breaks after println outside the 0 to 2 range`() {
        assertFailsWith<IllegalArgumentException> {
            FormatterRulesLoader.fromJson("""{ "line-breaks-after-println": 3 }""")
        }
        assertFailsWith<IllegalArgumentException> {
            FormatterRulesLoader.fromJson("""{ "line-breaks-after-println": -1 }""")
        }
    }

    @Test
    fun `rejects a negative indent`() {
        assertFailsWith<IllegalArgumentException> { FormatterRulesLoader.fromJson("""{ "indent-inside-if": -1 }""") }
    }

    @Test
    fun `rejects asking for the brace on both places at once`() {
        assertFailsWith<IllegalArgumentException> {
            FormatterRulesLoader.fromJson("""{ "if-brace-same-line": true, "if-brace-below-line": true }""")
        }
    }
}
