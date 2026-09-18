package printscript.interpreter

import printscript.common.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

// posicion de mentira: estos tests miran el error, no donde ocurrio
private val AT = Position(1, 1)

class ScopeTest {
    @Test
    fun `variables in parent scope are visible in child scope`() {
        val env = Environment()
        env.declare("x", NumberValue(10.0), at = AT)

        env.enterScope()
        assertEquals(NumberValue(10.0), env.lookup("x", at = AT))
        env.exitScope()

        assertEquals(NumberValue(10.0), env.lookup("x", at = AT))
    }

    @Test
    fun `variables declared in child scope are discarded upon exiting scope`() {
        val env = Environment()
        env.declare("x", NumberValue(1.0), at = AT)

        env.enterScope()
        env.declare("y", NumberValue(2.0), at = AT)
        assertEquals(NumberValue(2.0), env.lookup("y", at = AT))
        env.exitScope()

        val error =
            assertFailsWith<UndeclaredVariableError> {
                env.lookup("y", at = AT)
            }
        assertEquals("y", error.name)
    }

    @Test
    fun `child scope can shadow a variable from parent scope`() {
        val env = Environment()
        env.declare("x", StringValue("outer"), at = AT)

        env.enterScope()
        env.declare("x", StringValue("inner"), at = AT)
        assertEquals(StringValue("inner"), env.lookup("x", at = AT))
        env.exitScope()

        assertEquals(StringValue("outer"), env.lookup("x", at = AT))
    }

    @Test
    fun `reassigning variable from parent scope within child scope modifies parent value`() {
        val env = Environment()
        env.declare("x", NumberValue(1.0), at = AT)

        env.enterScope()
        env.assign("x", NumberValue(2.0), at = AT)
        assertEquals(NumberValue(2.0), env.lookup("x", at = AT))
        env.exitScope()

        assertEquals(NumberValue(2.0), env.lookup("x", at = AT))
    }

    @Test
    fun `cannot reassign constant variable in parent or child scope`() {
        val env = Environment()
        env.declare("c", NumberValue(42.0), isConst = true, at = AT)

        val err1 =
            assertFailsWith<CannotAssignToConstError> {
                env.assign("c", NumberValue(100.0), at = AT)
            }
        assertEquals("c", err1.name)

        env.enterScope()
        val err2 =
            assertFailsWith<CannotAssignToConstError> {
                env.assign("c", NumberValue(200.0), at = AT)
            }
        assertEquals("c", err2.name)
        env.exitScope()
    }

    @Test
    fun `cannot declare duplicate variable in the same scope`() {
        val env = Environment()
        env.declare("a", NumberValue(1.0), at = AT)

        assertFailsWith<VariableAlreadyDeclaredError> {
            env.declare("a", NumberValue(2.0), at = AT)
        }

        env.enterScope()
        env.declare("a", NumberValue(3.0), at = AT) // shadowing is allowed in new scope
        assertFailsWith<VariableAlreadyDeclaredError> {
            env.declare("a", NumberValue(4.0), at = AT) // duplicate in child scope
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
        env.declare("root", NumberValue(0.0), at = AT)

        env.enterScope()
        env.declare("level1", NumberValue(1.0), at = AT)

        env.enterScope()
        env.declare("level2", NumberValue(2.0), at = AT)
        assertEquals(NumberValue(2.0), env.lookup("level2", at = AT))
        assertEquals(NumberValue(1.0), env.lookup("level1", at = AT))
        assertEquals(NumberValue(0.0), env.lookup("root", at = AT))

        env.exitScope()
        assertFailsWith<UndeclaredVariableError> { env.lookup("level2", at = AT) }
        assertEquals(NumberValue(1.0), env.lookup("level1", at = AT))

        env.exitScope()
        assertFailsWith<UndeclaredVariableError> { env.lookup("level1", at = AT) }
        assertEquals(NumberValue(0.0), env.lookup("root", at = AT))
    }

    @Test
    fun `typeOf returns declared variable type`() {
        val env = Environment()
        env.declare("x", NumberValue(1.0), type = "number", at = AT)
        env.declare("y", null, type = "string", at = AT)

        assertEquals("number", env.typeOf("x"))
        assertEquals("string", env.typeOf("y"))
        assertEquals(null, env.typeOf("z"))
    }
}
