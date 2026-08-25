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
    fun `declara una variable y la imprime`() {
        val (result, output) =
            runEngine(
                """
                let saludo: string = "hola";
                println(saludo);
                """.trimIndent(),
            )

        assertTrue(result is ExecutionResult.Success)
        assertEquals(listOf("hola"), output)
    }

    @Test
    fun `usa una variable declarada dentro de una operacion`() {
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
    fun `reasigna una variable y el print refleja el valor nuevo`() {
        val (result, output) =
            runEngine(
                """
                let contador: number = 1;
                println(contador);
                contador = contador + 9;
                println(contador);
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
            let a: number = "hola";
            """.trimIndent()

        val (result, _) = runEngine(code)
        assertTrue(result is ExecutionResult.Failure)
        assertEquals("Semantic", result.type)
        assertTrue(result.message.contains("Incompatible types"))
    }
}
