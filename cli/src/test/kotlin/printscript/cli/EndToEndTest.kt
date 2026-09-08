package printscript.cli

import printscript.interpreter.output.BucketOutput
import java.io.StringReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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

    @Test
    fun `the semantic error reports the line of the failing statement`() {
        val code =
            """
            let a: number = 1;
            let b: string = 2;
            """.trimIndent()

        val (result, _) = runEngine(code)
        assertTrue(result is ExecutionResult.Failure)
        assertEquals("Semantic", result.type)
        assertEquals(2, result.start?.line)
        // apunta al let, que es donde arranca el statement
        assertEquals(1, result.start?.column)
        // el semantico no produce un end propio: el AST guarda un solo punto por nodo
        assertEquals(result.start, result.end)
    }

    @Test
    fun `a lexical error carries the range of the offending character`() {
        val (result, _) = runEngine("let a: number = 12 @ 4;")

        assertTrue(result is ExecutionResult.Failure)
        assertEquals("Lexical", result.type)
        assertEquals(1, result.start?.line)
        assertEquals(20, result.start?.column)
        assertEquals(21, result.end?.column)
    }

    @Test
    fun `a syntax error carries a range, not just a line`() {
        val (result, _) = runEngine("println(5)")

        assertTrue(result is ExecutionResult.Failure)
        assertEquals("Syntax", result.type)
        assertEquals(1, result.start?.line)
        assertEquals(1, result.end?.line)
        assertNotNull(result.start?.column)
        assertNotNull(result.end?.column)
    }

    @Test
    fun `the engine reports progress once per parsed statement`() {
        val reported = mutableListOf<Int>()
        val code = "let a: number = 1;\nlet b: number = 2;\nprintln(a + b);"

        Engine(BucketOutput()).validate(StringReader(code), onProgress = reported::add)

        assertEquals(listOf(1, 2, 3), reported)
    }
}
