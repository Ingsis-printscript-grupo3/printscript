package printscript.parser.expression

import org.junit.jupiter.api.Test
import printscript.common.TokenType
import kotlin.test.assertEquals

class UnaryMinusAndAssociativityTest {
    // --- Happy Path: Negative numeric literals (constant folding) ---

    @Test
    fun `constant folding on negative numeric literals`() {
        listOf(
            Triple("5", -5.0, pos(1, 1)),
            Triple("3.14", -3.14, pos(2, 4)),
            Triple("42", -42.0, pos(3, 10)),
        ).forEach { (raw, expectedVal, expectedPos) ->
            val expr =
                parseExpressionSuccess(
                    minusToken(line = expectedPos.line, column = expectedPos.column),
                    num(raw, line = expectedPos.line, column = expectedPos.column + 1),
                )
            val literal = assertNumber(expr, expectedVal)
            assertEquals(expectedPos, literal.position)
        }
    }

    // --- Happy Path: Negative variables and sub-expressions (desugaring) ---

    @Test
    fun `desugaring on negative variable`() {
        // -x  =>  0.0 - x
        val expr = parseExpressionSuccess(minusToken(1, 1), id("x", 1, 2))
        assertEquals(pos(1, 1), expr.position)
        assertId(assertDesugaredMinus(expr), "x")
    }

    @Test
    fun `desugaring on negative parenthesized sub-expression`() {
        // -(a + b)  =>  0.0 - (a + b)
        val expr =
            parseExpressionSuccess(
                minusToken(1, 1),
                lparenToken(1, 2),
                id("a", 1, 3),
                plusToken(1, 5),
                id("b", 1, 7),
                rparenToken(1, 8),
            )
        assertEquals(pos(1, 1), expr.position)

        val right = assertBinary(assertDesugaredMinus(expr), TokenType.PLUS)
        assertId(right.left, "a")
        assertId(right.right, "b")
    }

    // --- Happy Path: Consecutive / chained minus ---

    @Test
    fun `consecutive chained minus with numeric literal`() {
        // - -5  =>  0.0 - (-5.0)
        val expr = parseExpressionSuccess(minusToken(1, 1), minusToken(1, 3), num("5", 1, 4))
        assertEquals(pos(1, 1), expr.position)

        val literal = assertNumber(assertDesugaredMinus(expr), -5.0)
        assertEquals(pos(1, 3), literal.position)
    }

    @Test
    fun `consecutive chained minus with variable`() {
        // - -x  =>  0.0 - (0.0 - x)
        val expr = parseExpressionSuccess(minusToken(), minusToken(), id("x"))
        assertId(assertDesugaredMinus(assertDesugaredMinus(expr)), "x")
    }

    @Test
    fun `binary subtraction followed by negative numeric literal`() {
        // 5 - -3  =>  5 - (-3.0)
        val expr =
            assertBinary(
                parseExpressionSuccess(num("5"), minusToken(), minusToken(), num("3")),
                TokenType.MINUS,
            )
        assertNumber(expr.left, 5.0)
        assertNumber(expr.right, -3.0)
    }

    @Test
    fun `binary subtraction followed by negative variable`() {
        // 5 - -x  =>  5 - (0.0 - x)
        val expr =
            assertBinary(
                parseExpressionSuccess(num("5"), minusToken(), minusToken(), id("x")),
                TokenType.MINUS,
            )
        assertNumber(expr.left, 5.0)
        assertId(assertDesugaredMinus(expr.right), "x")
    }

    // --- Happy Path: Precedence tests without parentheses ---

    @Test
    fun `multiplication binds tighter than addition without parentheses`() {
        // 1 + 2 * 3  =>  1 + (2 * 3)
        val expr =
            assertBinary(
                parseExpressionSuccess(num("1"), plusToken(), num("2"), multToken(), num("3")),
                TokenType.PLUS,
            )
        assertNumber(expr.left, 1.0)

        val right = assertBinary(expr.right, TokenType.MULTIPLY)
        assertNumber(right.left, 2.0)
        assertNumber(right.right, 3.0)
    }

