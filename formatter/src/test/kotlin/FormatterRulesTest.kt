package printscript.formatter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FormatterRulesTest {
    @Test
    fun `uses sensible defaults when no values are provided`() {
        val rules = FormatterRules()

        assertEquals(false, rules.spaceBeforeColon)
        assertEquals(true, rules.spaceAfterColon)
        assertEquals(true, rules.spaceAroundAssignment)
        assertEquals(1, rules.lineBreaksBeforePrintln)
    }

    @Test
    fun `accepts every valid value for lineBreaksBeforePrintln`() {
        listOf(0, 1, 2).forEach { value ->
            val rules = FormatterRules(lineBreaksBeforePrintln = value)
            assertEquals(value, rules.lineBreaksBeforePrintln)
        }
    }

    @Test
    fun `rejects a lineBreaksBeforePrintln value outside the 0 to 2 range`() {
        assertFailsWith<IllegalArgumentException> { FormatterRules(lineBreaksBeforePrintln = 3) }
        assertFailsWith<IllegalArgumentException> { FormatterRules(lineBreaksBeforePrintln = -1) }
    }
}
