package printscript.interpreter

import printscript.common.Position
import printscript.interpreter.output.BucketOutput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class VariablesTest {
    @Test
    fun `reassignment of an already declared variable`() {
        val code =
            """
            let a: number = 12;
            let b: number = 4;
            a = a / b;
            println("Result: " + a);
            """.trimIndent()
        val output = BucketOutput()
        val interpreter = InterpreterFactory.create10(output)

        interpreter.interpret(parse(code))

        assertEquals(listOf("Result: 3"), output.lines())
    }

    @Test
    fun `declaration without initializer allows subsequent assignment`() {
        val code =
            """
            let x: number;
            x = 10;
            println(x);
            """.trimIndent()
        val output = BucketOutput()
        val interpreter = InterpreterFactory.create10(output)

        interpreter.interpret(parse(code))

        assertEquals(listOf("10"), output.lines())
    }

    @Test
    fun `accessing an uninitialized variable throws UninitializedVariableError`() {
        val code =
            """
            println("before");
            let x: number;
            println(x);
            """.trimIndent()
        val output = BucketOutput()
        val interpreter = InterpreterFactory.create10(output)

        val error =
            assertFailsWith<UninitializedVariableError> {
                interpreter.interpret(parse(code))
            }

        assertEquals("x", error.name)
        assertEquals(listOf("before"), output.lines())
    }

    @Test
    fun `using an undeclared variable is an error`() {
        val code =
            """
            println("before");
            println(x);
            """.trimIndent()
        val output = BucketOutput()
        val interpreter = InterpreterFactory.create10(output)

        val error =
            assertFailsWith<UndeclaredVariableError> {
                interpreter.interpret(parse(code))
            }

        assertEquals("x", error.name)
        assertEquals(listOf("before"), output.lines())
    }

    // la posicion es la del identificador que no existe, no la del println que lo contiene
    @Test
    fun `an undeclared variable error points at the identifier`() {
        val code =
            """
            let a: number = 1;
            println(x);
            """.trimIndent()
        val output = BucketOutput()
        val interpreter = InterpreterFactory.create10(output)

        val error =
            assertFailsWith<UndeclaredVariableError> {
                interpreter.interpret(parse(code))
            }

        assertEquals(Position(2, 9), error.start)
        assertEquals(error.start, error.end)
    }

    @Test
    fun `declaration with an incompatible type throws ValueConversionError`() {
        val code =
            """
            println("before");
            let x: number = "not_a_number";
            """.trimIndent()
        val output = BucketOutput()
        val interpreter = InterpreterFactory.create10(output)

        val error =
            assertFailsWith<ValueConversionError> {
                interpreter.interpret(parse(code))
            }

        assertEquals(StringValue("not_a_number"), error.value)
        assertEquals(listOf("before"), output.lines())
    }

    @Test
    fun `assignment with an incompatible type throws ValueConversionError`() {
        val code =
            """
            let x: number = 1;
            println(x);
            x = "invalid";
            """.trimIndent()
        val output = BucketOutput()
        val interpreter = InterpreterFactory.create10(output)

        val error =
            assertFailsWith<ValueConversionError> {
                interpreter.interpret(parse(code))
            }

        assertEquals(StringValue("invalid"), error.value)
        assertEquals(listOf("1"), output.lines())
    }
}
