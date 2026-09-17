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

class UnaryMinusEdgeCasesTest {
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

    @Test
    fun `constant folding on negative zero literal`() {
        val expr =
            parseExpressionSuccess(
                token(TokenType.MINUS, "-"),
                token(TokenType.NUMBERLITERAL, "0"),
            )

        assertIs<NumberLiteral>(expr)
        assertEquals(0.0, expr.value)
    }

    @Test
    fun `constant folding on negative zero float literal`() {
        val expr =
            parseExpressionSuccess(
                token(TokenType.MINUS, "-"),
                token(TokenType.NUMBERLITERAL, "0.0"),
            )

        assertIs<NumberLiteral>(expr)
        assertEquals(0.0, expr.value)
    }

    @Test
    fun `desugaring on negative parenthesized numeric literal`() {
        // -(5)  =>  0.0 - 5.0
        val expr =
            parseExpressionSuccess(
                token(TokenType.MINUS, "-"),
                token(TokenType.LEFTPAREN, "("),
                token(TokenType.NUMBERLITERAL, "5"),
                token(TokenType.RIGHTPAREN, ")"),
            )

        assertIs<BinaryExpression>(expr)
        assertEquals(TokenType.MINUS, expr.operator)
        assertEquals(0.0, (expr.left as NumberLiteral).value)
        assertEquals(5.0, (expr.right as NumberLiteral).value)
    }

    @Test
    fun `desugaring on negative parenthesized negative literal`() {
        // -(-5)  =>  0.0 - (-5.0)
        val expr =
            parseExpressionSuccess(
                token(TokenType.MINUS, "-"),
                token(TokenType.LEFTPAREN, "("),
                token(TokenType.MINUS, "-"),
                token(TokenType.NUMBERLITERAL, "5"),
                token(TokenType.RIGHTPAREN, ")"),
            )

        assertIs<BinaryExpression>(expr)
        assertEquals(TokenType.MINUS, expr.operator)
        assertEquals(0.0, (expr.left as NumberLiteral).value)
        assertEquals(-5.0, (expr.right as NumberLiteral).value)
    }

    @Test
    fun `nested parenthesized negative variable`() {
        // -( -x )  =>  0.0 - (0.0 - x)
        val expr =
            parseExpressionSuccess(
                token(TokenType.MINUS, "-"),
                token(TokenType.LEFTPAREN, "("),
                token(TokenType.MINUS, "-"),
                token(TokenType.IDENTIFIER, "x"),
                token(TokenType.RIGHTPAREN, ")"),
            )

        assertIs<BinaryExpression>(expr)
        assertEquals(TokenType.MINUS, expr.operator)
        assertEquals(0.0, (expr.left as NumberLiteral).value)

        val right = expr.right as BinaryExpression
        assertEquals(TokenType.MINUS, right.operator)
        assertEquals(0.0, (right.left as NumberLiteral).value)
        assertEquals("x", (right.right as Identifier).name)
    }

    @Test
    fun `consecutive triple chained minus with numeric literal`() {
        // - - -5  =>  0.0 - (0.0 - (-5.0))
        val expr =
            parseExpressionSuccess(
                token(TokenType.MINUS, "-"),
                token(TokenType.MINUS, "-"),
                token(TokenType.MINUS, "-"),
                token(TokenType.NUMBERLITERAL, "5"),
            )

        assertIs<BinaryExpression>(expr)
        assertEquals(TokenType.MINUS, expr.operator)
        assertEquals(0.0, (expr.left as NumberLiteral).value)

        val inner = expr.right as BinaryExpression
        assertEquals(TokenType.MINUS, inner.operator)
        assertEquals(0.0, (inner.left as NumberLiteral).value)
        assertEquals(-5.0, (inner.right as NumberLiteral).value)
    }

