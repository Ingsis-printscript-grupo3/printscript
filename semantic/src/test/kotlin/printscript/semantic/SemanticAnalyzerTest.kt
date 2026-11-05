package printscript.semantic

import printscript.common.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SemanticAnalyzerTest {
    @Test
    fun `yields a success per well typed statement`() {
        val code =
            """
            let x: number = 1;
            println(x);
            """.trimIndent()

        val results = analyze(code)

        assertEquals(2, results.size)
        assertTrue(results.all { it is SemanticResult.Success })
    }

    @Test
    fun `stops at the first failure and reports the statement position`() {
        val code =
            """
            let a: number = 1;
            let x: number = "oops";
            println(1);
            """.trimIndent()

        val results = analyze(code)

        assertEquals(2, results.size)
        val failure = assertIs<SemanticResult.Failure>(results.last())
        assertEquals(Position(2, 1), failure.position)
    }

    @Test
    fun `an empty program yields no results`() {
        val code = ""

        val results = analyze(code)

        assertTrue(results.isEmpty())
    }
}
