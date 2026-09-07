package printscript.parser.expression

import org.junit.jupiter.api.Test
import printscript.ast.NumberLiteral
import printscript.common.Position
import printscript.common.Token
import printscript.common.TokenType
import printscript.parser.result.ASTResult
import printscript.parser.stream.TokenStream
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PrefixParseletExtensibilityTest {
    private fun pos() = Position(1, 1)

    private fun token(
        type: TokenType,
        value: String = "",
    ) = Token(type, pos(), pos(), value)

    @Test
    fun `custom prefix parselet registrado reemplaza el comportamiento por defecto sin tocar ExpressionParser`() {
        val fakeParselet =
            PrefixParselet { _, _, _ ->
                ASTResult.Success(NumberLiteral(42.0))
            }

        val tokens = listOf(token(TokenType.IDENTIFIER, "x"), token(TokenType.EOF)).iterator()
        val stream = TokenStream(tokens)

        val expressionParser =
            ExpressionParser(
                stream,
                prefixParselets = mapOf(TokenType.IDENTIFIER to fakeParselet),
            )
        val result = expressionParser.parseExpression()

        assertIs<ASTResult.Success<*>>(result)
        val expression = result.value
        assertIs<NumberLiteral>(expression)
        assertEquals(42.0, expression.value)
    }
}
