package printscript.interpreter

import printscript.interpreter.input.QueueInput
import printscript.interpreter.output.BucketOutput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ReadInputTest {
    @Test
    fun `readInput converts to number and can be used in arithmetic`() {
        val code =
            """
            let age: number = readInput("Enter age: ");
            let nextAge: number = age + 1;
            println(nextAge);
            """.trimIndent()
        val input = QueueInput("25")
        val output = BucketOutput()
        val interpreter = InterpreterFactory.create11(output = output, input = input)

        interpreter.interpret(parse(code))

        assertEquals(listOf("26"), output.lines())
    }

    @Test
    fun `readInput converts to boolean`() {
        val code =
            """
            let active: boolean = readInput("Is active: ");
            println(active);
            """.trimIndent()
        val input = QueueInput("true")
        val output = BucketOutput()
        val interpreter = InterpreterFactory.create11(output = output, input = input)

        interpreter.interpret(parse(code))

        assertEquals(listOf("true"), output.lines())
    }

    @Test
    fun `readInput with an invalid number throws ValueConversionError`() {
        val code =
            """
            println("before");
            let n: number = readInput("Enter: ");
            """.trimIndent()
        val input = QueueInput("not_a_number")
        val output = BucketOutput()
        val interpreter = InterpreterFactory.create11(output = output, input = input)

        val error =
            assertFailsWith<ValueConversionError> {
                interpreter.interpret(parse(code))
            }

        assertEquals(StringValue("not_a_number"), error.value)
        assertEquals("number", error.targetType)
        assertEquals(listOf("before"), output.lines())
    }

    @Test
    fun `assigning readInput converts to the declared type`() {
        val code =
            """
            let x: number = 10;
            x = readInput("Enter: ");
            println(x);
            """.trimIndent()
        val input = QueueInput("50")
        val output = BucketOutput()
        val interpreter = InterpreterFactory.create11(output = output, input = input)

        interpreter.interpret(parse(code))

        assertEquals(listOf("50"), output.lines())
    }

    @Test
    fun `assigning readInput with an invalid value throws ValueConversionError`() {
        val code =
            """
            let x: number = 10;
            println(x);
            x = readInput("Enter: ");
            """.trimIndent()
        val input = QueueInput("not_a_number")
        val output = BucketOutput()
        val interpreter = InterpreterFactory.create11(output = output, input = input)

        val error =
            assertFailsWith<ValueConversionError> {
                interpreter.interpret(parse(code))
            }

        assertEquals(StringValue("not_a_number"), error.value)
        assertEquals("number", error.targetType)
        assertEquals(listOf("10"), output.lines())
    }
}
