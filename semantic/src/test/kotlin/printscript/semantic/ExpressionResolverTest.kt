package printscript.semantic

import printscript.ast.BinaryExpression
import printscript.ast.Expression
import printscript.ast.Identifier
import printscript.ast.NumberLiteral
import printscript.ast.StringLiteral
import printscript.common.TokenType
import printscript.semantic.plugin.expression.BinaryExpressionHandler
import printscript.semantic.plugin.expression.IdentifierHandler
import printscript.semantic.plugin.expression.NumberLiteralHandler
import printscript.semantic.plugin.expression.StringLiteralHandler
import printscript.semantic.symbol.SymbolTable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class ExpressionResolverTest {
    private fun num(value: Double) = NumberLiteral(value)

    private fun text(value: String) = StringLiteral(value)

    private fun id(name: String) = Identifier(name)

    private fun bin(
        left: Expression,
        op: TokenType,
        right: Expression,
    ) = BinaryExpression(left, op, right)

    @Test
    fun `a number literal resolves to number`() {
        val resolver = ExpressionResolver(SymbolTable())

        assertEquals(SemanticResult.Success("number"), resolver.resolveType(num(1.0)))
    }

    @Test
    fun `a string literal resolves to string`() {
        val resolver = ExpressionResolver(SymbolTable())

        assertEquals(SemanticResult.Success("string"), resolver.resolveType(text("hi")))
    }

    @Test
    fun `an identifier resolves to the declared type`() {
        val symbolTable = SymbolTable()
        symbolTable.define("x", "number")
        val resolver = ExpressionResolver(symbolTable)

        assertEquals(SemanticResult.Success("number"), resolver.resolveType(id("x")))
    }

    @Test
    fun `an undeclared identifier fails`() {
        val resolver = ExpressionResolver(SymbolTable())

        assertIs<SemanticResult.Failure>(resolver.resolveType(id("x")))
    }

    @Test
    fun `adding two numbers resolves to number`() {
        val resolver = ExpressionResolver(SymbolTable())

        assertEquals(SemanticResult.Success("number"), resolver.resolveType(bin(num(1.0), TokenType.PLUS, num(2.0))))
    }

    @Test
    fun `adding a number and a string concatenates to string`() {
        val resolver = ExpressionResolver(SymbolTable())

        assertEquals(
            SemanticResult.Success("string"),
            resolver.resolveType(bin(num(1.0), TokenType.PLUS, text("a"))),
        )
    }

    @Test
    fun `subtracting two numbers resolves to number`() {
        val resolver = ExpressionResolver(SymbolTable())

        assertEquals(
            SemanticResult.Success("number"),
            resolver.resolveType(bin(num(4.0), TokenType.MINUS, num(2.0))),
        )
    }

    @Test
    fun `subtracting a string is a type error`() {
        val resolver = ExpressionResolver(SymbolTable())

        assertIs<SemanticResult.Failure>(resolver.resolveType(bin(text("a"), TokenType.MINUS, num(2.0))))
    }

    @Test
    fun `a failure on the left side of a binary expression short-circuits`() {
        val resolver = ExpressionResolver(SymbolTable())

        assertIs<SemanticResult.Failure>(resolver.resolveType(bin(id("missing"), TokenType.PLUS, num(1.0))))
    }

    @Test
    fun `a failure on the right side of a binary expression short-circuits`() {
        val resolver = ExpressionResolver(SymbolTable())

        assertIs<SemanticResult.Failure>(resolver.resolveType(bin(num(1.0), TokenType.PLUS, id("missing"))))
    }

    @Test
    fun `nested binary expressions recurse through the back-reference`() {
        val resolver = ExpressionResolver(SymbolTable())
        // (1 + 2) * 3
        val nested = bin(bin(num(1.0), TokenType.PLUS, num(2.0)), TokenType.MULTIPLY, num(3.0))

        assertEquals(SemanticResult.Success("number"), resolver.resolveType(nested))
    }

    @Test
    fun `an expression with no handler registered fails`() {
        val resolver = ExpressionResolver(SymbolTable(), emptyList())

        assertFailsWith<UnknownExpressionError> {
            resolver.resolveType(num(1.0))
        }
    }

    // each handler has a guard that throws if it gets a node that isn't its own;
    // this never happens in the normal flow, since the resolver asks matches() first
    @Test
    fun `expression handlers reject nodes that are not theirs`() {
        val number = num(1.0)
        val string = text("hi")

        val cases =
            listOf(
                NumberLiteralHandler() to string,
                StringLiteralHandler() to number,
                IdentifierHandler() to number,
                BinaryExpressionHandler() to number,
            )

        for ((handler, foreignNode) in cases) {
            assertFailsWith<UnknownExpressionError> {
                handler.resolveType(foreignNode, SymbolTable(), ExpressionResolver(SymbolTable()))
            }
        }
    }
}