    @Test
    fun `consecutive triple chained minus with variable`() {
        // - - -x  =>  0.0 - (0.0 - (0.0 - x))
        val expr =
            parseExpressionSuccess(
                token(TokenType.MINUS, "-"),
                token(TokenType.MINUS, "-"),
                token(TokenType.MINUS, "-"),
                token(TokenType.IDENTIFIER, "x"),
            )

        assertIs<BinaryExpression>(expr)
        assertEquals(TokenType.MINUS, expr.operator)

        val second = expr.right as BinaryExpression
        assertEquals(TokenType.MINUS, second.operator)

        val third = second.right as BinaryExpression
        assertEquals(TokenType.MINUS, third.operator)
        assertEquals(0.0, (third.left as NumberLiteral).value)
        assertEquals("x", (third.right as Identifier).name)
    }

    @Test
    fun `constant folded negative number in addition on left and right`() {
        // -5 + 3  =>  (-5.0) + 3.0
        val exprLeft =
            parseExpressionSuccess(
                token(TokenType.MINUS, "-"),
                token(TokenType.NUMBERLITERAL, "5"),
                token(TokenType.PLUS, "+"),
                token(TokenType.NUMBERLITERAL, "3"),
            )
        assertIs<BinaryExpression>(exprLeft)
        assertEquals(TokenType.PLUS, exprLeft.operator)
        assertEquals(-5.0, (exprLeft.left as NumberLiteral).value)
        assertEquals(3.0, (exprLeft.right as NumberLiteral).value)

        // 3 + -5  =>  3.0 + (-5.0)
        val exprRight =
            parseExpressionSuccess(
                token(TokenType.NUMBERLITERAL, "3"),
                token(TokenType.PLUS, "+"),
                token(TokenType.MINUS, "-"),
                token(TokenType.NUMBERLITERAL, "5"),
            )
        assertIs<BinaryExpression>(exprRight)
        assertEquals(TokenType.PLUS, exprRight.operator)
        assertEquals(3.0, (exprRight.left as NumberLiteral).value)
        assertEquals(-5.0, (exprRight.right as NumberLiteral).value)
    }

    @Test
    fun `constant folded negative number in multiplication on left and right`() {
        // -5 * 3  =>  (-5.0) * 3.0
        val exprLeft =
            parseExpressionSuccess(
                token(TokenType.MINUS, "-"),
                token(TokenType.NUMBERLITERAL, "5"),
                token(TokenType.MULTIPLY, "*"),
                token(TokenType.NUMBERLITERAL, "3"),
            )
        assertIs<BinaryExpression>(exprLeft)
        assertEquals(TokenType.MULTIPLY, exprLeft.operator)
        assertEquals(-5.0, (exprLeft.left as NumberLiteral).value)
        assertEquals(3.0, (exprLeft.right as NumberLiteral).value)

        // 3 * -5  =>  3.0 * (-5.0)
        val exprRight =
            parseExpressionSuccess(
                token(TokenType.NUMBERLITERAL, "3"),
                token(TokenType.MULTIPLY, "*"),
                token(TokenType.MINUS, "-"),
                token(TokenType.NUMBERLITERAL, "5"),
            )
        assertIs<BinaryExpression>(exprRight)
        assertEquals(TokenType.MULTIPLY, exprRight.operator)
        assertEquals(3.0, (exprRight.left as NumberLiteral).value)
        assertEquals(-5.0, (exprRight.right as NumberLiteral).value)
    }

    @Test
    fun `multiplication with negative variable on the right`() {
        // x * -y  =>  x * (0.0 - y)
        val expr =
            parseExpressionSuccess(
                token(TokenType.IDENTIFIER, "x"),
                token(TokenType.MULTIPLY, "*"),
                token(TokenType.MINUS, "-"),
                token(TokenType.IDENTIFIER, "y"),
            )

        assertIs<BinaryExpression>(expr)
        assertEquals(TokenType.MULTIPLY, expr.operator)
        assertEquals("x", (expr.left as Identifier).name)

        val right = expr.right as BinaryExpression
        assertEquals(TokenType.MINUS, right.operator)
        assertEquals(0.0, (right.left as NumberLiteral).value)
        assertEquals("y", (right.right as Identifier).name)
    }

