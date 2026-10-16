package printscript.linter.rule

import printscript.ast.BinaryExpression
import printscript.ast.Identifier
import printscript.ast.NumberLiteral
import printscript.ast.PrintCall
import printscript.ast.ReadInput
import printscript.ast.StringLiteral
import printscript.ast.VariableDeclaration
import printscript.common.Position
import printscript.common.TokenType
import printscript.linter.LinterFactory
import printscript.linter.LinterRules
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReadInputArgumentRuleTest {
    private val rule = ReadInputArgumentRule()

    private fun pos() = Position(0, 0)

    private fun str(v: String) = StringLiteral(v, pos())

    private fun concat(
        left: String,
        right: String,
    ) = BinaryExpression(str(left), TokenType.PLUS, str(right), pos())

    @Test
    fun `applies only to ReadInput`() {
        val readInput = ReadInput(str("name?"), pos())
        val decl = VariableDeclaration("name", "string", null, pos())
        val print = PrintCall(str("hello"), pos())

        assertTrue(rule.appliesTo(readInput))
        assertFalse(rule.appliesTo(decl))
        assertFalse(rule.appliesTo(print))
        assertFalse(rule.appliesTo(str("name?")))
    }

    @Test
    fun `readInput with a string literal is valid`() {
        val readInput = ReadInput(str("name?"), pos())

        assertEquals(0, rule.check(readInput).size)
    }

    @Test
    fun `readInput with an identifier is valid`() {
        val readInput = ReadInput(Identifier("prompt", pos()), pos())

        assertEquals(0, rule.check(readInput).size)
    }

    @Test
    fun `readInput with a number literal is valid`() {
        val readInput = ReadInput(NumberLiteral(42.0, pos()), pos())

        assertEquals(0, rule.check(readInput).size)
    }

    @Test
    fun `readInput with a binary expression warns`() {
        val readInput = ReadInput(concat("a", "b"), pos())

        val warnings = rule.check(readInput)

        assertEquals(1, warnings.size)
        assertEquals(
            "readInput can only be called with an identifier or a literal, not an expression",
            warnings[0].message,
        )
    }

    @Test
    fun `the warning points at the position of the readInput call`() {
        val readInput = ReadInput(concat("a", "b"), Position(4, 18))

        val warnings = rule.check(readInput)

        assertEquals(1, warnings.size)
        assertEquals(Position(4, 18), warnings[0].position)
    }

    @Test
    fun `the rule is off when the config disables it`() {
        val config = LinterRules(readInputArgumentsMustBeLiteralOrIdentifier = false)
        val rules = LinterFactory.defaultRegistry().rulesFor(config)

        assertTrue(rules.none { it is ReadInputArgumentRule })
    }

    @Test
    fun `the rule is on by default`() {
        val rules = LinterFactory.defaultRegistry().rulesFor(LinterRules())

        assertTrue(rules.any { it is ReadInputArgumentRule })
    }

    @Test
    fun `check returns empty list for non matching node`() {
        val decl = VariableDeclaration("name", "string", null, pos())
        val print = PrintCall(str("hello"), pos())

        assertEquals(emptyList(), rule.check(decl))
        assertEquals(emptyList(), rule.check(print))
    }
}
