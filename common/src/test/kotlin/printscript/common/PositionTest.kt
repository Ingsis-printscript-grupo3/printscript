package printscript.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class PositionTest {
    @Test
    fun `exposes line and column`() {
        val position = Position(3, 7)

        assertEquals(3, position.line)
        assertEquals(7, position.column)
    }

    @Test
    fun `supports equals, hashCode, copy and toString`() {
        val a = Position(1, 2)
        val b = Position(1, 2)
        val c = a.copy(column = 3)

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, c)
        assert(a.toString().contains("Position"))
    }
}
