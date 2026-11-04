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
        val interpreter = testInterpreter(output = output)

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
        val interpreter = testInterpreter(output = output)

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
        val interpreter = testInterpreter(output = output)

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
        val interpreter = testInterpreter(output = output)

        interpreter.interpret(parse(code))

        assertEquals(listOf("inner", "outer"), output.lines())
    }

    @Test
    fun `an if without else runs nothing when the condition is false`() {
        val code =
            """
            if (false) {
                println("then");
            }
            println("after");
            """.trimIndent()
        val output = BucketOutput()
        val interpreter = testInterpreter(output = output)

        interpreter.interpret(parse(code))

        assertEquals(listOf("after"), output.lines())
    }

    @Test
    fun `a condition that is not a boolean throws ConditionTypeError`() {
        val code = "if (5) { println(\"then\"); }"
        val output = BucketOutput()
        val interpreter = testInterpreter(output = output)

        val error =
            assertFailsWith<ConditionTypeError> {
                interpreter.interpret(parse(code))
            }

        assertEquals("number", error.actualType)
        assertEquals(emptyList(), output.lines())
    }

    // el mismo interprete corre dos programas: el segundo ve lo que dejo el primero
    @Test
    fun `an if block discards its variables even when a statement inside fails`() {
        val interpreter = testInterpreter(output = BucketOutput())

        assertFailsWith<UndeclaredVariableError> {
            interpreter.interpret(parse("if (true) { let y: number = 1; println(z); }"))
        }
        val error =
            assertFailsWith<UndeclaredVariableError> {
                interpreter.interpret(parse("println(y);"))
            }

        assertEquals("y", error.name)
    }
}
