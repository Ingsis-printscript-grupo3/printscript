package printscript.parser.expression

import org.junit.jupiter.api.Test
import printscript.common.TokenType

class UnaryMinusEdgeCasesTest {
    @Test
    fun `constant folding on negative zero literals`() {
        listOf("0", "0.0").forEach { literal ->
            val expr = parseExpressionSuccess(minusToken(), num(literal))
            assertNumber(expr, 0.0)
        }
    }

    @Test
    fun `desugaring on negative parenthesized numeric literal`() {
        // -(5)  =>  0.0 - 5.0
        val expr = parseExpressionSuccess(minusToken(), lparenToken(), num("5"), rparenToken())
        assertNumber(assertDesugaredMinus(expr), 5.0)
    }

    @Test
    fun `desugaring on negative parenthesized negative literal`() {
        // -(-5)  =>  0.0 - (-5.0)
        val expr = parseExpressionSuccess(minusToken(), lparenToken(), minusToken(), num("5"), rparenToken())
        assertNumber(assertDesugaredMinus(expr), -5.0)
    }

    @Test
    fun `nested parenthesized negative variable`() {
        // -( -x )  =>  0.0 - (0.0 - x)
        val expr = parseExpressionSuccess(minusToken(), lparenToken(), minusToken(), id("x"), rparenToken())
        assertId(assertDesugaredMinus(assertDesugaredMinus(expr)), "x")
    }

    @Test
    fun `consecutive triple chained minus with numeric literal`() {
        // - - -5  =>  0.0 - (0.0 - (-5.0))
        val expr = parseExpressionSuccess(minusToken(), minusToken(), minusToken(), num("5"))
        assertNumber(assertDesugaredMinus(assertDesugaredMinus(expr)), -5.0)
    }

    @Test
    fun `consecutive triple chained minus with variable`() {
        // - - -x  =>  0.0 - (0.0 - (0.0 - x))
        val expr = parseExpressionSuccess(minusToken(), minusToken(), minusToken(), id("x"))
        assertId(assertDesugaredMinus(assertDesugaredMinus(assertDesugaredMinus(expr))), "x")
    }

    @Test
    fun `constant folded negative number in addition on left and right`() {
        // -5 + 3  =>  (-5.0) + 3.0
        val exprLeft =
            assertBinary(
                parseExpressionSuccess(minusToken(), num("5"), plusToken(), num("3")),
                TokenType.PLUS,
            )
        assertNumber(exprLeft.left, -5.0)
        assertNumber(exprLeft.right, 3.0)

        // 3 + -5  =>  3.0 + (-5.0)
        val exprRight =
            assertBinary(
                parseExpressionSuccess(num("3"), plusToken(), minusToken(), num("5")),
                TokenType.PLUS,
            )
        assertNumber(exprRight.left, 3.0)
        assertNumber(exprRight.right, -5.0)
    }

    @Test
    fun `constant folded negative number in multiplication on left and right`() {
        // -5 * 3  =>  (-5.0) * 3.0
        val exprLeft =
            assertBinary(
                parseExpressionSuccess(minusToken(), num("5"), multToken(), num("3")),
                TokenType.MULTIPLY,
            )
        assertNumber(exprLeft.left, -5.0)
        assertNumber(exprLeft.right, 3.0)

        // 3 * -5  =>  3.0 * (-5.0)
        val exprRight =
            assertBinary(
                parseExpressionSuccess(num("3"), multToken(), minusToken(), num("5")),
                TokenType.MULTIPLY,
            )
        assertNumber(exprRight.left, 3.0)
        assertNumber(exprRight.right, -5.0)
    }

    @Test
    fun `multiplication with negative variable on the right`() {
        // x * -y  =>  x * (0.0 - y)
        val expr =
            assertBinary(
                parseExpressionSuccess(id("x"), multToken(), minusToken(), id("y")),
                TokenType.MULTIPLY,
            )
        assertId(expr.left, "x")
        assertId(assertDesugaredMinus(expr.right), "y")
    }

    @Test
    fun `addition with negative variable on the right`() {
        // x + -y  =>  x + (0.0 - y)
        val expr =
            assertBinary(
                parseExpressionSuccess(id("x"), plusToken(), minusToken(), id("y")),
                TokenType.PLUS,
            )
        assertId(expr.left, "x")
        assertId(assertDesugaredMinus(expr.right), "y")
    }

    @Test
    fun `unary minus precedence with multiplication and addition`() {
        // x + -y * z  =>  x + ((0.0 - y) * z)
        val expr =
            assertBinary(
                parseExpressionSuccess(
                    id("x"),
                    plusToken(),
                    minusToken(),
                    id("y"),
                    multToken(),
                    id("z"),
                ),
                TokenType.PLUS,
            )
        assertId(expr.left, "x")

        val right = assertBinary(expr.right, TokenType.MULTIPLY)
        assertId(right.right, "z")
        assertId(assertDesugaredMinus(right.left), "y")
    }

    @Test
    fun `addition and multiplication are left associative`() {
        // 1 + 2 + 3  =>  ((1 + 2) + 3)
        val addExpr =
            assertBinary(
                parseExpressionSuccess(num("1"), plusToken(), num("2"), plusToken(), num("3")),
                TokenType.PLUS,
            )
        assertNumber(addExpr.right, 3.0)
        val addLeft = assertBinary(addExpr.left, TokenType.PLUS)
        assertNumber(addLeft.left, 1.0)
        assertNumber(addLeft.right, 2.0)

        // 2 * 3 * 4  =>  ((2 * 3) * 4)
        val mulExpr =
            assertBinary(
                parseExpressionSuccess(num("2"), multToken(), num("3"), multToken(), num("4")),
                TokenType.MULTIPLY,
            )
        assertNumber(mulExpr.right, 4.0)
        val mulLeft = assertBinary(mulExpr.left, TokenType.MULTIPLY)
        assertNumber(mulLeft.left, 2.0)
        assertNumber(mulLeft.right, 3.0)
    }

    @Test
    fun `right-associative operator chaining across four operands`() {
        // a = b = c = d  =>  (a = (b = (c = d)))
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
                    assignToken(),
                    id("d"),
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
        // a = b + c  =>  (a = (b + c))
        val rightAssoc = BinaryOperatorParselet(precedence = 5, isRightAssociative = true)
        val leftAssocPlus = BinaryOperatorParselet(precedence = 10, isRightAssociative = false)
        val expr =
            assertBinary(
                parseExpressionSuccessWithInfix(
                    mapOf(
                        TokenType.ASSIGN to rightAssoc,
                        TokenType.PLUS to leftAssocPlus,
                    ),
                    id("a"),
                    assignToken(),
                    id("b"),
                    plusToken(),
                    id("c"),
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
        listOf(
            // - -
            arrayOf(minusToken(), minusToken()),
            // - / 5
            arrayOf(minusToken(), divToken(), num("5")),
            // 5 + - -
            arrayOf(num("5"), plusToken(), minusToken(), minusToken()),
            // ( 5 + - )
            arrayOf(lparenToken(), num("5"), plusToken(), minusToken(), rparenToken()),
        ).forEach { tokens ->
            assertParseFailure(*tokens)
        }
    }
}
