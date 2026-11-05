package printscript.linter.rule

import printscript.common.Position
import printscript.linter.expressionOf
import printscript.linter.statementOf
import printscript.linter.warningsOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val MESSAGE = "println can only be called with an identifier or a literal, not an expression"

class PrintCallArgumentRuleTest {
    private val rule = PrintCallArgumentRule()

    @Test
    fun `applies only to PrintCall`() {
        assertTrue(rule.appliesTo(statementOf("println(5);")))
        assertFalse(rule.appliesTo(statementOf("let total: number = 1 + 2;")))
        assertFalse(rule.appliesTo(expressionOf("5")))
    }

    @Test
    fun `println with a number literal is valid`() {
        assertEquals(0, warningsOf(rule, "println(5);").size)
    }

    @Test
    fun `println with a string literal is valid`() {
        assertEquals(0, warningsOf(rule, """println("hola");""").size)
    }

    @Test
    fun `println with a boolean literal is valid`() {
        assertEquals(0, warningsOf(rule, "println(true);").size)
    }

    @Test
    fun `println with an identifier is valid`() {
        assertEquals(0, warningsOf(rule, "println(x);").size)
    }

    @Test
    fun `println with a sum warns`() {
        val warnings = warningsOf(rule, "println(1 + 2);")

        assertEquals(1, warnings.size)
        assertEquals(MESSAGE, warnings[0].message)
    }

    // con la lista negra anterior estos dos se colaban, porque no son BinaryExpression
    @Test
    fun `println with a readInput warns`() {
        val warnings = warningsOf(rule, """println(readInput("name?"));""")

        assertEquals(1, warnings.size)
        assertEquals(MESSAGE, warnings[0].message)
    }

    @Test
    fun `println with a readEnv warns`() {
        val warnings = warningsOf(rule, """println(readEnv("HOME"));""")

        assertEquals(1, warnings.size)
        assertEquals(MESSAGE, warnings[0].message)
    }

    @Test
    fun `every binary operator warns, not just plus and minus`() {
        listOf("+", "-", "*", "/").forEach { operator ->
            assertEquals(1, warningsOf(rule, "println(1 $operator 2);").size, "operador $operator")
        }
    }

    @Test
    fun `the warning points at the position of the println`() {
        val warnings = warningsOf(rule, "\n\n\n\n\n\nprintln(1 + 2);")

        assertEquals(Position(7, 1), warnings[0].position)
    }

    @Test
    fun `check returns empty list for non matching node`() {
        assertEquals(emptyList(), rule.check(statementOf("let total: number = 1 + 2;")))
        assertEquals(emptyList(), rule.check(expressionOf("5")))
    }
}
