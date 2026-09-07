package printscript.cli

import printscript.common.LanguageVersion
import printscript.interpreter.output.BucketOutput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Version11EndToEndTest {
    private fun runEngine(
        code: String,
        version: LanguageVersion,
    ): ExecutionResult {
        val engine = Engine(output = BucketOutput())
        return engine.execute(code, version)
    }

    private val program =
        """
        const flag: boolean = true;
        if (flag) {
            println(readInput("name:"));
        } else {
            println(readEnv("HOME"));
        }
        """.trimIndent()

    @Test
    fun `un programa 1_1 bajo version 1_1 no falla en el parser`() {
        val result = runEngine(program, LanguageVersion.V1_1)

        assertTrue(result is ExecutionResult.Failure)
        assertTrue(result.type != "Syntax", "expected the parser to accept the 1.1 program, but got: $result")
    }

    @Test
    fun `el mismo programa bajo version 1_0 falla en el parser nombrando la feature`() {
        val result = runEngine(program, LanguageVersion.V1_0)

        assertTrue(result is ExecutionResult.Failure)
        assertEquals("Syntax", result.type)
        assertTrue(result.message.contains("const declarations"))
        assertTrue(result.message.contains("1.0"))
    }

    @Test
    fun `if bajo version 1_0 falla en el parser nombrando la feature`() {
        val code =
            """
            if (true) {
                println("hi");
            }
            """.trimIndent()

        val result = runEngine(code, LanguageVersion.V1_0)

        assertTrue(result is ExecutionResult.Failure)
        assertEquals("Syntax", result.type)
        assertTrue(result.message.contains("if statements"))
        assertTrue(result.message.contains("1.0"))
    }
}
