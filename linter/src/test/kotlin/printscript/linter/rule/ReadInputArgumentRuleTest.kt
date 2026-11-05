package printscript.linter.rule

import printscript.common.Position
import printscript.linter.expressionOf
import printscript.linter.statementOf
import printscript.linter.warningsOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReadInputArgumentRuleTest {
    private val rule = ReadInputArgumentRule()

    @Test
    fun `applies only to ReadInput`() {
        assertTrue(rule.appliesTo(expressionOf("""readInput("name?")""")))
        assertFalse(rule.appliesTo(statementOf("let name: string;")))
        assertFalse(rule.appliesTo(statementOf("""println("hello");""")))
        assertFalse(rule.appliesTo(expressionOf("\"name?\"")))
    }

    @Test
    fun `readInput with a string literal is valid`() {
        assertEquals(0, warningsOf(rule, """let name: string = readInput("name?");""").size)
    }

    @Test
    fun `readInput with an identifier is valid`() {
        assertEquals(0, warningsOf(rule, "let name: string = readInput(prompt);").size)
    }

    @Test
    fun `readInput with a number literal is valid`() {
        assertEquals(0, warningsOf(rule, "let name: string = readInput(42);").size)
    }

    @Test
    fun `readInput with a binary expression warns`() {
        val warnings = warningsOf(rule, """let name: string = readInput("a" + "b");""")

        assertEquals(1, warnings.size)
        assertEquals(
            "readInput can only be called with an identifier or a literal, not an expression",
            warnings[0].message,
        )
    }

    @Test
    fun `the warning points at the position of the readInput call`() {
        val warnings = warningsOf(rule, "\n\n\nname = readInput(\"a\" + \"b\");")

        assertEquals(1, warnings.size)
        assertEquals(Position(4, 8), warnings[0].position)
    }

    @Test
    fun `the rule is off when the config disables it`() {
        val rules = LinterRuleRegistry().rulesFor(mapOf("mandatory-variable-or-literal-in-readInput" to "false"))

        assertTrue(rules.none { it is ReadInputArgumentRule })
    }

    @Test
    fun `the rule is on by default`() {
        val rules = LinterRuleRegistry().run { rulesFor(defaultConfig()) }

        assertTrue(rules.any { it is ReadInputArgumentRule })
    }

    @Test
    fun `check returns empty list for non matching node`() {
        assertEquals(emptyList(), rule.check(statementOf("let name: string;")))
        assertEquals(emptyList(), rule.check(statementOf("""println("hello");""")))
    }
}
