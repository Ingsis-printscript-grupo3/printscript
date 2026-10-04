package printscript.parser.expression

import printscript.common.LanguageVersion
import printscript.common.Position
import printscript.common.Token
import printscript.common.TokenType
import printscript.parser.result.ASTResult
import printscript.parser.stream.TokenStream
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class EndOfInputTest {
    private fun token(
        type: TokenType,
        value: String = "",
    ) = Token(type, Position(1, 1), Position(1, 1), value)

    @Test
    fun `an expression cut by the end of the file names the end of input`() {
        val tokens =
            listOf(
                token(TokenType.NUMBERLITERAL, "1"),
                token(TokenType.PLUS, "+"),
                token(TokenType.EOF),
            )
        val parser = ExpressionParser(TokenStream(tokens.iterator()), LanguageVersion.V1_1)

        val result = parser.parseExpression()

        val failure = assertIs<ASTResult.Failure>(result)
        assertTrue(failure.message.contains("found end of input"), failure.message)
        assertFalse(failure.message.contains("found ''"), failure.message)
    }
}
