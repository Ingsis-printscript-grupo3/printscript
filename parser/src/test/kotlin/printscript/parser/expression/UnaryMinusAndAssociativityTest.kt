package printscript.parser.expression

import org.junit.jupiter.api.Test
import printscript.common.Position
import printscript.common.TokenType
import kotlin.test.assertEquals

class UnaryMinusAndAssociativityTest {
    @Test
    fun `constant folding on negative numeric literals`() {
        listOf(
            Triple("-5", -5.0, Position(1, 1)),
            Triple("\n   -3.14", -3.14, Position(2, 4)),
            Triple("\n\n         -42", -42.0, Position(3, 10)),
        ).forEach { (code, expectedValue, expectedPosition) ->
            val literal = assertNumber(parseExpressionSuccess(code), expectedValue)
            assertEquals(expectedPosition, literal.position)
        }
    }

    @Test
    fun `desugaring on negative variable`() {
        val expr = parseExpressionSuccess("-x")
        assertEquals(Position(1, 1), expr.position)
        assertId(assertDesugaredMinus(expr), "x")
    }

    @Test
    fun `desugaring on negative parenthesized sub-expression`() {
        val expr =
            parseExpressionSuccess("-(a + b)")
        assertEquals(Position(1, 1), expr.position)

        val right = assertBinary(assertDesugaredMinus(expr), TokenType.PLUS)
        assertId(right.left, "a")
        assertId(right.right, "b")
    }

    @Test
    fun `consecutive chained minus with numeric literal`() {
        val expr = parseExpressionSuccess("- -5")
        assertEquals(Position(1, 1), expr.position)

        val literal = assertNumber(assertDesugaredMinus(expr), -5.0)
        assertEquals(Position(1, 3), literal.position)
    }

    @Test
    fun `consecutive chained minus with variable`() {
        val expr = parseExpressionSuccess("- -x")
        assertId(assertDesugaredMinus(assertDesugaredMinus(expr)), "x")
    }

    @Test
    fun `binary subtraction followed by negative numeric literal`() {
        val expr =
            assertBinary(
                parseExpressionSuccess("5 - -3"),
                TokenType.MINUS,
            )
        assertNumber(expr.left, 5.0)
        assertNumber(expr.right, -3.0)
    }

    @Test
    fun `binary subtraction followed by negative variable`() {
        val expr =
            assertBinary(
                parseExpressionSuccess("5 - -x"),
                TokenType.MINUS,
            )
        assertNumber(expr.left, 5.0)
        assertId(assertDesugaredMinus(expr.right), "x")
    }

    @Test
    fun `multiplication binds tighter than addition without parentheses`() {
        val expr =
            assertBinary(
                parseExpressionSuccess("1 + 2 * 3"),
                TokenType.PLUS,
            )
        assertNumber(expr.left, 1.0)

        val right = assertBinary(expr.right, TokenType.MULTIPLY)
        assertNumber(right.left, 2.0)
        assertNumber(right.right, 3.0)
    }

    @Test
    fun `multiplication on the left binds tighter than addition without parentheses`() {
        val expr =
            assertBinary(
                parseExpressionSuccess("2 * 3 + 1"),
                TokenType.PLUS,
            )
        assertNumber(expr.right, 1.0)

        val left = assertBinary(expr.left, TokenType.MULTIPLY)
        assertNumber(left.left, 2.0)
        assertNumber(left.right, 3.0)
    }

    @Test
    fun `unary minus binds tighter than multiplication`() {
        val expr =
            assertBinary(
                parseExpressionSuccess("-x * y"),
                TokenType.MULTIPLY,
            )
        assertId(expr.right, "y")
        assertId(assertDesugaredMinus(expr.left), "x")
    }

    @Test
    fun `unary minus binds tighter than addition`() {
        val expr =
            assertBinary(
                parseExpressionSuccess("-x + y"),
                TokenType.PLUS,
            )
        assertId(expr.right, "y")
        assertId(assertDesugaredMinus(expr.left), "x")
    }

    @Test
    fun `subtraction is left-associative grouping as ((10 - 4) - 2)`() {
        val expr =
            assertBinary(
                parseExpressionSuccess("10 - 4 - 2"),
                TokenType.MINUS,
            )
        assertNumber(expr.right, 2.0)

        val left = assertBinary(expr.left, TokenType.MINUS)
        assertNumber(left.left, 10.0)
        assertNumber(left.right, 4.0)
    }

    @Test
    fun `division is left-associative grouping as ((16 slash 4) slash 2)`() {
        val expr =
            assertBinary(
                parseExpressionSuccess("16 / 4 / 2"),
                TokenType.DIVIDE,
            )
        assertNumber(expr.right, 2.0)

        val left = assertBinary(expr.left, TokenType.DIVIDE)
        assertNumber(left.left, 16.0)
        assertNumber(left.right, 4.0)
    }

    @Test
    fun `right-associative operator groups as (a op (b op c))`() {
        val rightAssoc = BinaryOperatorParselet(precedence = 25, isRightAssociative = true)
        val expr =
            assertBinary(
                parseExpressionSuccessWithInfix(
                    mapOf(TokenType.ASSIGN to rightAssoc),
                    "a = b = c",
                ),
                TokenType.ASSIGN,
            )
        assertId(expr.left, "a")

        val right = assertBinary(expr.right, TokenType.ASSIGN)
        assertId(right.left, "b")
        assertId(right.right, "c")
    }

    @Test
    fun `left-associative operator groups as ((a op b) op c)`() {
        val leftAssoc = BinaryOperatorParselet(precedence = 25, isRightAssociative = false)
        val expr =
            assertBinary(
                parseExpressionSuccessWithInfix(
                    mapOf(TokenType.ASSIGN to leftAssoc),
                    "a = b = c",
                ),
                TokenType.ASSIGN,
            )
        assertId(expr.right, "c")

        val left = assertBinary(expr.left, TokenType.ASSIGN)
        assertId(left.left, "a")
        assertId(left.right, "b")
    }

    @Test
    fun `fails on invalid or trailing unary minus error paths`() {
        listOf("-", "5 + -", "- * 5", "( - )").forEach { code ->
            assertParseFailure(code)
        }
    }
}
