package printscript.parser

import printscript.common.LanguageVersion
import printscript.common.TokenType
import printscript.parser.expression.DefaultExpressionParselets
import printscript.parser.statement.DefaultStatementHandlers
import kotlin.test.Test
import kotlin.test.assertTrue

// los mapas de 1.1 se montan sobre los de 1.0: todo lo que 1.0 resolvia, 1.1 lo resuelve
class VersionMapsTest {
    @Test
    fun `the 1_1 handler map resolves everything the 1_0 one resolves`() {
        val handlers10 = DefaultStatementHandlers.map(LanguageVersion.V1_0)
        val handlers11 = DefaultStatementHandlers.map(LanguageVersion.V1_1)

        assertTrue(handlers11.keys.containsAll(handlers10.keys))
        assertTrue(handlers11.keys.containsAll(listOf(TokenType.IF, TokenType.CONST)))
        assertTrue(handlers10.keys.none { it == TokenType.IF || it == TokenType.CONST })
    }

    @Test
    fun `the 1_1 prefix parselet map resolves everything the 1_0 one resolves`() {
        val prefix10 = DefaultExpressionParselets.prefix(LanguageVersion.V1_0)
        val prefix11 = DefaultExpressionParselets.prefix(LanguageVersion.V1_1)

        prefix10.forEach { (type, parselet) -> assertTrue(prefix11[type] === parselet, "$type") }
        assertTrue(
            prefix11.keys.containsAll(
                listOf(TokenType.BOOLEANLITERAL, TokenType.READINPUT, TokenType.READENV),
            ),
        )
        assertTrue(prefix10.keys.none { it in setOf(TokenType.BOOLEANLITERAL, TokenType.READINPUT) })
    }
}
