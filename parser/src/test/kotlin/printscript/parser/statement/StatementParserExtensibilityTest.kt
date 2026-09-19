package printscript.parser.statement

import org.junit.jupiter.api.Test
import printscript.ast.Assignment
import printscript.ast.NumberLiteral
import printscript.common.LanguageVersion
import printscript.common.Position
import printscript.common.Token
import printscript.common.TokenType
import printscript.parser.expression.ExpressionParser
import printscript.parser.result.ASTResult
import printscript.parser.stream.TokenStream
import kotlin.test.assertEquals
import kotlin.test.assertIs

class StatementParserExtensibilityTest {
    private fun pos() = Position(1, 1)

    private fun token(
        type: TokenType,
        value: String = "",
    ) = Token(type, pos(), pos(), value)

    @Test
    fun `a registered custom handler replaces the default behaviour without touching StatementParser`() {
        val fakeHandler =
            StatementHandler { _, _, _ ->
                ASTResult.Success(Assignment("injected", NumberLiteral(1.0)))
            }

        val tokens = listOf(token(TokenType.LET), token(TokenType.EOF)).iterator()
        val stream = TokenStream(tokens)
        val expressionParser = ExpressionParser(stream, LanguageVersion.V1_1)

        val statementParser =
            StatementParser(
                stream,
                expressionParser,
                version = LanguageVersion.V1_1,
                handlers = mapOf(TokenType.LET to fakeHandler),
            )
        val result = statementParser.parseStatement()

        assertIs<ASTResult.Success<*>>(result)
        val statement = result.value
        assertIs<Assignment>(statement)
        assertEquals("injected", statement.name)
    }
}
