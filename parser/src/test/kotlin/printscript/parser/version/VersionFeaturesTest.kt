package printscript.parser.version

import printscript.common.LanguageVersion
import printscript.common.Position
import printscript.common.Token
import printscript.common.TokenType
import printscript.parser.expression.DefaultExpressionParselets
import printscript.parser.statement.DefaultStatementHandlers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VersionFeaturesTest {
    private fun token(
        type: TokenType,
        value: String = "",
    ) = Token(type, Position(2, 5), Position(2, 9), value)

    private val featuresOf11 =
        listOf(
            TokenType.IF,
            TokenType.CONST,
            TokenType.BOOLEANTYPE,
            TokenType.BOOLEANLITERAL,
            TokenType.READINPUT,
            TokenType.READENV,
        )

    @Test
    fun `every 1_1 feature is unavailable in 1_0 and available in 1_1`() {
        featuresOf11.forEach { type ->
            assertNotNull(VersionFeatures.unavailable(token(type), LanguageVersion.V1_0), "$type en 1.0")
            assertNull(VersionFeatures.unavailable(token(type), LanguageVersion.V1_1), "$type en 1.1")
        }
    }

    @Test
    fun `a token that belongs to no versioned feature is always available`() {
        listOf(TokenType.LET, TokenType.PRINTLN, TokenType.IDENTIFIER, TokenType.NUMBERLITERAL).forEach { type ->
            assertNull(VersionFeatures.unavailable(token(type), LanguageVersion.V1_0))
            assertTrue(VersionFeatures.availableIn(type, LanguageVersion.V1_0))
        }
    }

    @Test
    fun `the error names the feature, the version it needs and the one that was asked for`() {
        val failure = VersionFeatures.unavailable(token(TokenType.READINPUT), LanguageVersion.V1_0)

        assertNotNull(failure)
        assertEquals(
            "'readInput' requires PrintScript 1.1, but version 1.0 was requested.",
            failure.message,
        )
    }

    @Test
    fun `the error carries the position of the offending token`() {
        val failure = VersionFeatures.unavailable(token(TokenType.IF, "if"), LanguageVersion.V1_0)

        assertNotNull(failure)
        assertEquals(Position(2, 5), failure.start)
        assertEquals(Position(2, 9), failure.end)
    }

    @Test
    fun `availableIn agrees with unavailable`() {
        featuresOf11.forEach { type ->
            assertTrue(!VersionFeatures.availableIn(type, LanguageVersion.V1_0))
            assertTrue(VersionFeatures.availableIn(type, LanguageVersion.V1_1))
        }
    }

    // los mapas de 1.1 se montan sobre los de 1.0: todo lo que 1.0 resolvia, 1.1 lo resuelve igual
    @Test
    fun `the 1_1 handler map contains everything the 1_0 one has`() {
        val handlers10 = DefaultStatementHandlers.map(LanguageVersion.V1_0)
        val handlers11 = DefaultStatementHandlers.map(LanguageVersion.V1_1)

        handlers10.forEach { (type, handler) -> assertEquals(handler, handlers11[type], "$type") }
        assertTrue(handlers11.keys.containsAll(listOf(TokenType.IF, TokenType.CONST)))
        assertTrue(handlers10.keys.none { it == TokenType.IF || it == TokenType.CONST })
    }

    @Test
    fun `the 1_1 prefix parselet map contains everything the 1_0 one has`() {
        val prefix10 = DefaultExpressionParselets.prefix(LanguageVersion.V1_0)
        val prefix11 = DefaultExpressionParselets.prefix(LanguageVersion.V1_1)

        prefix10.forEach { (type, parselet) -> assertEquals(parselet, prefix11[type], "$type") }
        assertTrue(
            prefix11.keys.containsAll(
                listOf(TokenType.BOOLEANLITERAL, TokenType.READINPUT, TokenType.READENV),
            ),
        )
        assertTrue(prefix10.keys.none { it in setOf(TokenType.BOOLEANLITERAL, TokenType.READINPUT) })
    }
}
