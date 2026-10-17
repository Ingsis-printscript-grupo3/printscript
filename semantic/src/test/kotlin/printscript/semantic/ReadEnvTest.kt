package printscript.semantic

import printscript.common.LanguageVersion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReadEnvTest {
    @Test
    fun `readEnv in a declaration takes the declared type`() {
        val code =
            """
            let n: number = readEnv("SOME_ENV");
            let s: string = readEnv("SOME_ENV");
            let b: boolean = readEnv("SOME_ENV");
            """.trimIndent()

        val results = analyze(code, LanguageVersion.V1_1)

        assertEquals(3, results.size)
        assertTrue(results.all { it is SemanticResult.Success })
    }

    @Test
    fun `readEnv in an assignment takes the type of the variable`() {
        val code =
            """
            let port: number;
            port = readEnv("PORT");
            """.trimIndent()

        val results = analyze(code, LanguageVersion.V1_1)

        assertEquals(2, results.size)
        assertTrue(results.all { it is SemanticResult.Success })
    }

    @Test
    fun `printing readEnv defaults to string`() {
        val code = "println(readEnv(\"USER\"));"

        val results = analyze(code, LanguageVersion.V1_1)

        assertTrue(results.all { it is SemanticResult.Success })
    }
}
