package printscript.parser.expression

import org.junit.jupiter.api.Test
import printscript.ast.BinaryExpression
import printscript.ast.Expression
import printscript.ast.Identifier
import printscript.ast.NumberLiteral
import printscript.common.Position
import printscript.common.Token
import printscript.common.TokenType
import printscript.parser.result.ASTResult
import printscript.parser.stream.TokenStream
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class UnaryMinusAndAssociativityTest {
    private fun pos(
        line: Int = 1,
        column: Int = 1,
    ) = Position(line, column)

    private fun token(
        type: TokenType,
        value: String = "",
        line: Int = 1,
        column: Int = 1,
    ) = Token(type, pos(line, column), Position(line, column + value.length), value)

    private fun parseExpression(vararg tokens: Token): ASTResult<Expression> {
        val tokenList = tokens.toList() + token(TokenType.EOF)
        val stream = TokenStream(tokenList.iterator())
        val expressionParser = ExpressionParser(stream)
        return expressionParser.parseExpression()
    }

    private fun parseExpressionSuccess(vararg tokens: Token): Expression {
        val result = parseExpression(*tokens)
        assertIs<ASTResult.Success<*>>(result)
        return result.value as Expression
    }

    // --- Happy Path: Negative numeric literals (constant folding) ---

    @Test
    fun `constant folding on negative integer literal`() {
        val expr =
            parseExpressionSuccess(
                token(TokenType.MINUS, "-", line = 1, column = 1),
                token(TokenType.NUMBERLITERAL, "5", line = 1, column = 2),
            )

        assertIs<NumberLiteral>(expr)
        assertEquals(-5.0, expr.value)
        assertEquals(pos(1, 1), expr.position)
    }

    @Test
    fun `constant folding on negative float literal`() {
        val expr =
            parseExpressionSuccess(
                token(TokenType.MINUS, "-", line = 2, column = 4),
                token(TokenType.NUMBERLITERAL, "3.14", line = 2, column = 5),
            )

        assertIs<NumberLiteral>(expr)
        assertEquals(-3.14, expr.value)
        assertEquals(pos(2, 4), expr.position)
    }

    @Test
    fun `constant folding preserves position of the minus token`() {
        val expr =
            parseExpressionSuccess(
                token(TokenType.MINUS, "-", line = 3, column = 10),
                token(TokenType.NUMBERLITERAL, "42", line = 3, column = 11),
            )

        assertEquals(pos(3, 10), expr.position)
    }

    // --- Happy Path: Negative variables and sub-expressions (desugaring) ---

    @Test
    fun `desugaring on negative variable`() {
        // -x  =>  0.0 - x
        val expr =
            parseExpressionSuccess(
                token(TokenType.MINUS, "-", line = 1, column = 1),
                token(TokenType.IDENTIFIER, "x", line = 1, column = 2),
            )

        assertIs<BinaryExpression>(expr)
        assertEquals(TokenType.MINUS, expr.operator)
        assertEquals(pos(1, 1), expr.position)

        val left = expr.left
        assertIs<NumberLiteral>(left)
        assertEquals(0.0, left.value)
        assertEquals(pos(1, 1), left.position)

        val right = expr.right
        assertIs<Identifier>(right)
        assertEquals("x", right.name)
    }

    @Test
    fun `desugaring on negative parenthesized sub-expression`() {
        // -(a + b)  =>  0.0 - (a + b)
        val expr =
            parseExpressionSuccess(
                token(TokenType.MINUS, "-", line = 1, column = 1),
                token(TokenType.LEFTPAREN, "(", line = 1, column = 2),
                token(TokenType.IDENTIFIER, "a", line = 1, column = 3),
                token(TokenType.PLUS, "+", line = 1, column = 5),
                token(TokenType.IDENTIFIER, "b", line = 1, column = 7),
                token(TokenType.RIGHTPAREN, ")", line = 1, column = 8),
            )

        assertIs<BinaryExpression>(expr)
        assertEquals(TokenType.MINUS, expr.operator)
        assertEquals(pos(1, 1), expr.position)

        val left = expr.left
        assertIs<NumberLiteral>(left)
        assertEquals(0.0, left.value)

        val right = expr.right
        assertIs<BinaryExpression>(right)
        assertEquals(TokenType.PLUS, right.operator)
        assertEquals("a", (right.left as Identifier).name)
        assertEquals("b", (right.right as Identifier).name)
    }

    // --- Happy Path: Consecutive / chained minus ---

    @Test
    fun `consecutive chained minus with numeric literal`() {
        // - -5  =>  0.0 - (-5.0)
        val expr =
            parseExpressionSuccess(
                token(TokenType.MINUS, "-", line = 1, column = 1),
                token(TokenType.MINUS, "-", line = 1, column = 3),
                token(TokenType.NUMBERLITERAL, "5", line = 1, column = 4),
            )

        assertIs<BinaryExpression>(expr)
        assertEquals(TokenType.MINUS, expr.operator)
        assertEquals(pos(1, 1), expr.position)

        val left = expr.left
        assertIs<NumberLiteral>(left)
        assertEquals(0.0, left.value)

        val right = expr.right
        assertIs<NumberLiteral>(right)
        assertEquals(-5.0, right.value)
        assertEquals(pos(1, 3), right.position)
    }

    @Test
    fun `consecutive chained minus with variable`() {
        // - -x  =>  0.0 - (0.0 - x)
        val expr =
            parseExpressionSuccess(
                token(TokenType.MINUS, "-", line = 1, column = 1),
                token(TokenType.MINUS, "-", line = 1, column = 3),
                token(TokenType.IDENTIFIER, "x", line = 1, column = 4),
            )

        assertIs<BinaryExpression>(expr)
        assertEquals(TokenType.MINUS, expr.operator)

        val outerLeft = expr.left
        assertIs<NumberLiteral>(outerLeft)
        assertEquals(0.0, outerLeft.value)

        val innerExpr = expr.right
        assertIs<BinaryExpression>(innerExpr)
        assertEquals(TokenType.MINUS, innerExpr.operator)

        val innerLeft = innerExpr.left
        assertIs<NumberLiteral>(innerLeft)
        assertEquals(0.0, innerLeft.value)

        val innerRight = innerExpr.right
        assertIs<Identifier>(innerRight)
        assertEquals("x", innerRight.name)
    }

    @Test
    fun `binary subtraction followed by negative numeric literal`() {
        // 5 - -3  =>  5 - (-3.0)
        val expr =
            parseExpressionSuccess(
                token(TokenType.NUMBERLITERAL, "5", line = 1, column = 1),
                token(TokenType.MINUS, "-", line = 1, column = 3),
                token(TokenType.MINUS, "-", line = 1, column = 5),
                token(TokenType.NUMBERLITERAL, "3", line = 1, column = 6),
            )

        assertIs<BinaryExpression>(expr)
        assertEquals(TokenType.MINUS, expr.operator)
        assertEquals(5.0, (expr.left as NumberLiteral).value)

        val right = expr.right
        assertIs<NumberLiteral>(right)
        assertEquals(-3.0, right.value)
    }

    @Test
    fun `binary subtraction followed by negative variable`() {
        // 5 - -x  =>  5 - (0.0 - x)
        val expr =
            parseExpressionSuccess(
                token(TokenType.NUMBERLITERAL, "5", line = 1, column = 1),
                token(TokenType.MINUS, "-", line = 1, column = 3),
                token(TokenType.MINUS, "-", line = 1, column = 5),
                token(TokenType.IDENTIFIER, "x", line = 1, column = 6),
            )

        assertIs<BinaryExpression>(expr)
        assertEquals(TokenType.MINUS, expr.operator)
        assertEquals(5.0, (expr.left as NumberLiteral).value)

        val right = expr.right
        assertIs<BinaryExpression>(right)
        assertEquals(TokenType.MINUS, right.operator)
        assertEquals(0.0, (right.left as NumberLiteral).value)
        assertEquals("x", (right.right as Identifier).name)
    }

    // --- Happy Path: Precedence tests without parentheses ---

    @Test
    fun `multiplication binds tighter than addition without parentheses`() {
        // 1 + 2 * 3  =>  1 + (2 * 3)
        val expr =
            parseExpressionSuccess(
                token(TokenType.NUMBERLITERAL, "1"),
                token(TokenType.PLUS, "+"),
                token(TokenType.NUMBERLITERAL, "2"),
                token(TokenType.MULTIPLY, "*"),
                token(TokenType.NUMBERLITERAL, "3"),
            )

        assertIs<BinaryExpression>(expr)
        assertEquals(TokenType.PLUS, expr.operator)
        assertEquals(1.0, (expr.left as NumberLiteral).value)

        val right = expr.right
        assertIs<BinaryExpression>(right)
        assertEquals(TokenType.MULTIPLY, right.operator)
        assertEquals(2.0, (right.left as NumberLiteral).value)
        assertEquals(3.0, (right.right as NumberLiteral).value)
    }

    @Test
    fun `multiplication on the left binds tighter than addition without parentheses`() {
        // 2 * 3 + 1  =>  (2 * 3) + 1
        val expr =
            parseExpressionSuccess(
                token(TokenType.NUMBERLITERAL, "2"),
                token(TokenType.MULTIPLY, "*"),
                token(TokenType.NUMBERLITERAL, "3"),
                token(TokenType.PLUS, "+"),
                token(TokenType.NUMBERLITERAL, "1"),
            )

        assertIs<BinaryExpression>(expr)
        assertEquals(TokenType.PLUS, expr.operator)
        assertEquals(1.0, (expr.right as NumberLiteral).value)

        val left = expr.left
        assertIs<BinaryExpression>(left)
        assertEquals(TokenType.MULTIPLY, left.operator)
        assertEquals(2.0, (left.left as NumberLiteral).value)
        assertEquals(3.0, (left.right as NumberLiteral).value)
    }

    @Test
    fun `unary minus binds tighter than multiplication`() {
        // -x * y  =>  (0.0 - x) * y
        val expr =
            parseExpressionSuccess(
                token(TokenType.MINUS, "-"),
                token(TokenType.IDENTIFIER, "x"),
                token(TokenType.MULTIPLY, "*"),
                token(TokenType.IDENTIFIER, "y"),
            )

        assertIs<BinaryExpression>(expr)
        assertEquals(TokenType.MULTIPLY, expr.operator)
        assertEquals("y", (expr.right as Identifier).name)

        val left = expr.left
        assertIs<BinaryExpression>(left)
        assertEquals(TokenType.MINUS, left.operator)
        assertEquals(0.0, (left.left as NumberLiteral).value)
        assertEquals("x", (left.right as Identifier).name)
    }

    @Test
    fun `unary minus binds tighter than addition`() {
        // -x + y  =>  (0.0 - x) + y
        val expr =
            parseExpressionSuccess(
                token(TokenType.MINUS, "-"),
                token(TokenType.IDENTIFIER, "x"),
                token(TokenType.PLUS, "+"),
                token(TokenType.IDENTIFIER, "y"),
            )

        assertIs<BinaryExpression>(expr)
        assertEquals(TokenType.PLUS, expr.operator)
        assertEquals("y", (expr.right as Identifier).name)

        val left = expr.left
        assertIs<BinaryExpression>(left)
        assertEquals(TokenType.MINUS, left.operator)
        assertEquals(0.0, (left.left as NumberLiteral).value)
        assertEquals("x", (left.right as Identifier).name)
    }

    // --- Happy Path: Left-associativity tests ---

    @Test
    fun `subtraction is left-associative grouping as ((10 - 4) - 2)`() {
        // 10 - 4 - 2  =>  ((10 - 4) - 2)
        val expr =
            parseExpressionSuccess(
                token(TokenType.NUMBERLITERAL, "10"),
                token(TokenType.MINUS, "-"),
                token(TokenType.NUMBERLITERAL, "4"),
                token(TokenType.MINUS, "-"),
                token(TokenType.NUMBERLITERAL, "2"),
            )

        assertIs<BinaryExpression>(expr)
        assertEquals(TokenType.MINUS, expr.operator)
        assertEquals(2.0, (expr.right as NumberLiteral).value)

        val left = expr.left
        assertIs<BinaryExpression>(left)
        assertEquals(TokenType.MINUS, left.operator)
        assertEquals(10.0, (left.left as NumberLiteral).value)
        assertEquals(4.0, (left.right as NumberLiteral).value)
    }

    @Test
    fun `division is left-associative grouping as ((16 slash 4) slash 2)`() {
        // 16 / 4 / 2  =>  ((16 / 4) / 2)
        val expr =
            parseExpressionSuccess(
                token(TokenType.NUMBERLITERAL, "16"),
                token(TokenType.DIVIDE, "/"),
                token(TokenType.NUMBERLITERAL, "4"),
                token(TokenType.DIVIDE, "/"),
                token(TokenType.NUMBERLITERAL, "2"),
            )

        assertIs<BinaryExpression>(expr)
        assertEquals(TokenType.DIVIDE, expr.operator)
        assertEquals(2.0, (expr.right as NumberLiteral).value)

        val left = expr.left
        assertIs<BinaryExpression>(left)
        assertEquals(TokenType.DIVIDE, left.operator)
        assertEquals(16.0, (left.left as NumberLiteral).value)
        assertEquals(4.0, (left.right as NumberLiteral).value)
    }

    // --- Happy Path: Associativity tests for BinaryOperatorParselet with isRightAssociative = true ---

    @Test
    fun `right-associative operator groups as (a op (b op c))`() {
        // Given an operator registered with isRightAssociative = true:
        // a = b = c  =>  (a = (b = c))
        val rightAssocParselet = BinaryOperatorParselet(precedence = 25, isRightAssociative = true)
        val tokens =
            listOf(
                token(TokenType.IDENTIFIER, "a"),
                token(TokenType.ASSIGN, "="),
                token(TokenType.IDENTIFIER, "b"),
                token(TokenType.ASSIGN, "="),
                token(TokenType.IDENTIFIER, "c"),
                token(TokenType.EOF),
            )
        val stream = TokenStream(tokens.iterator())
        val parser =
            ExpressionParser(
                stream = stream,
                infixParselets = mapOf(TokenType.ASSIGN to rightAssocParselet),
            )

        val result = parser.parseExpression()
        assertIs<ASTResult.Success<*>>(result)
        val expr = result.value as BinaryExpression

        assertEquals(TokenType.ASSIGN, expr.operator)
        assertEquals("a", (expr.left as Identifier).name)

        val right = expr.right
        assertIs<BinaryExpression>(right)
        assertEquals(TokenType.ASSIGN, right.operator)
        assertEquals("b", (right.left as Identifier).name)
        assertEquals("c", (right.right as Identifier).name)
    }

    @Test
    fun `left-associative operator groups as ((a op b) op c)`() {
        // Contrasting default left-associative behavior:
        val leftAssocParselet = BinaryOperatorParselet(precedence = 25, isRightAssociative = false)
        val tokens =
            listOf(
                token(TokenType.IDENTIFIER, "a"),
                token(TokenType.ASSIGN, "="),
                token(TokenType.IDENTIFIER, "b"),
                token(TokenType.ASSIGN, "="),
                token(TokenType.IDENTIFIER, "c"),
                token(TokenType.EOF),
            )
        val stream = TokenStream(tokens.iterator())
        val parser =
            ExpressionParser(
                stream = stream,
                infixParselets = mapOf(TokenType.ASSIGN to leftAssocParselet),
            )

        val result = parser.parseExpression()
        assertIs<ASTResult.Success<*>>(result)
        val expr = result.value as BinaryExpression

        assertEquals(TokenType.ASSIGN, expr.operator)
        assertEquals("c", (expr.right as Identifier).name)

        val left = expr.left
        assertIs<BinaryExpression>(left)
        assertEquals(TokenType.ASSIGN, left.operator)
        assertEquals("a", (left.left as Identifier).name)
        assertEquals("b", (left.right as Identifier).name)
    }

    // --- Non-Happy / Error Paths ---

    @Test
    fun `fails on trailing unary minus at end of expression`() {
        // - (with nothing after)
        val result =
            parseExpression(
                token(TokenType.MINUS, "-"),
            )

        assertIs<ASTResult.Failure>(result)
        assertTrue(result.message.contains("Expected a value or expression"))
    }

    @Test
    fun `fails on trailing unary minus followed by binary operator`() {
        // 5 + -
        val result =
            parseExpression(
                token(TokenType.NUMBERLITERAL, "5"),
                token(TokenType.PLUS, "+"),
                token(TokenType.MINUS, "-"),
            )

        assertIs<ASTResult.Failure>(result)
        assertTrue(result.message.contains("Expected a value or expression"))
    }

    @Test
    fun `fails on unary minus followed by another invalid operator`() {
        // - * 5
        val result =
            parseExpression(
                token(TokenType.MINUS, "-"),
                token(TokenType.MULTIPLY, "*"),
                token(TokenType.NUMBERLITERAL, "5"),
            )

        assertIs<ASTResult.Failure>(result)
        assertTrue(result.message.contains("Expected a value or expression"))
    }

    @Test
    fun `fails on unary minus followed by closing parenthesis`() {
        // ( - )
        val result =
            parseExpression(
                token(TokenType.LEFTPAREN, "("),
                token(TokenType.MINUS, "-"),
                token(TokenType.RIGHTPAREN, ")"),
            )

        assertIs<ASTResult.Failure>(result)
        assertTrue(result.message.contains("Expected a value or expression"))
    }
}
