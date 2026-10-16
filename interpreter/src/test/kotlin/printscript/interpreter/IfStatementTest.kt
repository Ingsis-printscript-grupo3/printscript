package printscript.interpreter

import printscript.interpreter.output.BucketOutput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IfStatementTest {
    @Test
    fun `if executes the then branch when the condition is true`() {
        val code =
            """
            let x: number = 10;
            if (true) {
                let y: number = 20;
                x = x + y;
                println("inside: " + x);
            }
            println("outside: " + x);
            """.trimIndent()
        val output = BucketOutput()
        val interpreter = InterpreterFactory.create11(output = output)

        interpreter.interpret(parse(code))

        assertEquals(listOf("inside: 30", "outside: 30"), output.lines())
    }

    @Test
    fun `if executes the else branch when the condition is false`() {
        val code =
            """
            if (false) {
                println("then");
            } else {
                println("else");
            }
            """.trimIndent()
        val output = BucketOutput()
        val interpreter = InterpreterFactory.create11(output = output)

        interpreter.interpret(parse(code))

        assertEquals(listOf("else"), output.lines())
    }

    @Test
    fun `a variable declared inside an if block is not visible outside`() {
        val code =
            """
            if (true) {
                let y: number = 20;
                println(y);
            }
            println(y);
            """.trimIndent()
        val output = BucketOutput()
        val interpreter = InterpreterFactory.create11(output = output)

        val error =
            assertFailsWith<UndeclaredVariableError> {
                interpreter.interpret(parse(code))
            }

        assertEquals("y", error.name)
        assertEquals(listOf("20"), output.lines())
    }

    @Test
    fun `shadowing inside an if block does not overwrite the outer variable`() {
        val code =
            """
            let a: string = "outer";
            if (true) {
                let a: string = "inner";
                println(a);
            }
            println(a);
            """.trimIndent()
        val output = BucketOutput()
        val interpreter = InterpreterFactory.create11(output = output)

        interpreter.interpret(parse(code))

        assertEquals(listOf("inner", "outer"), output.lines())
    }
}
