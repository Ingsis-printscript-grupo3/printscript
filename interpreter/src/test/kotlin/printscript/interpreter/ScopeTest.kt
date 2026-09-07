package printscript.interpreter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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

        val error =
            assertFailsWith<IllegalStateException> {
                env.exitScope()
            }
        assertTrue(error.message!!.contains("root scope"))
    }

    @Test
    fun `nested scopes discard variables at each level on exit`() {
        val env = Environment()
        env.declare("root", NumberValue(0.0))

        env.enterScope()
        env.declare("level1", NumberValue(1.0))

        env.enterScope()
        env.declare("level2", NumberValue(2.0))
        assertEquals(NumberValue(2.0), env.lookup("level2"))
        assertEquals(NumberValue(1.0), env.lookup("level1"))
        assertEquals(NumberValue(0.0), env.lookup("root"))

        env.exitScope()
        assertFailsWith<UndeclaredVariableError> { env.lookup("level2") }
        assertEquals(NumberValue(1.0), env.lookup("level1"))

        env.exitScope()
        assertFailsWith<UndeclaredVariableError> { env.lookup("level1") }
        assertEquals(NumberValue(0.0), env.lookup("root"))
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
