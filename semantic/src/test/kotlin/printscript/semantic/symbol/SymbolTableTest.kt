package printscript.semantic.symbol

import printscript.semantic.SemanticResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SymbolTableTest {
    @Test
    fun `defining a new variable succeeds`() {
        val table = SymbolTable()

        val result = table.define("x", "number")

        assertIs<SemanticResult.Success<Unit>>(result)
    }

    @Test
    fun `redefining an existing variable fails`() {
        val table = SymbolTable()
        table.define("x", "number")

        val result = table.define("x", "string")

        assertIs<SemanticResult.Failure>(result)
    }

    @Test
    fun `looking up a declared variable returns its type`() {
        val table = SymbolTable()
        table.define("x", "number")

        val result = table.lookup("x")

        assertEquals(SemanticResult.Success("number"), result)
    }

    @Test
    fun `looking up an undeclared variable fails`() {
        val table = SymbolTable()

        val result = table.lookup("x")

        assertIs<SemanticResult.Failure>(result)
    }
}
