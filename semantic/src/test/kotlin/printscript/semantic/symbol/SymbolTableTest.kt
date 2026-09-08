package printscript.semantic.symbol

import printscript.semantic.SemanticResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SymbolTableTest {
    @Test
    fun `defines and looks up a variable in root scope`() {
        val table = SymbolTable()
        val defineResult = table.define("x", "number")
        assertIs<SemanticResult.Success<*>>(defineResult)

        val lookupResult = table.lookupVariable("x")
        assertIs<SemanticResult.Success<VariableSymbol>>(lookupResult)
        assertEquals("number", lookupResult.value.type)
        assertFalse(lookupResult.value.isConst)

        val typeResult = table.lookupType("x")
        assertIs<SemanticResult.Success<String>>(typeResult)
        assertEquals("number", typeResult.value)
    }

    @Test
    fun `defines variable with isConst flag set to true`() {
        val table = SymbolTable()
        table.define("PI", "number", isConst = true)

        val lookupResult = table.lookupVariable("PI")
        assertIs<SemanticResult.Success<VariableSymbol>>(lookupResult)
        assertEquals("number", lookupResult.value.type)
        assertTrue(lookupResult.value.isConst)
    }

    @Test
    fun `rejects duplicate variable declaration in the same scope`() {
        val table = SymbolTable()
        table.define("x", "number")

        val duplicate = table.define("x", "string")
        assertIs<SemanticResult.Failure>(duplicate)
        assertTrue(duplicate.message.contains("already exists"))
    }

    @Test
    fun `returns failure when looking up an undeclared variable`() {
        val table = SymbolTable()
        val result = table.lookup("unknown")
        assertIs<SemanticResult.Failure>(result)
        assertTrue(result.message.contains("not declared"))

        val typeResult = table.lookupType("unknown")
        assertIs<SemanticResult.Failure>(typeResult)
        assertTrue(typeResult.message.contains("not declared"))
    }

    @Test
    fun `child scope can access variables defined in parent scope`() {
        val table = SymbolTable()
        table.define("globalVar", "string")

        table.enterScope()
        val lookupResult = table.lookupVariable("globalVar")
        assertIs<SemanticResult.Success<VariableSymbol>>(lookupResult)
        assertEquals("string", lookupResult.value.type)
        table.exitScope()
    }

    @Test
    fun `child scope can shadow variable with same name without altering parent`() {
        val table = SymbolTable()
        table.define("x", "number", isConst = false)

        table.enterScope()
        // Shadowing in inner scope
        val innerDefine = table.define("x", "string", isConst = true)
        assertIs<SemanticResult.Success<*>>(innerDefine)

        val innerLookup = table.lookupVariable("x")
        assertIs<SemanticResult.Success<VariableSymbol>>(innerLookup)
        assertEquals("string", innerLookup.value.type)
        assertTrue(innerLookup.value.isConst)

        table.exitScope()

        // Outer scope is restored
        val outerLookup = table.lookupVariable("x")
        assertIs<SemanticResult.Success<VariableSymbol>>(outerLookup)
        assertEquals("number", outerLookup.value.type)
        assertFalse(outerLookup.value.isConst)
    }

    @Test
    fun `variables defined in child scope are discarded upon exitScope`() {
        val table = SymbolTable()
        table.enterScope()
        table.define("temp", "boolean")
        table.exitScope()

        val lookup = table.lookup("temp")
        assertIs<SemanticResult.Failure>(lookup)
    }

    @Test
    fun `cannot exit root scope`() {
        val table = SymbolTable()
        assertFailsWith<IllegalStateException> {
            table.exitScope()
        }
    }

    @Test
    fun `supports multiple nested scopes`() {
        val table = SymbolTable()
        table.define("a", "number")

        table.enterScope() // Scope 1
        table.define("b", "string")

        table.enterScope() // Scope 2
        table.define("c", "boolean")

        assertEquals("number", (table.lookupType("a") as SemanticResult.Success).value)
        assertEquals("string", (table.lookupType("b") as SemanticResult.Success).value)
        assertEquals("boolean", (table.lookupType("c") as SemanticResult.Success).value)

        table.exitScope() // Exit Scope 2
        assertIs<SemanticResult.Failure>(table.lookup("c"))
        assertIs<SemanticResult.Success<*>>(table.lookup("b"))
        assertIs<SemanticResult.Success<*>>(table.lookup("a"))

        table.exitScope() // Exit Scope 1
        assertIs<SemanticResult.Failure>(table.lookup("b"))
        assertIs<SemanticResult.Success<*>>(table.lookup("a"))
    }
}
