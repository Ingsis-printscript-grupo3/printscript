package printscript.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class TokenTest {
    @Test
    fun `exposes type, start, end and value`() {
        val token = Token(TokenType.IDENTIFIER, Position(1, 1), Position(1, 4), "abc")

        assertEquals(TokenType.IDENTIFIER, token.type)
        assertEquals(Position(1, 1), token.start)
        assertEquals(Position(1, 4), token.end)
        assertEquals("abc", token.value)
    }

    @Test
    fun `supports equals, hashCode, copy and toString`() {
        val a = Token(TokenType.PLUS, Position(0, 0), Position(0, 1), "+")
        val b = Token(TokenType.PLUS, Position(0, 0), Position(0, 1), "+")
        val c = a.copy(value = "-")

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, c)
        assert(a.toString().contains("Token"))
    }
}
