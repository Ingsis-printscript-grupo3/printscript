package printscript.linter

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
import printscript.linter.rule.IdentifierFormatRule
import printscript.linter.rule.ReadInputCallArgumentRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LinterTest {
    private fun analyze(
        statements: List<Statement>,
        config: LinterRules = LinterRules(),
    ): List<Warning> {
        val warnings = mutableListOf<Warning>()
        Linter(config).analyze(statements.iterator(), warnings::add)
        return warnings
    }

    private fun pos() = Position(0, 0)

    private fun num(v: Double) = NumberLiteral(v, pos())

    private fun id(n: String) = Identifier(n, pos())

    private fun str(s: String) = StringLiteral(s, pos())

    @Test
    fun `camel case variable declaration is valid`() {
        val stmt = VariableDeclaration("myVar", "number", null, pos())
        val warnings = analyze(listOf(stmt), LinterRules(identifierFormat = "camel case"))
        assertEquals(0, warnings.size)
    }

    @Test
    fun `snake case variable declaration warns when camel case is expected`() {
        val stmt = VariableDeclaration("my_var", "number", null, pos())
        val warnings = analyze(listOf(stmt), LinterRules(identifierFormat = "camel case"))
        assertEquals(1, warnings.size)
        assertEquals("Identifier 'my_var' does not match format camel case", warnings[0].message)
    }

    @Test
    fun `snake case variable declaration is valid when snake case is expected`() {
        val stmt = VariableDeclaration("my_var", "number", null, pos())
        val warnings = analyze(listOf(stmt), LinterRules(identifierFormat = "snake case"))
        assertEquals(0, warnings.size)
    }

    @Test
    fun `camel case variable declaration warns when snake case is expected`() {
        val stmt = VariableDeclaration("myVar", "number", null, pos())
        val warnings = analyze(listOf(stmt), LinterRules(identifierFormat = "snake case"))
        assertEquals(1, warnings.size)
        assertEquals("Identifier 'myVar' does not match format snake case", warnings[0].message)
    }

    @Test
    fun `println with literal is valid`() {
        val stmt = PrintCall(num(5.0), pos())
        val warnings = analyze(listOf(stmt))
        assertEquals(0, warnings.size)
    }

    @Test
    fun `println with identifier is valid`() {
        val stmt = PrintCall(id("x"), pos())
        val warnings = analyze(listOf(stmt))
        assertEquals(0, warnings.size)
    }

    @Test
    fun `println with binary expression warns when rule is on`() {
        val expr = BinaryExpression(num(1.0), TokenType.PLUS, num(2.0), pos())
        val stmt = PrintCall(expr, pos())
        val warnings = analyze(listOf(stmt))
        assertEquals(1, warnings.size)
        assertEquals(
            "println can only be called with an identifier or a literal, not an expression",
            warnings[0].message,
        )
    }

    @Test
    fun `println with binary expression is valid when rule is off`() {
        val expr = BinaryExpression(num(1.0), TokenType.PLUS, num(2.0), pos())
        val stmt = PrintCall(expr, pos())
        val warnings = analyze(listOf(stmt), LinterRules(printCallArgumentsMustBeLiteralOrIdentifier = false))
        assertEquals(0, warnings.size)
    }

    @Test
    fun `readInput with literal is valid`() {
        val readInput = ReadInput(str("name:"), pos())
        val stmt = VariableDeclaration("name", "string", readInput, pos())
        val warnings = analyze(listOf(stmt))
        assertEquals(0, warnings.size)
    }

    @Test
    fun `readInput with identifier is valid`() {
        val readInput = ReadInput(id("prompt"), pos())
        val stmt = VariableDeclaration("name", "string", readInput, pos())
        val warnings = analyze(listOf(stmt))
        assertEquals(0, warnings.size)
    }

    @Test
    fun `readInput with binary expression warns when rule is on`() {
        val binExpr = BinaryExpression(str("Enter: "), TokenType.PLUS, id("suffix"), pos())
        val readInput = ReadInput(binExpr, pos())
        val stmt = VariableDeclaration("name", "string", readInput, pos())
        val warnings = analyze(listOf(stmt))
        assertEquals(1, warnings.size)
        assertEquals(
            "readInput can only be called with an identifier or a literal, not an expression",
            warnings[0].message,
        )
    }

    @Test
    fun `readInput with binary expression is valid when rule is off`() {
        val binExpr = BinaryExpression(str("Enter: "), TokenType.PLUS, id("suffix"), pos())
        val readInput = ReadInput(binExpr, pos())
        val stmt = VariableDeclaration("name", "string", readInput, pos())
        val warnings = analyze(listOf(stmt), LinterRules(readInputArgumentsMustBeLiteralOrIdentifier = false))
        assertEquals(0, warnings.size)
    }

    @Test
    fun `readInput in assignment and if statement warns`() {
        val binExpr = BinaryExpression(str("a"), TokenType.PLUS, str("b"), pos())
        val assign = Assignment("x", ReadInput(binExpr, pos()), pos())
        val ifStmt =
            IfStatement(
                condition = id("cond"),
                thenBranch = Block(listOf(assign), pos()),
                elseBranch = Block(listOf(PrintCall(ReadInput(binExpr, pos()), pos())), pos()),
                position = pos(),
            )
        val rule = ReadInputCallArgumentRule()
        val warnings = rule.check(ifStmt)
        assertEquals(2, warnings.size)
    }

    @Test
    fun `the warning points at the position of the statement that caused it`() {
        val stmt = VariableDeclaration("my_var", "number", null, Position(3, 7))

        val warnings = analyze(listOf(stmt))

        assertEquals(Position(3, 7), warnings[0].position)
    }

    @Test
    fun `keeps going after the first warning and returns every one of them`() {
        val statements =
            listOf(
                VariableDeclaration("my_var", "number", null, Position(1, 1)),
                VariableDeclaration("other_var", "number", null, Position(2, 1)),
                PrintCall(BinaryExpression(num(1.0), TokenType.PLUS, num(2.0), pos()), Position(3, 1)),
            )

        val warnings = analyze(statements)

        assertEquals(3, warnings.size)
        assertEquals(listOf(Position(1, 1), Position(2, 1), Position(3, 1)), warnings.map { it.position })
    }

    @Test
    fun `rejects an identifier format that no rule knows`() {
        assertFailsWith<IllegalArgumentException> { LinterRules(identifierFormat = "camelCase") }
    }

    @Test
    fun `rejects an unknown identifier format when the rule is built on its own`() {
        assertFailsWith<IllegalArgumentException> { IdentifierFormatRule("camelCase") }
    }
}
