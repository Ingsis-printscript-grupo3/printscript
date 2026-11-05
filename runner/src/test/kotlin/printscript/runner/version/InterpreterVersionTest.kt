package printscript.runner.version

import printscript.common.LanguageVersion
import printscript.interpreter.Interpreter
import printscript.interpreter.UnknownExpressionError
import printscript.interpreter.UnknownStatementError
import printscript.interpreter.env.MapEnvProvider
import printscript.interpreter.input.QueueInput
import printscript.interpreter.output.BucketOutput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class InterpreterVersionTest {
    private fun interpreter(
        definition: LanguageDefinition,
        output: BucketOutput,
    ) = Interpreter(
        definition.statementInterpreters(output),
        definition.expressionEvaluators(QueueInput(), MapEnvProvider()),
    )

    @Test
    fun `each version maps to its own definition`() {
        assertSame(PrintScript10, LanguageVersion.V1_0.definition())
        assertSame(PrintScript11, LanguageVersion.V1_1.definition())
    }

    @Test
    fun `1_0 interpreter rejects if statements`() {
        val code =
            """
            println("before");
            if (true) { println("inside"); }
            """.trimIndent()
        val output = BucketOutput()

        assertFailsWith<UnknownStatementError> {
            interpreter(PrintScript10, output).interpret(parse11(code))
        }

        assertEquals(listOf("before"), output.lines())
    }

    @Test
    fun `1_0 interpreter rejects boolean literals`() {
        val code =
            """
            println("before");
            println(true);
            """.trimIndent()
        val output = BucketOutput()

        assertFailsWith<UnknownExpressionError> {
            interpreter(PrintScript10, output).interpret(parse11(code))
        }

        assertEquals(listOf("before"), output.lines())
    }

    @Test
    fun `1_0 interpreter rejects readInput`() {
        val code =
            """
            println("before");
            println(readInput("prompt"));
            """.trimIndent()
        val output = BucketOutput()

        assertFailsWith<UnknownExpressionError> {
            interpreter(PrintScript10, output).interpret(parse11(code))
        }

        assertEquals(listOf("before"), output.lines())
    }

    @Test
    fun `1_0 interpreter rejects readEnv`() {
        val code =
            """
            println("before");
            println(readEnv("MY_VAR"));
            """.trimIndent()
        val output = BucketOutput()

        assertFailsWith<UnknownExpressionError> {
            interpreter(PrintScript10, output).interpret(parse11(code))
        }

        assertEquals(listOf("before"), output.lines())
    }

    @Test
    fun `1_1 interpreter runs if statements`() {
        val output = BucketOutput()

        interpreter(PrintScript11, output).interpret(parse11("if (true) { println(\"inside\"); }"))

        assertEquals(listOf("inside"), output.lines())
    }
}
