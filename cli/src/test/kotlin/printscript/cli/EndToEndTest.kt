package printscript.cli

import printscript.interpreter.output.BucketOutput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EndToEndTest {
    private fun runEngine(code: String): Pair<ExecutionResult, List<String>> {
        val bucket = BucketOutput()
        val engine = Engine(output = bucket)
        val result = engine.execute(code)
        return Pair(result, bucket.lines())
    }

    @Test
    fun `declares a variable and prints it`() {
        val (result, output) =
            runEngine(
                """
                let greeting: string = "hello";
                println(greeting);
                """.trimIndent(),
            )

        assertTrue(result is ExecutionResult.Success)
        assertEquals(listOf("hello"), output)
    }

    @Test
    fun `uses a declared variable inside an operation`() {
        val (result, output) =
            runEngine(
                """
                let x: number = 5;
                let y: number = x * 3;
                println(y);
                """.trimIndent(),
            )

        assertTrue(result is ExecutionResult.Success)
        assertEquals(listOf("15"), output)
    }

    @Test
    fun `reassigns a variable and the print reflects the new value`() {
        val (result, output) =
            runEngine(
                """
                let counter: number = 1;
                println(counter);
                counter = counter + 9;
                println(counter);
                """.trimIndent(),
            )

        assertTrue(result is ExecutionResult.Success)
        assertEquals(listOf("1", "10"), output)
    }

    @Test
    fun `error 1 - lexical error with invalid character`() {
        val code =
            """
            let a: number = 12 @ 4;
            """.trimIndent()

        val (result, _) = runEngine(code)
        assertTrue(result is ExecutionResult.Failure)
        assertEquals("Lexical", result.type)
    }

    @Test
    fun `error 2 - syntax error with missing semicolon`() {
        val code =
            """
            let a: number = 12
            """.trimIndent()

        val (result, _) = runEngine(code)
        assertTrue(result is ExecutionResult.Failure)
        assertEquals("Syntax", result.type)
        assertTrue(result.message.contains("Expected"))
    }

    @Test
    fun `error 3 - semantic error with incompatible types`() {
        val code =
            """
            let a: number = "hello";
            """.trimIndent()

        val (result, _) = runEngine(code)
        assertTrue(result is ExecutionResult.Failure)
        assertEquals("Semantic", result.type)
        assertTrue(result.message.contains("Incompatible types"))
    }

    @Test
    fun `error 4 - runtime error when using an uninitialized variable`() {
        val code =
            """
            let x: number;
            println(x);
            """.trimIndent()

        val (result, _) = runEngine(code)
        assertTrue(result is ExecutionResult.Failure)
        assertEquals("Runtime", result.type)
    }
}
