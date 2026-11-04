package printscript.interpreter

import printscript.interpreter.output.BucketOutput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ConstTest {
    @Test
    fun `a const can be declared and read`() {
        val code =
            """
            const x: number = 42;
            println(x);
            """.trimIndent()
        val output = BucketOutput()
        val interpreter = InterpreterFactory.create11(output = output)

        interpreter.interpret(parse(code))

        assertEquals(listOf("42"), output.lines())
    }

    @Test
    fun `reassigning a const throws CannotAssignToConstError`() {
        val code =
            """
            const PI: number = 3.14;
            println(PI);
            PI = 3.14159;
            """.trimIndent()
        val output = BucketOutput()
        val interpreter = InterpreterFactory.create11(output = output)

        val error =
            assertFailsWith<CannotAssignToConstError> {
                interpreter.interpret(parse(code))
            }

        assertEquals("PI", error.name)
        assertEquals(listOf("3.14"), output.lines())
    }
}
