package printscript.runner

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
    fun `a 1_1 program under version 1_1 passes syntactic and semantic validation`() {
        val engine = Engine(output = BucketOutput())
        val result = engine.validate(program, LanguageVersion.V1_1)

        assertEquals(ExecutionResult.Success, result)
    }

    @Test
    fun `the same program under version 1_0 fails in parser naming the feature`() {
        val result = runEngine(program, LanguageVersion.V1_0)

        assertTrue(result is ExecutionResult.Failure)
        assertEquals("Syntax", result.type)
        assertTrue(result.message.contains("const declarations"))
        assertTrue(result.message.contains("1.0"))
    }

    @Test
    fun `if statement under version 1_0 fails in parser naming the feature`() {
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

    @Test
    fun `reassigning a const variable fails in semantic validation`() {
        val code =
            """
            const a: number = 1;
            a = 2;
            """.trimIndent()

        val result = runEngine(code, LanguageVersion.V1_1)

        assertTrue(result is ExecutionResult.Failure)
        assertEquals("Semantic", result.type)
        assertTrue(result.message.contains("Cannot reassign constant 'a'"))
    }

    @Test
    fun `invalid argument in if fails in semantic validation matching Austral TCK`() {
        val code =
            """
            let a: number = 21;
            if(a) {
                println("this should fail, invalid argument in if statement");
            }
            """.trimIndent()

        val result = runEngine(code, LanguageVersion.V1_1)

        assertTrue(result is ExecutionResult.Failure)
        assertEquals("Semantic", result.type)
        assertTrue(result.message.contains("must be a boolean expression"))
    }

    @Test
    fun `valid if else conditional passes semantic validation in 1_1`() {
        val code =
            """
            const flag: boolean = true;
            if (flag) {
                println("then branch");
            } else {
                println("else branch");
            }
            """.trimIndent()

        val engine = Engine(output = BucketOutput())
        val result = engine.validate(code, LanguageVersion.V1_1)

        assertEquals(ExecutionResult.Success, result)
    }
}
