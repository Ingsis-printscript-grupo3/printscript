package printscript.interpreter

import printscript.common.LanguageVersion
import printscript.interpreter.output.BucketOutput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class VersionSupportTest {
    @Test
    fun `1_0 interpreter rejects if statements`() {
        val code =
            """
            println("before");
            if (true) { println("inside"); }
            """.trimIndent()
        val output = BucketOutput()
        val interpreter = InterpreterFactory.create(LanguageVersion.V1_0, output)

        assertFailsWith<UnknownStatementError> {
            interpreter.interpret(parse(code))
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
        val interpreter = InterpreterFactory.create(LanguageVersion.V1_0, output)

        assertFailsWith<UnknownExpressionError> {
            interpreter.interpret(parse(code))
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
        val interpreter = InterpreterFactory.create(LanguageVersion.V1_0, output)

        assertFailsWith<UnknownExpressionError> {
            interpreter.interpret(parse(code))
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
        val interpreter = InterpreterFactory.create(LanguageVersion.V1_0, output)

        assertFailsWith<UnknownExpressionError> {
            interpreter.interpret(parse(code))
        }

        assertEquals(listOf("before"), output.lines())
    }

    @Test
    fun `1_1 interpreter runs 1_1 features`() {
        val code = "if (true) { println(\"inside\"); }"
        val output = BucketOutput()
        val interpreter = InterpreterFactory.create(LanguageVersion.V1_1, output)

        interpreter.interpret(parse(code))

        assertEquals(listOf("inside"), output.lines())
    }
}
