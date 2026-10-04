package printscript.parser

import printscript.common.LanguageVersion
import printscript.common.TokenType
import printscript.parser.expression.DefaultExpressionParselets
import printscript.parser.statement.DefaultStatementHandlers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

// los mapas de 1.1 se montan sobre los de 1.0: todo lo que 1.0 resolvia, 1.1 lo resuelve
class VersionMapsTest {
    @Test
    fun `the 1_1 handler map resolves everything the 1_0 one resolves`() {
        val handlers10 = DefaultStatementHandlers.map(LanguageVersion.V1_0)
        val handlers11 = DefaultStatementHandlers.map(LanguageVersion.V1_1)
        val only11 = setOf(TokenType.IF, TokenType.CONST)

        assertEquals(emptySet<TokenType>(), handlers10.keys - handlers11.keys)
        assertEquals(emptySet<TokenType>(), only11 - handlers11.keys)
        assertEquals(emptySet<TokenType>(), handlers10.keys intersect only11)
    }

    @Test
    fun `the 1_1 prefix parselet map resolves everything the 1_0 one resolves`() {
        val prefix10 = DefaultExpressionParselets.prefix(LanguageVersion.V1_0)
        val prefix11 = DefaultExpressionParselets.prefix(LanguageVersion.V1_1)
        val only11 = setOf(TokenType.BOOLEANLITERAL, TokenType.READINPUT, TokenType.READENV)

        prefix10.forEach { (type, parselet) -> assertSame(parselet, prefix11[type], "$type") }
        assertEquals(emptySet<TokenType>(), only11 - prefix11.keys)
        assertEquals(emptySet<TokenType>(), prefix10.keys intersect only11)
    }
}
