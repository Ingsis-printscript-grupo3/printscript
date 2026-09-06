package printscript.semantic

import printscript.common.Position
import kotlin.test.Test
import kotlin.test.assertEquals

class SemanticErrorTest {
    @Test
    fun `defaults end to start when not provided`() {
        val error = SemanticError("boom", Position(1, 2))

        assertEquals("boom", error.message)
        assertEquals(Position(1, 2), error.start)
        assertEquals(Position(1, 2), error.end)
    }

    @Test
    fun `accepts an explicit end position`() {
        val error = SemanticError("boom", Position(1, 2), Position(3, 4))

        assertEquals(Position(3, 4), error.end)
    }
}
