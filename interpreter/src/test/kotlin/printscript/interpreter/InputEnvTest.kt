package printscript.interpreter

import printscript.interpreter.env.MapEnvProvider
import printscript.interpreter.env.SystemEnvProvider
import printscript.interpreter.input.ConsoleInput
import printscript.interpreter.input.QueueInput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class InputEnvTest {
    @Test
    fun `QueueInput yields inputs in FIFO order`() {
        val input = QueueInput("first", "second")

        assertEquals("first", input.readInput("prompt1"))
        assertEquals("second", input.readInput("prompt2"))
    }

    @Test
    fun `QueueInput created from list yields inputs in order`() {
        val input = QueueInput(listOf("a", "b"))

        assertEquals("a", input.readInput("p"))
        assertEquals("b", input.readInput("p"))
    }

    @Test
    fun `QueueInput throws NoSuchElementException when empty`() {
        val input = QueueInput()

        assertFailsWith<NoSuchElementException> {
            input.readInput("prompt")
        }
    }

    @Test
    fun `ConsoleInput can be instantiated as InputProvider`() {
        val consoleInput = ConsoleInput()
        assertNotNull(consoleInput)
    }

    @Test
    fun `MapEnvProvider returns values for defined keys`() {
        val env = MapEnvProvider("USER" to "alice", "PORT" to "8080")

        assertEquals("alice", env.getEnv("USER"))
        assertEquals("8080", env.getEnv("PORT"))
        assertNull(env.getEnv("NON_EXISTENT"))
    }

    @Test
    fun `MapEnvProvider created with map returns values`() {
        val env = MapEnvProvider(mapOf("KEY" to "VALUE"))

        assertEquals("VALUE", env.getEnv("KEY"))
        assertNull(env.getEnv("OTHER"))
    }

    @Test
    fun `SystemEnvProvider returns null for nonexistent variables`() {
        val env = SystemEnvProvider()
        assertNull(env.getEnv("THIS_VARIABLE_SHOULD_DEFINITELY_NOT_EXIST_PRINTSCRIPT_TEST"))
    }
}
