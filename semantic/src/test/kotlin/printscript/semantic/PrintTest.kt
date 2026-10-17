package printscript.semantic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PrintTest {
    @Test
    fun `printing a well typed expression succeeds`() {
        val code = "println(1);"

        val results = analyze(code)

        assertTrue(results.all { it is SemanticResult.Success })
    }

    @Test
    fun `printing an expression that fails to resolve propagates the failure`() {
        val code = "println(missing);"

        val results = analyze(code)

        val failure = assertIs<SemanticResult.Failure>(results.single())
        assertEquals("Variable 'missing' not declared.", failure.message)
    }
}