    @Test
    fun `multiplication on the left binds tighter than addition without parentheses`() {
        // 2 * 3 + 1  =>  (2 * 3) + 1
        val expr =
            assertBinary(
                parseExpressionSuccess(num("2"), multToken(), num("3"), plusToken(), num("1")),
                TokenType.PLUS,
            )
        assertNumber(expr.right, 1.0)

        val left = assertBinary(expr.left, TokenType.MULTIPLY)
        assertNumber(left.left, 2.0)
        assertNumber(left.right, 3.0)
    }

    @Test
    fun `unary minus binds tighter than multiplication`() {
        // -x * y  =>  (0.0 - x) * y
        val expr =
            assertBinary(
                parseExpressionSuccess(minusToken(), id("x"), multToken(), id("y")),
                TokenType.MULTIPLY,
            )
        assertId(expr.right, "y")
        assertId(assertDesugaredMinus(expr.left), "x")
    }

    @Test
    fun `unary minus binds tighter than addition`() {
        // -x + y  =>  (0.0 - x) + y
        val expr =
            assertBinary(
                parseExpressionSuccess(minusToken(), id("x"), plusToken(), id("y")),
                TokenType.PLUS,
            )
        assertId(expr.right, "y")
        assertId(assertDesugaredMinus(expr.left), "x")
    }

    // --- Happy Path: Left-associativity tests ---

    @Test
    fun `subtraction is left-associative grouping as ((10 - 4) - 2)`() {
        // 10 - 4 - 2  =>  ((10 - 4) - 2)
        val expr =
            assertBinary(
                parseExpressionSuccess(num("10"), minusToken(), num("4"), minusToken(), num("2")),
                TokenType.MINUS,
            )
        assertNumber(expr.right, 2.0)

        val left = assertBinary(expr.left, TokenType.MINUS)
        assertNumber(left.left, 10.0)
        assertNumber(left.right, 4.0)
    }

    @Test
    fun `division is left-associative grouping as ((16 slash 4) slash 2)`() {
        // 16 / 4 / 2  =>  ((16 / 4) / 2)
        val expr =
            assertBinary(
                parseExpressionSuccess(num("16"), divToken(), num("4"), divToken(), num("2")),
                TokenType.DIVIDE,
            )
        assertNumber(expr.right, 2.0)

        val left = assertBinary(expr.left, TokenType.DIVIDE)
        assertNumber(left.left, 16.0)
        assertNumber(left.right, 4.0)
    }

    // --- Happy Path: Associativity tests for BinaryOperatorParselet with isRightAssociative = true ---

    @Test
    fun `right-associative operator groups as (a op (b op c))`() {
        // Given an operator registered with isRightAssociative = true:
        // a = b = c  =>  (a = (b = c))
        val rightAssoc = BinaryOperatorParselet(precedence = 25, isRightAssociative = true)
        val expr =
            assertBinary(
                parseExpressionSuccessWithInfix(
                    mapOf(TokenType.ASSIGN to rightAssoc),
                    id("a"),
                    assignToken(),
                    id("b"),
                    assignToken(),
                    id("c"),
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
        // Contrasting default left-associative behavior:
        val leftAssoc = BinaryOperatorParselet(precedence = 25, isRightAssociative = false)
        val expr =
            assertBinary(
                parseExpressionSuccessWithInfix(
                    mapOf(TokenType.ASSIGN to leftAssoc),
                    id("a"),
                    assignToken(),
                    id("b"),
                    assignToken(),
                    id("c"),
                ),
                TokenType.ASSIGN,
            )
        assertId(expr.right, "c")

        val left = assertBinary(expr.left, TokenType.ASSIGN)
        assertId(left.left, "a")
        assertId(left.right, "b")
    }

    // --- Non-Happy / Error Paths ---

    @Test
    fun `fails on invalid or trailing unary minus error paths`() {
        listOf(
            // -
            arrayOf(minusToken()),
            // 5 + -
            arrayOf(num("5"), plusToken(), minusToken()),
            // - * 5
            arrayOf(minusToken(), multToken(), num("5")),
            // ( - )
            arrayOf(lparenToken(), minusToken(), rparenToken()),
        ).forEach { tokens ->
            assertParseFailure(*tokens)
        }
    }
}
