package printscript.parser.expression

import printscript.ast.Expression
import printscript.common.LanguageVersion
import printscript.common.Position
import printscript.common.Token
import printscript.common.TokenType
import printscript.parser.result.ASTResult
import printscript.parser.stream.TokenStream
import kotlin.test.Test
import kotlin.test.assertEquals

class ExpressionPositionTest {
    private fun token(
        type: TokenType,
        value: String = "",
        line: Int = 1,
        column: Int = 1,
    ) = Token(type, Position(line, column), Position(line, column + value.length), value)

    private fun parseExpression(vararg tokens: Token): Expression {
        val tokenList = tokens.toList() + token(TokenType.EOF)
        val stream = TokenStream(tokenList.iterator())
        val expressionParser =
            ExpressionParser(
                stream,
                DefaultExpressionParselets.prefix(LanguageVersion.V1_1),
                DefaultExpressionParselets.infix,
            )
        return when (val result = expressionParser.parseExpression()) {
            is ASTResult.Success -> result.value
            is ASTResult.Failure -> throw RuntimeException(result.message)
        }
    }

    @Test
    fun `a number literal takes the position of its own token`() {
        val expression = parseExpression(token(TokenType.NUMBERLITERAL, "5", line = 2, column = 3))
        assertEquals(Position(2, 3), expression.position)
    }

    @Test
    fun `a string literal takes the position of its own token`() {
        val expression = parseExpression(token(TokenType.STRINGLITERAL, "hi", line = 4, column = 1))
        assertEquals(Position(4, 1), expression.position)
    }

    @Test
    fun `an identifier takes the position of its own token`() {
        val expression = parseExpression(token(TokenType.IDENTIFIER, "x", line = 6, column = 9))
        assertEquals(Position(6, 9), expression.position)
    }

    @Test
    fun `a boolean literal takes the position of its own token`() {
        val expression = parseExpression(token(TokenType.BOOLEANLITERAL, "true", line = 8, column = 2))
        assertEquals(Position(8, 2), expression.position)
    }

    @Test
    fun `a binary expression takes the position of its left operand`() {
        val expression =
            parseExpression(
                token(TokenType.NUMBERLITERAL, "1", line = 5, column = 4),
                token(TokenType.PLUS, "+", line = 5, column = 6),
                token(TokenType.NUMBERLITERAL, "2", line = 5, column = 8),
            )
        assertEquals(Position(5, 4), expression.position)
    }

    @Test
    fun `nested parenthesized expressions keep the position of their inner value`() {
        val expression =
            parseExpression(
                token(TokenType.LEFTPAREN, "(", line = 1, column = 1),
                token(TokenType.NUMBERLITERAL, "5", line = 1, column = 2),
                token(TokenType.RIGHTPAREN, ")", line = 1, column = 3),
            )
        assertEquals(Position(1, 2), expression.position)
    }
}
