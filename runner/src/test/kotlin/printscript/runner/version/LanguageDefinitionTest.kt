package printscript.runner.version

import printscript.common.TokenType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

// prueba que cada version arme correctamente las piezas de 1.0 y 1.1
class LanguageDefinitionTest {
    @Test
    fun `version 1_1 adds its own keywords`() {
        val added = PrintScript11.keywords - PrintScript10.keywords.keys

        assertEquals(
            setOf("const", "boolean", "true", "false", "if", "else", "readInput", "readEnv"),
            added.keys,
        )
    }

    @Test
    fun `version 1_0 only knows its own keywords`() {
        assertEquals(setOf("let", "println", "number", "string"), PrintScript10.keywords.keys)
    }

    @Test
    fun `version 1_1 keeps every 1_0 keyword`() {
        val v10 = PrintScript10.keywords
        val v11 = PrintScript11.keywords

        assertEquals(v10, v11.filterKeys { it in v10 })
    }

    @Test
    fun `the 1_1 handler map resolves everything the 1_0 one resolves`() {
        val handlers10 = PrintScript10.statementHandlers.keys
        val handlers11 = PrintScript11.statementHandlers.keys
        val only11 = setOf(TokenType.IF, TokenType.CONST)

        assertEquals(emptySet<TokenType>(), handlers10 - handlers11)
        assertEquals(emptySet<TokenType>(), only11 - handlers11)
        assertEquals(emptySet<TokenType>(), handlers10 intersect only11)
    }

    // se compara la clase y no la instancia: cada version crea la suya de UnaryMinusParselet
    @Test
    fun `the 1_1 prefix parselet map resolves everything the 1_0 one resolves`() {
        val prefix10 = PrintScript10.prefixParselets
        val prefix11 = PrintScript11.prefixParselets
        val only11 = setOf(TokenType.BOOLEANLITERAL, TokenType.READINPUT, TokenType.READENV)

        prefix10.forEach { (type, parselet) ->
            assertEquals(parselet::class, prefix11.getValue(type)::class, "$type")
        }
        assertEquals(emptySet<TokenType>(), only11 - prefix11.keys)
        assertEquals(emptySet<TokenType>(), prefix10.keys intersect only11)
    }

    @Test
    fun `infix parselets are identical in 1_0 and 1_1`() {
        assertEquals(
            setOf(TokenType.PLUS, TokenType.MINUS, TokenType.MULTIPLY, TokenType.DIVIDE),
            PrintScript10.infixParselets.keys,
        )
        assertSame(PrintScript10.infixParselets, PrintScript11.infixParselets)
    }
}
