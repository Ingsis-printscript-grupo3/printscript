package printscript.interpreter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EnvironmentTest {

    @Test
    fun `declaring a variable with a value and reading it returns that value`() {
        val env = Environment()
        env.declare("x", NumberValue(5.0))

        assertEquals(NumberValue(5.0), env.lookup("x"))
    }

    @Test
    fun `declaring the same variable twice fails`() {
        val env = Environment()
        env.declare("x", NumberValue(5.0))

        val error = assertFailsWith<VariableAlreadyDeclaredError> {
            env.declare("x", NumberValue(10.0))
        }
        assertEquals("x", error.name)
    }

    @Test
    fun `assigning to an undeclared variable fails`() {
        val env = Environment()
        val error = assertFailsWith<UndeclaredVariableError> {
            env.assign("x", NumberValue(5.0))
        }
        assertEquals("x", error.name)
    }

    @Test
    fun `reading an undeclared variable fails`() {
        val env = Environment()
        val error = assertFailsWith<UndeclaredVariableError> {
            env.lookup("x")
        }
        assertEquals("x", error.name)
    }

    @Test
    fun `declaring without a value and assigning one later allows reading it`() {
        val env = Environment()
        env.declare("x", null)
        env.assign("x", NumberValue(5.0))

        assertEquals(NumberValue(5.0), env.lookup("x"))
    }

    @Test
    fun `reading a declared variable without a value fails`() {
        val env = Environment()
        env.declare("x", null)
        val error = assertFailsWith<UninitializedVariableError> {
            env.lookup("x")
        }
        assertEquals("x", error.name)
    }


}
