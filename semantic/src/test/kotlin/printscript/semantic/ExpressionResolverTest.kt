package printscript.semantic

import printscript.ast.BinaryExpression
import printscript.ast.Expression
import printscript.ast.Identifier
import printscript.ast.NumberLiteral
import printscript.ast.StringLiteral
import printscript.ast.registry.Registry
import printscript.common.TokenType
import printscript.semantic.symbol.SymbolTable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ExpressionResolverTest {
    private fun resolver(symbolTable: SymbolTable = SymbolTable()) = ExpressionResolver(symbolTable)

    @Test
    fun `resolves number literals`() {
        val result = resolver().resolveType(NumberLiteral(1.0))

        assertEquals(SemanticResult.Success("number"), result)
    }

    @Test
    fun `resolves string literals`() {
        val result = resolver().resolveType(StringLiteral("hi"))

        assertEquals(SemanticResult.Success("string"), result)
    }

    @Test
    fun `resolves a declared identifier to its declared type`() {
        val symbolTable = SymbolTable()
        symbolTable.define("a", "number")

        val result = resolver(symbolTable).resolveType(Identifier("a"))

        assertEquals(SemanticResult.Success("number"), result)
    }

    @Test
    fun `fails resolving an undeclared identifier`() {
        val result = resolver().resolveType(Identifier("missing"))

        assertIs<SemanticResult.Failure>(result)
    }

    @Test
    fun `adding two numbers yields number`() {
        val result = resolver().resolveType(BinaryExpression(NumberLiteral(1.0), TokenType.PLUS, NumberLiteral(2.0)))

        assertEquals(SemanticResult.Success("number"), result)
    }

    @Test
    fun `adding a number and a string yields string`() {
        val result = resolver().resolveType(BinaryExpression(NumberLiteral(1.0), TokenType.PLUS, StringLiteral("x")))

        assertEquals(SemanticResult.Success("string"), result)
    }

    @Test
    fun `subtracting two numbers yields number`() {
        val result = resolver().resolveType(BinaryExpression(NumberLiteral(5.0), TokenType.MINUS, NumberLiteral(2.0)))

        assertEquals(SemanticResult.Success("number"), result)
    }

    @Test
    fun `subtracting a string is a type error`() {
        val result = resolver().resolveType(BinaryExpression(StringLiteral("x"), TokenType.MINUS, NumberLiteral(2.0)))

        assertIs<SemanticResult.Failure>(result)
    }

    @Test
    fun `a failure on the left operand short-circuits the binary expression`() {
        val result =
            resolver().resolveType(
                BinaryExpression(Identifier("missing"), TokenType.PLUS, NumberLiteral(2.0)),
            )

        assertIs<SemanticResult.Failure>(result)
    }

    @Test
    fun `an expression with no handler registered fails explicitly with the node position`() {
        val emptyRegistry = Registry<Expression, ExpressionResolver, SemanticResult<String>>()
        val resolver = ExpressionResolver(SymbolTable(), emptyRegistry)
        val node = NumberLiteral(1.0, printscript.common.Position(4, 2))

        val result = resolver.resolveType(node)

        assertIs<SemanticResult.Failure>(result)
        assertEquals(node.position, result.position)
    }
}