    @Test
    fun `addition with negative variable on the right`() {
        // x + -y  =>  x + (0.0 - y)
        val expr =
            parseExpressionSuccess(
                token(TokenType.IDENTIFIER, "x"),
                token(TokenType.PLUS, "+"),
                token(TokenType.MINUS, "-"),
                token(TokenType.IDENTIFIER, "y"),
            )

        assertIs<BinaryExpression>(expr)
        assertEquals(TokenType.PLUS, expr.operator)
        assertEquals("x", (expr.left as Identifier).name)

        val right = expr.right as BinaryExpression
        assertEquals(TokenType.MINUS, right.operator)
        assertEquals(0.0, (right.left as NumberLiteral).value)
        assertEquals("y", (right.right as Identifier).name)
    }

    @Test
    fun `unary minus precedence with multiplication and addition`() {
        // x + -y * z  =>  x + ((0.0 - y) * z)
        val expr =
            parseExpressionSuccess(
                token(TokenType.IDENTIFIER, "x"),
                token(TokenType.PLUS, "+"),
                token(TokenType.MINUS, "-"),
                token(TokenType.IDENTIFIER, "y"),
                token(TokenType.MULTIPLY, "*"),
                token(TokenType.IDENTIFIER, "z"),
            )

        assertIs<BinaryExpression>(expr)
        assertEquals(TokenType.PLUS, expr.operator)
        assertEquals("x", (expr.left as Identifier).name)

        val right = expr.right as BinaryExpression
        assertEquals(TokenType.MULTIPLY, right.operator)
        assertEquals("z", (right.right as Identifier).name)

        val innerMinus = right.left as BinaryExpression
        assertEquals(TokenType.MINUS, innerMinus.operator)
        assertEquals(0.0, (innerMinus.left as NumberLiteral).value)
        assertEquals("y", (innerMinus.right as Identifier).name)
    }

    @Test
    fun `addition and multiplication are left associative`() {
        // 1 + 2 + 3  =>  ((1 + 2) + 3)
        val addExpr =
            parseExpressionSuccess(
                token(TokenType.NUMBERLITERAL, "1"),
                token(TokenType.PLUS, "+"),
                token(TokenType.NUMBERLITERAL, "2"),
                token(TokenType.PLUS, "+"),
                token(TokenType.NUMBERLITERAL, "3"),
            )
        assertIs<BinaryExpression>(addExpr)
        assertEquals(TokenType.PLUS, addExpr.operator)
        assertEquals(3.0, (addExpr.right as NumberLiteral).value)
        val addLeft = addExpr.left as BinaryExpression
        assertEquals(1.0, (addLeft.left as NumberLiteral).value)
        assertEquals(2.0, (addLeft.right as NumberLiteral).value)

        // 2 * 3 * 4  =>  ((2 * 3) * 4)
        val mulExpr =
            parseExpressionSuccess(
                token(TokenType.NUMBERLITERAL, "2"),
                token(TokenType.MULTIPLY, "*"),
                token(TokenType.NUMBERLITERAL, "3"),
                token(TokenType.MULTIPLY, "*"),
                token(TokenType.NUMBERLITERAL, "4"),
            )
        assertIs<BinaryExpression>(mulExpr)
        assertEquals(TokenType.MULTIPLY, mulExpr.operator)
        assertEquals(4.0, (mulExpr.right as NumberLiteral).value)
        val mulLeft = mulExpr.left as BinaryExpression
        assertEquals(2.0, (mulLeft.left as NumberLiteral).value)
        assertEquals(3.0, (mulLeft.right as NumberLiteral).value)
    }

