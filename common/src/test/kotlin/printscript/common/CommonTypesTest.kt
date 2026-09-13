package printscript.common

import kotlin.test.Test
import kotlin.test.assertEquals

// Position y Token son data class sin comportamiento: lo unico que hay para verificar es
// que lleven los datos que les pasan. equals, hashCode, copy y toString los genera Kotlin
// y jacoco ya los filtra, asi que testearlos no cubria nada
class CommonTypesTest {
    @Test
    fun `position exposes line and column`() {
        val position = Position(3, 7)
        assertEquals(3, position.line)
        assertEquals(7, position.column)
    }

    @Test
    fun `token exposes type, start, end and value`() {
        val token = Token(TokenType.IDENTIFIER, Position(1, 1), Position(1, 4), "abc")
        assertEquals(TokenType.IDENTIFIER, token.type)
        assertEquals(Position(1, 1), token.start)
        assertEquals(Position(1, 4), token.end)
        assertEquals("abc", token.value)
    }
}
