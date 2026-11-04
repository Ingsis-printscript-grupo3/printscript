package printscript.runner

import printscript.common.LanguageVersion
import printscript.common.TokenType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

// prueba que la tabla de versiones arme correctamente las piezas de 1.0 y 1.1
class LanguageSpecTest {
    @Test
    fun `version 1_1 adds its own keywords`() {
        val spec10 = languageSpecFor(LanguageVersion.V1_0)
        val spec11 = languageSpecFor(LanguageVersion.V1_1)
        val added = spec11.keywords - spec10.keywords.keys

        assertEquals(
            setOf("const", "boolean", "true", "false", "if", "else", "readInput", "readEnv"),
            added.keys,
        )
    }

    @Test
    fun `version 1_0 only knows its own keywords`() {
        val spec10 = languageSpecFor(LanguageVersion.V1_0)
        assertEquals(setOf("let", "println", "number", "string"), spec10.keywords.keys)
    }

    @Test
    fun `version 1_1 keeps every 1_0 keyword`() {
        val v10 = languageSpecFor(LanguageVersion.V1_0).keywords
        val v11 = languageSpecFor(LanguageVersion.V1_1).keywords

        assertEquals(v10, v11.filterKeys { it in v10 })
    }

    @Test
    fun `the 1_1 handler map resolves everything the 1_0 one resolves`() {
        val spec10 = languageSpecFor(LanguageVersion.V1_0)
        val spec11 = languageSpecFor(LanguageVersion.V1_1)
        val only11 = setOf(TokenType.IF, TokenType.CONST)

        assertEquals(emptySet<TokenType>(), spec10.statementHandlers.keys - spec11.statementHandlers.keys)
        assertEquals(emptySet<TokenType>(), only11 - spec11.statementHandlers.keys)
        assertEquals(emptySet<TokenType>(), spec10.statementHandlers.keys intersect only11)
    }

    @Test
    fun `the 1_1 prefix parselet map resolves everything the 1_0 one resolves`() {
        val spec10 = languageSpecFor(LanguageVersion.V1_0)
        val spec11 = languageSpecFor(LanguageVersion.V1_1)
        val only11 = setOf(TokenType.BOOLEANLITERAL, TokenType.READINPUT, TokenType.READENV)

        spec10.prefixParselets.forEach { (type, parselet) ->
            assertSame(parselet, spec11.prefixParselets[type], "$type")
        }
        assertEquals(emptySet<TokenType>(), only11 - spec11.prefixParselets.keys)
        assertEquals(emptySet<TokenType>(), spec10.prefixParselets.keys intersect only11)
    }

    @Test
    fun `infix parselets are identical in 1_0 and 1_1`() {
        val spec10 = languageSpecFor(LanguageVersion.V1_0)
        val spec11 = languageSpecFor(LanguageVersion.V1_1)

        assertEquals(
            setOf(TokenType.PLUS, TokenType.MINUS, TokenType.MULTIPLY, TokenType.DIVIDE),
            spec10.infixParselets.keys,
        )
        assertEquals(spec10.infixParselets, spec11.infixParselets)
    }
}
