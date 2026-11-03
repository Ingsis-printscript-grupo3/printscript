package printscript.parser.expression

import org.junit.jupiter.api.Test
import printscript.common.TokenType

class UnaryMinusEdgeCasesTest {
    @Test
    fun `constant folding on negative zero literals`() {
        listOf("0", "0.0").forEach { literal ->
            val expr = parseExpressionSuccess("-$literal")
            assertNumber(expr, 0.0)
        }
    }

    @Test
    fun `desugaring on negative parenthesized numeric literal`() {
        val expr = parseExpressionSuccess("-(5)")
        assertNumber(assertDesugaredMinus(expr), 5.0)
    }

    @Test
    fun `desugaring on negative parenthesized negative literal`() {
        val expr = parseExpressionSuccess("-(-5)")
        assertNumber(assertDesugaredMinus(expr), -5.0)
    }

    @Test
    fun `nested parenthesized negative variable`() {
        val expr = parseExpressionSuccess("-(-x)")
        assertId(assertDesugaredMinus(assertDesugaredMinus(expr)), "x")
    }

    @Test
    fun `consecutive triple chained minus with numeric literal`() {
        val expr = parseExpressionSuccess("- - -5")
        assertNumber(assertDesugaredMinus(assertDesugaredMinus(expr)), -5.0)
    }

    @Test
    fun `consecutive triple chained minus with variable`() {
        val expr = parseExpressionSuccess("- - -x")
        assertId(assertDesugaredMinus(assertDesugaredMinus(assertDesugaredMinus(expr))), "x")
    }

    @Test
    fun `constant folded negative number in addition on left and right`() {
        val exprLeft =
            assertBinary(
                parseExpressionSuccess("-5 + 3"),
                TokenType.PLUS,
            )
        assertNumber(exprLeft.left, -5.0)
        assertNumber(exprLeft.right, 3.0)

        val exprRight =
            assertBinary(
                parseExpressionSuccess("3 + -5"),
                TokenType.PLUS,
            )
        assertNumber(exprRight.left, 3.0)
        assertNumber(exprRight.right, -5.0)
    }

    @Test
    fun `constant folded negative number in multiplication on left and right`() {
        val exprLeft =
            assertBinary(
                parseExpressionSuccess("-5 * 3"),
                TokenType.MULTIPLY,
            )
        assertNumber(exprLeft.left, -5.0)
        assertNumber(exprLeft.right, 3.0)

        val exprRight =
            assertBinary(
                parseExpressionSuccess("3 * -5"),
                TokenType.MULTIPLY,
            )
        assertNumber(exprRight.left, 3.0)
        assertNumber(exprRight.right, -5.0)
    }

    @Test
    fun `multiplication with negative variable on the right`() {
        val expr =
            assertBinary(
                parseExpressionSuccess("x * -y"),
                TokenType.MULTIPLY,
            )
        assertId(expr.left, "x")
        assertId(assertDesugaredMinus(expr.right), "y")
    }

    @Test
    fun `addition with negative variable on the right`() {
        val expr =
            assertBinary(
                parseExpressionSuccess("x + -y"),
                TokenType.PLUS,
            )
        assertId(expr.left, "x")
        assertId(assertDesugaredMinus(expr.right), "y")
    }

    @Test
    fun `unary minus precedence with multiplication and addition`() {
        val expr =
            assertBinary(
                parseExpressionSuccess("x + -y * z"),
                TokenType.PLUS,
            )
        assertId(expr.left, "x")

        val right = assertBinary(expr.right, TokenType.MULTIPLY)
        assertId(right.right, "z")
        assertId(assertDesugaredMinus(right.left), "y")
    }

    @Test
    fun `addition and multiplication are left associative`() {
        val addExpr =
            assertBinary(
                parseExpressionSuccess("1 + 2 + 3"),
                TokenType.PLUS,
            )
        assertNumber(addExpr.right, 3.0)
        val addLeft = assertBinary(addExpr.left, TokenType.PLUS)
        assertNumber(addLeft.left, 1.0)
        assertNumber(addLeft.right, 2.0)

        val mulExpr =
            assertBinary(
                parseExpressionSuccess("2 * 3 * 4"),
                TokenType.MULTIPLY,
            )
        assertNumber(mulExpr.right, 4.0)
        val mulLeft = assertBinary(mulExpr.left, TokenType.MULTIPLY)
        assertNumber(mulLeft.left, 2.0)
        assertNumber(mulLeft.right, 3.0)
    }

    @Test
    fun `right-associative operator chaining across four operands`() {
        val rightAssoc = BinaryOperatorParselet(precedence = 25, isRightAssociative = true)
        val expr =
            assertBinary(
                parseExpressionSuccessWithInfix(
                    mapOf(TokenType.ASSIGN to rightAssoc),
                    "a = b = c = d",
                ),
                TokenType.ASSIGN,
            )
        assertId(expr.left, "a")

        val bLevel = assertBinary(expr.right, TokenType.ASSIGN)
        assertId(bLevel.left, "b")

        val cLevel = assertBinary(bLevel.right, TokenType.ASSIGN)
        assertId(cLevel.left, "c")
        assertId(cLevel.right, "d")
    }

    @Test
    fun `right-associative operator interacting with higher precedence left-associative operator`() {
        val rightAssoc = BinaryOperatorParselet(precedence = 5, isRightAssociative = true)
        val leftAssocPlus = BinaryOperatorParselet(precedence = 10, isRightAssociative = false)
        val expr =
            assertBinary(
                parseExpressionSuccessWithInfix(
                    mapOf(
                        TokenType.ASSIGN to rightAssoc,
                        TokenType.PLUS to leftAssocPlus,
                    ),
                    "a = b + c",
                ),
                TokenType.ASSIGN,
            )
        assertId(expr.left, "a")

        val right = assertBinary(expr.right, TokenType.PLUS)
        assertId(right.left, "b")
        assertId(right.right, "c")
    }

    @Test
    fun `fails on invalid or trailing unary minus edge cases`() {
        listOf("- -", "- / 5", "5 + - -", "( 5 + - )").forEach { code ->
            assertParseFailure(code)
        }
    }
}
