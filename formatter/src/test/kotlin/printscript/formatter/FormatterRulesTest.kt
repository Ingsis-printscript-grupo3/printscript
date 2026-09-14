package printscript.formatter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull

class FormatterRulesTest {
    @Test
    fun `no rule is active by default`() {
        val rules = FormatterRules()

        assertFalse(rules.spaceBeforeColon)
        assertFalse(rules.spaceAfterColon)
        assertFalse(rules.spacingAroundEquals)
        assertFalse(rules.singleSpaceSeparation)
        assertNull(rules.lineBreaksAfterPrintln)
        assertNull(rules.indentInsideIf)
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
