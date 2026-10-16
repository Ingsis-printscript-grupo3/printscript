package printscript.interpreter

import printscript.common.LanguageVersion
import printscript.interpreter.env.MapEnvProvider
import printscript.interpreter.output.BucketOutput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ReadEnvTest {
    @Test
    fun `readEnv reads an environment variable and converts it to number`() {
        val code =
            """
            let port: number = readEnv("PORT");
            println(port);
            """.trimIndent()
        val env = MapEnvProvider("PORT" to "8080")
        val output = BucketOutput()
        val interpreter = InterpreterFactory.create11(output = output, env = env)

        interpreter.interpret(parse(code, LanguageVersion.V1_1))

        assertEquals(listOf("8080"), output.lines())
    }

    @Test
    fun `readEnv of a missing variable throws EnvVariableNotFoundError`() {
        val code =
            """
            println("before");
            let secret: string = readEnv("SECRET_KEY");
            """.trimIndent()
        val env = MapEnvProvider()
        val output = BucketOutput()
        val interpreter = InterpreterFactory.create11(output = output, env = env)

        val error =
            assertFailsWith<EnvVariableNotFoundError> {
                interpreter.interpret(parse(code, LanguageVersion.V1_1))
            }

        assertEquals("SECRET_KEY", error.name)
        assertEquals(listOf("before"), output.lines())
    }

    @Test
    fun `readEnv with an invalid boolean throws ValueConversionError`() {
        val code =
            """
            println("before");
            let debug: boolean = readEnv("DEBUG");
            """.trimIndent()
        val env = MapEnvProvider("DEBUG" to "maybe")
        val output = BucketOutput()
        val interpreter = InterpreterFactory.create11(output = output, env = env)

        val error =
            assertFailsWith<ValueConversionError> {
                interpreter.interpret(parse(code, LanguageVersion.V1_1))
            }

        assertEquals(StringValue("maybe"), error.value)
        assertEquals("boolean", error.targetType)
        assertEquals(listOf("before"), output.lines())
    }
}
