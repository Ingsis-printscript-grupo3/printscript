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
    private fun typesOf(lexer: Lexer): List<TokenType> = lexer.tokenize().asSequence().map { it.type }.toList()

    private fun typesOf(
        source: String,
        keywords: Map<String, TokenType> = KEYWORDS,
    ): List<TokenType> = typesOf(LexerFactory.create(source, keywords))

    @Test
    fun `tokenizes a program from a String source`() {
        val types = typesOf("let total: number = 10 + 20;")

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
        val expected = typesOf(source)

        assertEquals(expected, typesOf(LexerFactory.create(StringReader(source), KEYWORDS)))
        assertEquals(expected, typesOf(LexerFactory.create(CharStream(StringReader(source)), KEYWORDS)))
        assertEquals(
            expected,
            typesOf(LexerFactory.create(ByteArrayInputStream(source.toByteArray()), KEYWORDS)),
        )
    }

    @Test
    fun `reads an InputStream with the given charset`() {
        val source = "let ñ: string = \"añil\";"
        val input = ByteArrayInputStream(source.toByteArray(StandardCharsets.UTF_8))

        val values =
            LexerFactory.create(input, KEYWORDS, StandardCharsets.UTF_8)
                .tokenize()
                .asSequence()
                .map { it.value }
                .toList()

        assertTrue(values.contains("ñ"))
        assertTrue(values.contains("añil"))
    }

    @Test
    fun `tokenizes the keywords it is given`() {
        val types = typesOf("const if else readInput readEnv true")

        assertTrue(types.containsAll(listOf(TokenType.CONST, TokenType.IF, TokenType.ELSE)))
        assertTrue(types.containsAll(listOf(TokenType.READINPUT, TokenType.READENV, TokenType.BOOLEANLITERAL)))
    }

    // asi una version vieja deja `if` o `const` como nombres de variable: alcanza con no pasarlos
    @Test
    fun `a word missing from the keywords is a plain identifier`() {
        val types = typesOf("let const if", mapOf("let" to TokenType.LET))

        assertEquals(listOf(TokenType.LET, TokenType.IDENTIFIER, TokenType.IDENTIFIER, TokenType.EOF), types)
    }

    @Test
    fun `default readers cover identifiers numbers strings and symbols`() {
        val types = typesOf("x 1 \"s\" +")

        assertEquals(4, LexerFactory.defaultReaders(KEYWORDS).size)
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

        val tokens =
            LexerFactory.create(
                "@",
                KEYWORDS,
                listOf(customReader),
            ).tokenize().asSequence().toList()

        assertEquals(2, tokens.size)
        assertEquals("custom", tokens[0].value)
        assertEquals(TokenType.EOF, tokens[1].type)
    }

    @Test
    fun `unknown characters still fail as lexical errors`() {
        assertFailsWith<LexicalError> {
            LexerFactory.create("let x = #;", KEYWORDS).tokenize().asSequence().toList()
        }
    }
}
