package printscript.interpreter

import printscript.common.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

// posicion de mentira: estos tests miran el error, no donde ocurrio
private val AT = Position(1, 1)

class EnvironmentTest {
    @Test
    fun `declaring a variable with a value and reading it returns that value`() {
        val env = Environment()
        env.declare("x", NumberValue(5.0), at = AT)

        assertEquals(NumberValue(5.0), env.lookup("x", at = AT))
    }

    @Test
    fun `declaring the same variable twice fails`() {
        val env = Environment()
        env.declare("x", NumberValue(5.0), at = AT)

        val error =
            assertFailsWith<VariableAlreadyDeclaredError> {
                env.declare("x", NumberValue(10.0), at = AT)
            }
        assertEquals("x", error.name)
    }

    @Test
    fun `assigning to an undeclared variable fails`() {
        val env = Environment()
        val error =
            assertFailsWith<UndeclaredVariableError> {
                env.assign("x", NumberValue(5.0), at = AT)
            }
        assertEquals("x", error.name)
    }

    @Test
    fun `reading an undeclared variable fails`() {
        val env = Environment()
        val error =
            assertFailsWith<UndeclaredVariableError> {
                env.lookup("x", at = AT)
            }
        assertEquals("x", error.name)
    }

    @Test
    fun `declaring without a value and assigning one later allows reading it`() {
        val env = Environment()
        env.declare("x", null, at = AT)
        env.assign("x", NumberValue(5.0), at = AT)

        assertEquals(NumberValue(5.0), env.lookup("x", at = AT))
    }

    @Test
    fun `reading a declared variable without a value fails`() {
        val env = Environment()
        env.declare("x", null, at = AT)
        val error =
            assertFailsWith<UninitializedVariableError> {
                env.lookup("x", at = AT)
            }
        assertEquals("x", error.name)
    }
}
