package printscript.linter.rule

import printscript.ast.BinaryExpression
import printscript.ast.BooleanLiteral
import printscript.ast.Expression
import printscript.ast.Identifier
import printscript.ast.NumberLiteral
import printscript.ast.PrintCall
import printscript.ast.ReadEnv
import printscript.ast.ReadInput
import printscript.ast.StringLiteral
import printscript.ast.VariableDeclaration
import printscript.common.Position
import printscript.common.TokenType
import kotlin.test.Test
import kotlin.test.assertEquals

private const val MESSAGE = "println can only be called with an identifier or a literal, not an expression"

class PrintCallArgumentRuleTest {
    private val rule = PrintCallArgumentRule()

    private fun pos() = Position(0, 0)

    private fun num(value: Double) = NumberLiteral(value, pos())

    private fun operation(operator: TokenType = TokenType.PLUS) = BinaryExpression(num(1.0), operator, num(2.0), pos())

    private fun print(value: Expression) = PrintCall(value, pos())

    @Test
    fun `println with a number literal is valid`() {
        assertEquals(0, rule.check(print(num(5.0))).size)
    }

    @Test
    fun `println with a string literal is valid`() {
        assertEquals(0, rule.check(print(StringLiteral("hola", pos()))).size)
    }

    @Test
    fun `println with a boolean literal is valid`() {
        assertEquals(0, rule.check(print(BooleanLiteral(true, pos()))).size)
    }

    @Test
    fun `println with an identifier is valid`() {
        assertEquals(0, rule.check(print(Identifier("x", pos()))).size)
    }

    @Test
    fun `println with a sum warns`() {
        val warnings = rule.check(print(operation()))

        assertEquals(1, warnings.size)
        assertEquals(MESSAGE, warnings[0].message)
    }

    // con la lista negra anterior estos dos se colaban, porque no son BinaryExpression
    @Test
    fun `println with a readInput warns`() {
        val warnings = rule.check(print(ReadInput(StringLiteral("name?", pos()), pos())))

        assertEquals(1, warnings.size)
        assertEquals(MESSAGE, warnings[0].message)
    }

    @Test
    fun `println with a readEnv warns`() {
        val warnings = rule.check(print(ReadEnv(StringLiteral("HOME", pos()), pos())))

        assertEquals(1, warnings.size)
        assertEquals(MESSAGE, warnings[0].message)
    }

    @Test
    fun `every binary operator warns, not just plus and minus`() {
        val operators = listOf(TokenType.PLUS, TokenType.MINUS, TokenType.MULTIPLY, TokenType.DIVIDE)

        operators.forEach { operator ->
            assertEquals(1, rule.check(print(operation(operator))).size, "operador $operator")
        }
    }

    @Test
    fun `the warning points at the position of the println`() {
        val statement = PrintCall(operation(), Position(7, 1))

        assertEquals(Position(7, 1), rule.check(statement)[0].position)
    }

    @Test
    fun `a statement that is not a println is ignored`() {
        assertEquals(0, rule.check(VariableDeclaration("total", "number", operation(), pos())).size)
    }
}
