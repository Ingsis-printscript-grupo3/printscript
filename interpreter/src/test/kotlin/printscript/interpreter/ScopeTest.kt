package printscript.interpreter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScopeTest {
    @Test
    fun `variables in parent scope are visible in child scope`() {
        val env = Environment()
        env.declare("x", NumberValue(10.0))

        env.enterScope()
        assertEquals(NumberValue(10.0), env.lookup("x"))
        env.exitScope()

        assertEquals(NumberValue(10.0), env.lookup("x"))
    }

    @Test
    fun `variables declared in child scope are discarded upon exiting scope`() {
        val env = Environment()
        env.declare("x", NumberValue(1.0))

        env.enterScope()
        env.declare("y", NumberValue(2.0))
        assertEquals(NumberValue(2.0), env.lookup("y"))
        env.exitScope()

        val error =
            assertFailsWith<UndeclaredVariableError> {
                env.lookup("y")
            }
        assertEquals("y", error.name)
    }

    @Test
    fun `child scope can shadow a variable from parent scope`() {
        val env = Environment()
        env.declare("x", StringValue("outer"))

        env.enterScope()
        env.declare("x", StringValue("inner"))
        assertEquals(StringValue("inner"), env.lookup("x"))
        env.exitScope()

        assertEquals(StringValue("outer"), env.lookup("x"))
    }

    @Test
    fun `reassigning variable from parent scope within child scope modifies parent value`() {
        val env = Environment()
        env.declare("x", NumberValue(1.0))

        env.enterScope()
        env.assign("x", NumberValue(2.0))
        assertEquals(NumberValue(2.0), env.lookup("x"))
        env.exitScope()

        assertEquals(NumberValue(2.0), env.lookup("x"))
    }

    @Test
    fun `cannot reassign constant variable in parent or child scope`() {
        val env = Environment()
        env.declare("c", NumberValue(42.0), isConst = true)

        val err1 =
            assertFailsWith<CannotAssignToConstError> {
                env.assign("c", NumberValue(100.0))
            }
        assertEquals("c", err1.name)

        env.enterScope()
        val err2 =
            assertFailsWith<CannotAssignToConstError> {
                env.assign("c", NumberValue(200.0))
            }
        assertEquals("c", err2.name)
        env.exitScope()
    }

    @Test
    fun `cannot declare duplicate variable in the same scope`() {
        val env = Environment()
        env.declare("a", NumberValue(1.0))

        assertFailsWith<VariableAlreadyDeclaredError> {
            env.declare("a", NumberValue(2.0))
        }

        env.enterScope()
        env.declare("a", NumberValue(3.0)) // shadowing is allowed in new scope
        assertFailsWith<VariableAlreadyDeclaredError> {
            env.declare("a", NumberValue(4.0)) // duplicate in child scope
        }
        env.exitScope()
    }

    @Test
    fun `cannot exit root scope`() {
        val env = Environment()
        assertTrue(env.isRootScope())
        assertEquals(1, env.currentScopeDepth())

        val error =
            assertFailsWith<IllegalStateException> {
                env.exitScope()
            }
        assertTrue(error.message!!.contains("root scope"))
    }

    @Test
    fun `scope depth reflects nesting`() {
        val env = Environment()
        assertTrue(env.isRootScope())
        assertEquals(1, env.currentScopeDepth())

        env.enterScope()
        assertFalse(env.isRootScope())
        assertEquals(2, env.currentScopeDepth())

        env.enterScope()
        assertEquals(3, env.currentScopeDepth())

        env.exitScope()
        assertEquals(2, env.currentScopeDepth())

        env.exitScope()
        assertTrue(env.isRootScope())
        assertEquals(1, env.currentScopeDepth())
    }

    @Test
    fun `typeOf returns declared variable type`() {
        val env = Environment()
        env.declare("x", NumberValue(1.0), type = "number")
        env.declare("y", null, type = "string")

        assertEquals("number", env.typeOf("x"))
        assertEquals("string", env.typeOf("y"))
        assertEquals(null, env.typeOf("z"))
    }
}
