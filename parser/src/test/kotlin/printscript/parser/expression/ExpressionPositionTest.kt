package printscript.parser.expression

import printscript.common.Position
import kotlin.test.Test
import kotlin.test.assertEquals

class ExpressionPositionTest {
    @Test
    fun `a number literal takes the position of its own token`() {
        assertEquals(Position(2, 3), parseExpressionSuccess("\n  5").position)
    }

    @Test
    fun `a string literal takes the position of its own token`() {
        assertEquals(Position(4, 1), parseExpressionSuccess("\n\n\n\"hi\"").position)
    }

    @Test
    fun `an identifier takes the position of its own token`() {
        assertEquals(Position(6, 9), parseExpressionSuccess("\n\n\n\n\n        x").position)
    }

    @Test
    fun `a boolean literal takes the position of its own token`() {
        assertEquals(Position(8, 2), parseExpressionSuccess("\n\n\n\n\n\n\n true").position)
    }

    @Test
    fun `a binary expression takes the position of its left operand`() {
        assertEquals(Position(5, 4), parseExpressionSuccess("\n\n\n\n   1 + 2").position)
    }

    @Test
    fun `nested parenthesized expressions keep the position of their inner value`() {
        assertEquals(Position(1, 2), parseExpressionSuccess("(5)").position)
    }
}
