package printscript.parser.expression

import printscript.parser.result.ASTResult
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class EndOfInputTest {
    @Test
    fun `an expression cut by the end of the file names the end of input`() {
        val result = parseExpression("1 +")

        val failure = assertIs<ASTResult.Failure>(result)
        assertTrue(failure.message.contains("found end of input"), failure.message)
        assertFalse(failure.message.contains("found ''"), failure.message)
    }
}
