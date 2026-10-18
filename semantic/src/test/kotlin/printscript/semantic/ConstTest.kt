package printscript.semantic

import printscript.common.LanguageVersion
import printscript.common.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ConstTest {
    @Test
    fun `declaring a const with an initializer succeeds`() {
        val code =
            """
            const x: number = 10;
            println(x);
            """.trimIndent()

        val results = analyze(code, LanguageVersion.V1_1)

        assertEquals(2, results.size)
        assertTrue(results.all { it is SemanticResult.Success })
    }

    @Test
    fun `reassigning a const fails at the assignment`() {
        val code =
            """
            const x: number = 10;
            x = 20;
            """.trimIndent()

        val results = analyze(code, LanguageVersion.V1_1)

        assertIs<SemanticResult.Success<*>>(results.first())
        val failure = assertIs<SemanticResult.Failure>(results.last())
        assertEquals("Cannot reassign constant 'x'.", failure.message)
        assertEquals(Position(2, 1), failure.position)
    }
}
