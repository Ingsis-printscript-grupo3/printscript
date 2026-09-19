package printscript.lexer

import printscript.common.Position
import printscript.common.Token
import printscript.common.TokenType
import printscript.lexer.plugin.TokenReader
import java.io.ByteArrayInputStream
import java.io.StringReader
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LexerFactoryTest {
    private fun typesOf(lexer: LexerInterface): List<TokenType> = lexer.tokenize().asSequence().map { it.type }.toList()

    @Test
    fun `tokenizes a program from a String source`() {
        val types = typesOf(LexerFactory.create("let total: number = 10 + 20;"))

        assertEquals(
            listOf(
                TokenType.LET,
                TokenType.IDENTIFIER,
                TokenType.COLON,
                TokenType.NUMBERTYPE,
                TokenType.ASSIGN,
                TokenType.NUMBERLITERAL,
                TokenType.PLUS,
                TokenType.NUMBERLITERAL,
                TokenType.SEMICOLON,
                TokenType.EOF,
            ),
            types,
        )
    }

    @Test
    fun `every source overload produces the same tokens`() {
        val source = "println(x);"
        val expected = typesOf(LexerFactory.create(source))

        assertEquals(expected, typesOf(LexerFactory.create(StringReader(source))))
        assertEquals(expected, typesOf(LexerFactory.create(CharStream(StringReader(source)))))
        assertEquals(expected, typesOf(LexerFactory.create(ByteArrayInputStream(source.toByteArray()))))
    }

    @Test
    fun `reads an InputStream with the given charset`() {
        val source = "let ñ: string = \"añil\";"
        val input = ByteArrayInputStream(source.toByteArray(StandardCharsets.UTF_8))

        val values =
            LexerFactory.create(input, StandardCharsets.UTF_8)
                .tokenize()
                .asSequence()
                .map { it.value }
                .toList()

        assertTrue(values.contains("ñ"))
        assertTrue(values.contains("añil"))
    }

    // el lexer es agnostico de version a proposito: `const` sale como CONST aunque el
    // programa sea 1.0, y es el parser el que decide que esa feature no esta disponible
    @Test
    fun `keywords of both versions are tokenized regardless of version`() {
        val types = typesOf(LexerFactory.create("const if else readInput readEnv true"))

        assertTrue(types.containsAll(listOf(TokenType.CONST, TokenType.IF, TokenType.ELSE)))
        assertTrue(types.containsAll(listOf(TokenType.READINPUT, TokenType.READENV, TokenType.BOOLEANLITERAL)))
    }

    @Test
    fun `default readers cover identifiers numbers strings and symbols`() {
        val types = typesOf(LexerFactory.create("x 1 \"s\" +"))

        assertEquals(4, LexerFactory.defaultReaders().size)
        assertEquals(
            listOf(
                TokenType.IDENTIFIER,
                TokenType.NUMBERLITERAL,
                TokenType.STRINGLITERAL,
                TokenType.PLUS,
                TokenType.EOF,
            ),
            types,
        )
    }

    @Test
    fun `custom readers can replace the defaults`() {
        val customReader =
            object : TokenReader {
                override fun matches(char: Char): Boolean = char == '@'

                override fun read(
                    stream: CharStream,
                    start: Position,
                ): Token {
                    stream.advance()
                    return Token(TokenType.IDENTIFIER, start, stream.position(), "custom")
                }
            }

        val tokens = LexerFactory.create("@", listOf(customReader)).tokenize().asSequence().toList()

        assertEquals(2, tokens.size)
        assertEquals("custom", tokens[0].value)
        assertEquals(TokenType.EOF, tokens[1].type)
    }

    @Test
    fun `unknown characters still fail as lexical errors`() {
        assertFailsWith<LexicalError> {
            LexerFactory.create("let x = #;").tokenize().asSequence().toList()
        }
    }
}
