package printscript.formatter

import printscript.formatter.rule.FormatterRuleRegistry
import kotlin.test.Test
import kotlin.test.assertFailsWith

// configs armadas en codigo, sin pasar por el loader: tienen que fallar en vez de dejar una regla apagada
class FormatterRulesValidatorTest {
    private val validator = FormatterRulesValidator(FormatterRuleRegistry())

    private fun assertRejected(config: FormatterRules) {
        assertFailsWith<IllegalArgumentException> { validator.check(config) }
    }

    @Test
    fun `accepts a valid config`() {
        validator.check(FormatterRules.of("if-brace-same-line" to true, "indent-inside-if" to 2))
    }

    @Test
    fun `rejects a misspelled key`() {
        assertRejected(FormatterRules.of("indent-insde-if" to 2))
    }

    @Test
    fun `rejects a value out of range`() {
        assertRejected(FormatterRules.of("line-breaks-after-println" to 5))
    }

    @Test
    fun `rejects an integer rule with a value of another type`() {
        assertRejected(FormatterRules.of("line-breaks-after-println" to "dos"))
    }

    @Test
    fun `rejects a boolean rule with a value of another type`() {
        assertRejected(FormatterRules.of("if-brace-same-line" to "true"))
    }

    @Test
    fun `rejects asking for the brace on both places at once`() {
        assertRejected(FormatterRules.of("if-brace-same-line" to true, "if-brace-below-line" to true))
    }

    @Test
    fun `rejects asking for spaces and no spaces around equals at once`() {
        assertRejected(
            FormatterRules.of("enforce-spacing-around-equals" to true, "enforce-no-spacing-around-equals" to true),
        )
    }

    @Test
    fun `the factory validates the config before creating the formatter`() {
        assertFailsWith<IllegalArgumentException> {
            FormatterFactory.create(FormatterRules.of("line-breaks-after-println" to 5))
        }
    }
}
