package printscript.semantic

import printscript.ast.BinaryExpression
import printscript.ast.BooleanLiteral
import printscript.ast.Expression
import printscript.ast.Identifier
import printscript.ast.NumberLiteral
import printscript.ast.StringLiteral
import printscript.ast.registry.Registry
import printscript.common.LanguageVersion
import printscript.common.TokenType
import printscript.semantic.symbol.SymbolTable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ExpressionResolverTest {
    private fun resolver(
        symbolTable: SymbolTable = SymbolTable(),
        version: LanguageVersion = LanguageVersion.V1_1,
    ) = ExpressionResolver(symbolTable, version)

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
        val resolver = ExpressionResolver(SymbolTable(), LanguageVersion.V1_1, emptyRegistry)
        val node = NumberLiteral(1.0, printscript.common.Position(4, 2))

        val result = resolver.resolveType(node)

        assertIs<SemanticResult.Failure>(result)
        assertEquals(node.position, result.position)
    }

    @Test
    fun `resolves boolean literals in 1_1`() {
        val trueResult = resolver(version = LanguageVersion.V1_1).resolveType(BooleanLiteral(true))
        assertEquals(SemanticResult.Success("boolean"), trueResult)

        val falseResult = resolver(version = LanguageVersion.V1_1).resolveType(BooleanLiteral(false))
        assertEquals(SemanticResult.Success("boolean"), falseResult)
    }

    @Test
    fun `fails resolving boolean literals in 1_0`() {
        val result = resolver(version = LanguageVersion.V1_0).resolveType(BooleanLiteral(true))
        assertIs<SemanticResult.Failure>(result)
    }

    @Test
    fun `adding boolean and number is rejected`() {
        val expr1 = BinaryExpression(BooleanLiteral(true), TokenType.PLUS, NumberLiteral(1.0))
        val result1 = resolver().resolveType(expr1)
        assertIs<SemanticResult.Failure>(result1)

        val expr2 = BinaryExpression(NumberLiteral(1.0), TokenType.PLUS, BooleanLiteral(false))
        val result2 = resolver().resolveType(expr2)
        assertIs<SemanticResult.Failure>(result2)
    }

    @Test
    fun `adding boolean and string is rejected`() {
        val expr1 = BinaryExpression(StringLiteral("hello"), TokenType.PLUS, BooleanLiteral(true))
        val result1 = resolver().resolveType(expr1)
        assertIs<SemanticResult.Failure>(result1)

        val expr2 = BinaryExpression(BooleanLiteral(true), TokenType.PLUS, StringLiteral("hello"))
        val result2 = resolver().resolveType(expr2)
        assertIs<SemanticResult.Failure>(result2)
    }

    @Test
    fun `arithmetic operations between booleans are rejected`() {
        val ops = listOf(TokenType.PLUS, TokenType.MINUS, TokenType.MULTIPLY, TokenType.DIVIDE)
        for (op in ops) {
            val expr = BinaryExpression(BooleanLiteral(true), op, BooleanLiteral(false))
            val result = resolver().resolveType(expr)
            assertIs<SemanticResult.Failure>(result)
        }
    }
}
