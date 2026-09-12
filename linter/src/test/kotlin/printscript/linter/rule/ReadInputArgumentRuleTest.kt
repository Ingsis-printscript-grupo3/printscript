package printscript.linter.rule

import printscript.ast.Assignment
import printscript.ast.BinaryExpression
import printscript.ast.Block
import printscript.ast.Identifier
import printscript.ast.IfStatement
import printscript.ast.NumberLiteral
import printscript.ast.PrintCall
import printscript.ast.ReadInput
import printscript.ast.Statement
import printscript.ast.StringLiteral
import printscript.ast.VariableDeclaration
import printscript.common.Position
import printscript.common.TokenType
import printscript.linter.Linter
import printscript.linter.LinterRules
import printscript.linter.Warning
import kotlin.test.Test
import kotlin.test.assertEquals

class ReadInputArgumentRuleTest {
    private val rule = ReadInputArgumentRule()

    private fun pos() = Position(0, 0)

    private fun str(v: String) = StringLiteral(v, pos())

    private fun num(v: Double) = NumberLiteral(v, pos())

    private fun concat(
        left: String,
        right: String,
    ) = BinaryExpression(str(left), TokenType.PLUS, str(right), pos())

    @Test
    fun `readInput with a string literal is valid`() {
        val stmt = VariableDeclaration("name", "string", ReadInput(str("name?"), pos()), pos())

        assertEquals(0, rule.check(stmt).size)
    }

    @Test
    fun `readInput with an identifier is valid`() {
        val stmt = VariableDeclaration("name", "string", ReadInput(Identifier("prompt", pos()), pos()), pos())

        assertEquals(0, rule.check(stmt).size)
    }

    @Test
    fun `readInput with a binary expression warns`() {
        val stmt = VariableDeclaration("name", "string", ReadInput(concat("a", "b"), pos()), pos())

        val warnings = rule.check(stmt)

        assertEquals(1, warnings.size)
        assertEquals(
            "readInput can only be called with an identifier or a literal, not an expression",
            warnings[0].message,
        )
    }

    @Test
    fun `the warning points at the position of the readInput call, not of the statement`() {
        val readInput = ReadInput(concat("a", "b"), Position(4, 18))
        val stmt = VariableDeclaration("name", "string", readInput, Position(4, 1))

        assertEquals(Position(4, 18), rule.check(stmt)[0].position)
    }

    @Test
    fun `finds readInput inside an assignment`() {
        val stmt = Assignment("name", ReadInput(concat("a", "b"), pos()), pos())

        assertEquals(1, rule.check(stmt).size)
    }

    @Test
    fun `finds readInput inside a println`() {
        val stmt = PrintCall(ReadInput(concat("a", "b"), pos()), pos())

        assertEquals(1, rule.check(stmt).size)
    }

    @Test
    fun `finds readInput inside an if condition`() {
        val stmt = IfStatement(ReadInput(concat("a", "b"), pos()), Block(emptyList(), pos()), null, pos())

        assertEquals(1, rule.check(stmt).size)
    }

    @Test
    fun `finds readInput nested inside a binary expression`() {
        val value = BinaryExpression(num(1.0), TokenType.PLUS, ReadInput(concat("a", "b"), pos()), pos())
        val stmt = VariableDeclaration("total", "number", value, pos())

        assertEquals(1, rule.check(stmt).size)
    }

    @Test
    fun `finds a readInput nested inside the argument of another readInput`() {
        val inner = ReadInput(concat("a", "b"), Position(1, 20))
        val stmt = VariableDeclaration("name", "string", ReadInput(inner, Position(1, 1)), Position(1, 1))

        val warnings = rule.check(stmt)

        assertEquals(2, warnings.size)
        assertEquals(listOf(Position(1, 1), Position(1, 20)), warnings.map { it.position })
    }

    @Test
    fun `a declaration without an initializer is ignored`() {
        assertEquals(0, rule.check(VariableDeclaration("name", "string", null, pos())).size)
    }

    @Test
    fun `the rule is off when the config disables it`() {
        val stmt = VariableDeclaration("name", "string", ReadInput(concat("a", "b"), pos()), pos())

        val warnings = mutableListOf<Warning>()
        Linter(LinterRules(readInputArgumentsMustBeLiteralOrIdentifier = false))
            .analyze(listOf<Statement>(stmt).iterator(), warnings::add)

        assertEquals(0, warnings.size)
    }

    @Test
    fun `the rule is on by default`() {
        val stmt = VariableDeclaration("name", "string", ReadInput(concat("a", "b"), pos()), pos())

        val warnings = mutableListOf<Warning>()
        Linter().analyze(listOf<Statement>(stmt).iterator(), warnings::add)

        assertEquals(1, warnings.size)
    }
}