    @Test
    fun `right-associative operator chaining across four operands`() {
        // a = b = c = d  =>  (a = (b = (c = d)))
        val rightAssoc = BinaryOperatorParselet(precedence = 25, isRightAssociative = true)
        val tokens =
            listOf(
                token(TokenType.IDENTIFIER, "a"),
                token(TokenType.ASSIGN, "="),
                token(TokenType.IDENTIFIER, "b"),
                token(TokenType.ASSIGN, "="),
                token(TokenType.IDENTIFIER, "c"),
                token(TokenType.ASSIGN, "="),
                token(TokenType.IDENTIFIER, "d"),
                token(TokenType.EOF),
            )
        val parser =
            ExpressionParser(
                stream = TokenStream(tokens.iterator()),
                infixParselets = mapOf(TokenType.ASSIGN to rightAssoc),
            )

        val result = parser.parseExpression()
        assertIs<ASTResult.Success<*>>(result)
        val expr = result.value as BinaryExpression

        assertEquals("a", (expr.left as Identifier).name)
        val bLevel = expr.right as BinaryExpression
        assertEquals("b", (bLevel.left as Identifier).name)
        val cLevel = bLevel.right as BinaryExpression
        assertEquals("c", (cLevel.left as Identifier).name)
        assertEquals("d", (cLevel.right as Identifier).name)
    }

    @Test
    fun `right-associative operator interacting with higher precedence left-associative operator`() {
        // a = b + c  =>  (a = (b + c))
        val rightAssoc = BinaryOperatorParselet(precedence = 5, isRightAssociative = true)
        val leftAssocPlus = BinaryOperatorParselet(precedence = 10, isRightAssociative = false)
        val tokens =
            listOf(
                token(TokenType.IDENTIFIER, "a"),
                token(TokenType.ASSIGN, "="),
                token(TokenType.IDENTIFIER, "b"),
                token(TokenType.PLUS, "+"),
                token(TokenType.IDENTIFIER, "c"),
                token(TokenType.EOF),
            )
        val parser =
            ExpressionParser(
                stream = TokenStream(tokens.iterator()),
                infixParselets =
                    mapOf(
                        TokenType.ASSIGN to rightAssoc,
                        TokenType.PLUS to leftAssocPlus,
                    ),
            )

        val result = parser.parseExpression()
        assertIs<ASTResult.Success<*>>(result)
        val expr = result.value as BinaryExpression

        assertEquals(TokenType.ASSIGN, expr.operator)
        assertEquals("a", (expr.left as Identifier).name)
        val right = expr.right as BinaryExpression
        assertEquals(TokenType.PLUS, right.operator)
        assertEquals("b", (right.left as Identifier).name)
        assertEquals("c", (right.right as Identifier).name)
    }

    @Test
    fun `fails on consecutive minuses ending at EOF`() {
        // - -
        val result =
            parseExpression(
                token(TokenType.MINUS, "-"),
                token(TokenType.MINUS, "-"),
            )

        assertIs<ASTResult.Failure>(result)
        assertTrue(result.message.contains("Expected a value or expression"))
    }

    @Test
    fun `fails on unary minus followed by division`() {
        // - / 5
        val result =
            parseExpression(
                token(TokenType.MINUS, "-"),
                token(TokenType.DIVIDE, "/"),
                token(TokenType.NUMBERLITERAL, "5"),
            )

        assertIs<ASTResult.Failure>(result)
        assertTrue(result.message.contains("Expected a value or expression"))
    }

    @Test
    fun `fails on double trailing unary minus after addition`() {
        // 5 + - -
        val result =
            parseExpression(
                token(TokenType.NUMBERLITERAL, "5"),
                token(TokenType.PLUS, "+"),
                token(TokenType.MINUS, "-"),
                token(TokenType.MINUS, "-"),
            )

        assertIs<ASTResult.Failure>(result)
        assertTrue(result.message.contains("Expected a value or expression"))
    }

    @Test
    fun `fails on trailing unary minus inside parenthesized expression`() {
        // ( 5 + - )
        val result =
            parseExpression(
                token(TokenType.LEFTPAREN, "("),
                token(TokenType.NUMBERLITERAL, "5"),
                token(TokenType.PLUS, "+"),
                token(TokenType.MINUS, "-"),
                token(TokenType.RIGHTPAREN, ")"),
            )

        assertIs<ASTResult.Failure>(result)
        assertTrue(result.message.contains("Expected a value or expression"))
    }
}
