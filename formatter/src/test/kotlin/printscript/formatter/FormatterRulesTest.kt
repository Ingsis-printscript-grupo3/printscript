package printscript.formatter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FormatterRulesTest {
    @Test
    fun `uses the defaults the TCK golden files expect`() {
        val rules = FormatterRules()

        assertFalse(rules.spaceBeforeColon)
        assertFalse(rules.spaceAfterColon)
        assertTrue(rules.spaceAroundAssignment)
        assertEquals(0, rules.lineBreaksAfterPrintln)
        assertTrue(rules.braceOnSameLine)
    }

    @Test
    fun `accepts every valid value for line breaks after println`() {
        listOf(0, 1, 2).forEach { value ->
            assertEquals(value, FormatterRules(lineBreaksAfterPrintln = value).lineBreaksAfterPrintln)
        }
    }

    @Test
    fun `rejects line breaks after println outside the 0 to 2 range`() {
        assertFailsWith<IllegalArgumentException> { FormatterRules(lineBreaksAfterPrintln = 3) }
        assertFailsWith<IllegalArgumentException> { FormatterRules(lineBreaksAfterPrintln = -1) }
    }

    @Test
    fun `the no spacing key wins over the spacing one`() {
        val rules = FormatterRules(spacingAroundEquals = true, noSpacingAroundEquals = true)

        assertFalse(rules.spaceAroundAssignment)
    }

    @Test
    fun `asking for the brace below moves it off the same line`() {
        assertFalse(FormatterRules(ifBraceBelowLine = true).braceOnSameLine)
    }

    @Test
    fun `rejects a negative indent`() {
        assertFailsWith<IllegalArgumentException> { FormatterRules(indentInsideIf = -1) }
    }

    @Test
    fun `rejects asking for the brace on both places at once`() {
        assertFailsWith<IllegalArgumentException> {
            FormatterRules(ifBraceSameLine = true, ifBraceBelowLine = true)
        }
    }
}
