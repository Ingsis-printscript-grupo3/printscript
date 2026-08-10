package printscript.interpreter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EnvironmentTest {

    @Test
    fun `declarar una variable con valor y leerla devuelve ese valor`() {
        val env = Environment()
        env.declare("x", NumberValue(5.0))

        assertEquals(NumberValue(5.0), env.lookup("x"))
    }

    @Test
    fun `declarar dos veces la misma variable falla`() {
        val env = Environment()
        env.declare("x", NumberValue(5.0))

        val error = assertFailsWith<VariableAlreadyDeclaredError> {
            env.declare("x", NumberValue(10.0))
        }
        assertEquals("x", error.name)
    }

    @Test
    fun `asignar a una variable no declarada falla`() {
        val env = Environment()
        val error = assertFailsWith<UndeclaredVariableError> {
            env.assign("x", NumberValue(5.0))
        }
        assertEquals("x", error.name)
    }

    @Test
    fun `leer una variable no declarada falla`() {
        val env = Environment()
        val error = assertFailsWith<UndeclaredVariableError> {
            env.lookup("x")
        }
        assertEquals("x", error.name)
    }

    @Test
    fun `declarar sin valor y asignarle uno desp permite leerla`() {
        val env = Environment()
        env.declare("x", null)
        env.assign("x", NumberValue(5.0))

        assertEquals(NumberValue(5.0), env.lookup("x"))
    }

    @Test
    fun `leer una variable declarada sin valor falla`() {
        val env = Environment()
        env.declare("x", null)
        val error = assertFailsWith<UninitializedVariableError> {
            env.lookup("x")
        }
        assertEquals("x", error.name)
    }


}
