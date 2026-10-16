package printscript.interpreter

import printscript.interpreter.output.BucketOutput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ArithmeticTest {
    @Test
    fun `concatenation of two string variables`() {
        val code =
            """
            let name: string = "Joe";
            let lastName: string = "Doe";
            println(name + " " + lastName);
            """.trimIndent()
        val output = BucketOutput()
        val interpreter = InterpreterFactory.create10(output)

        interpreter.interpret(parse(code))

        assertEquals(listOf("Joe Doe"), output.lines())
    }

    @Test
    fun `division stored in a variable and concatenated`() {
        val code =
            """
            let a: number = 12;
            let b: number = 4;
            let c: number = a / b;
            println("Result: " + c);
            """.trimIndent()
        val output = BucketOutput()
        val interpreter = InterpreterFactory.create10(output)

        interpreter.interpret(parse(code))

        assertEquals(listOf("Result: 3"), output.lines())
    }

    @Test
    fun `adding two numbers gives a number and does not concatenate them`() {
        val code = "println(1 + 2);"
        val output = BucketOutput()
        val interpreter = InterpreterFactory.create10(output)

        interpreter.interpret(parse(code))

        assertEquals(listOf("3"), output.lines())
    }

    @Test
    fun `a result with decimals keeps the decimals`() {
        val code = "println(7 / 2);"
        val output = BucketOutput()
        val interpreter = InterpreterFactory.create10(output)

        interpreter.interpret(parse(code))

        assertEquals(listOf("3.5"), output.lines())
    }

    @Test
    fun `subtracting on a string is a type error`() {
        val code =
            """
            println("before");
            println("hola" - 1);
            """.trimIndent()
        val output = BucketOutput()
        val interpreter = InterpreterFactory.create10(output)

        val error =
            assertFailsWith<TypeMismatchError> {
                interpreter.interpret(parse(code))
            }

        assertEquals("string", error.leftType)
        assertEquals("number", error.rightType)
        assertEquals(listOf("before"), output.lines())
    }

    @Test
    fun `evaluates negative number literals and unary minus`() {
        val code =
            """
            let negLit: number = -5;
            let negated: number = -negLit;
            println(negLit);
            println(negated);
            """.trimIndent()
        val output = BucketOutput()
        val interpreter = InterpreterFactory.create10(output)

        interpreter.interpret(parse(code))

        assertEquals(listOf("-5", "5"), output.lines())
    }

    @Test
    fun `evaluates chained minus and subtraction of negative values`() {
        val code =
            """
            println(5 - -3);
            println(0 - -5);
            """.trimIndent()
        val output = BucketOutput()
        val interpreter = InterpreterFactory.create10(output)

        interpreter.interpret(parse(code))

        assertEquals(listOf("8", "5"), output.lines())
    }
}
