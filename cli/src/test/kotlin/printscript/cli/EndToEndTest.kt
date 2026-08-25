package printscript.cli
import printscript.interpreter.output.BucketOutput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class EndToEndTest {

    private fun run(code: String): List<String> {
        val bucket = BucketOutput()
        runPrintScript(code, bucket)
        return bucket.lines()
    }

    @Test
    fun `declara una variable y la imprime`() {
        val output =
            run(
                """
                let saludo: string = "hola";
                println(saludo);
                """.trimIndent(),
            )

        assertEquals(listOf("hola"), output)
    }

    @Test
    fun `usa una variable declarada dentro de una operacion`() {
        val output =
            run(
                """
                let x: number = 5;
                let y: number = x * 3;
                println(y);
                """.trimIndent(),
            )

        assertEquals(listOf("15"), output)
    }

    @Test
    fun `reasigna una variable y el print refleja el valor nuevo`() {
        val output =
            run(
                """
                let contador: number = 1;
                println(contador);
                contador = contador + 9;
                println(contador);
                """.trimIndent(),
            )

        assertEquals(listOf("1", "10"), output)
    }

    @Test
    fun `error 1 - lexical error with invalid character`() {
        val code =
            """
            let a: number = 12 @ 4;
            """.trimIndent()

        assertFailsWith<printscript.lexer.LexicalError> {
            run(code)
        }
    }

    @Test
    fun `error 2 - syntax error with missing semicolon`() {
        val code =
            """
            let a: number = 12
            """.trimIndent()

        val exception =
            assertFailsWith<printscript.parser.SyntaxError> {
                run(code)
            }
        assertTrue(exception.message!!.contains("Expected"))
    }

    @Test
    fun `error 3 - semantic error with incompatible types`() {
        val code =
            """
            let a: number = "hola";
            """.trimIndent()

        val exception =
            assertFailsWith<Exception> {
                run(code)
            }
        assertTrue(exception.message!!.contains("Incompatible types"))
    }
}
