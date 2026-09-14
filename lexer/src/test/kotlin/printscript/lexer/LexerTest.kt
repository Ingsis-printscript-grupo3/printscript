package printscript.lexer

import printscript.common.Position
import printscript.common.TokenType
import java.io.StringReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LexerTest {
    private fun tokenize(source: String): List<printscript.common.Token> =
        Lexer(CharStream(StringReader(source))).tokenize().asSequence().toList()

    private fun types(source: String): List<TokenType> = tokenize(source).map { it.type }

    @Test
    fun `recognises the let keyword`() {
        assertEquals(listOf(TokenType.LET, TokenType.EOF), types("let"))
    }

    @Test
    fun `recognises the println keyword`() {
        assertEquals(listOf(TokenType.PRINTLN, TokenType.EOF), types("println"))
    }

    @Test
    fun `recognises a plain identifier`() {
        val tokens = tokenize("nombre")
        assertEquals(TokenType.IDENTIFIER, tokens[0].type)
        assertEquals("nombre", tokens[0].value)
    }

    @Test
    fun `an identifier that starts like a keyword is not mistaken for one`() {
        val tokens = tokenize("letx")
        assertEquals(TokenType.IDENTIFIER, tokens[0].type)
        assertEquals("letx", tokens[0].value)
    }

    @Test
    fun `recognises number and string as types`() {
        assertEquals(listOf(TokenType.NUMBERTYPE, TokenType.EOF), types("number"))
        assertEquals(listOf(TokenType.STRINGTYPE, TokenType.EOF), types("string"))
    }

    @Test
    fun `recognises an integer number`() {
        val tokens = tokenize("12")
        assertEquals(TokenType.NUMBERLITERAL, tokens[0].type)
        assertEquals("12", tokens[0].value)
    }

    @Test
    fun `recognises a decimal number`() {
        val tokens = tokenize("3.14")
        assertEquals(TokenType.NUMBERLITERAL, tokens[0].type)
        assertEquals("3.14", tokens[0].value)
    }

    @Test
    fun `recognises a string in double quotes`() {
        val tokens = tokenize("\"hola\"")
        assertEquals(TokenType.STRINGLITERAL, tokens[0].type)
        assertEquals("hola", tokens[0].value)
    }

    @Test
    fun `recognises a string in single quotes`() {
        val tokens = tokenize("'hola'")
        assertEquals(TokenType.STRINGLITERAL, tokens[0].type)
        assertEquals("hola", tokens[0].value)
    }

    @Test
    fun `recognises every arithmetic operator`() {
        assertEquals(
            listOf(TokenType.PLUS, TokenType.MINUS, TokenType.MULTIPLY, TokenType.DIVIDE, TokenType.EOF),
            types("+ - * /"),
        )
    }

    @Test
    fun `recognises every symbol`() {
        assertEquals(
            listOf(
                TokenType.ASSIGN,
                TokenType.COLON,
                TokenType.SEMICOLON,
                TokenType.LEFTPAREN,
                TokenType.RIGHTPAREN,
                TokenType.EOF,
            ),
            types("=:;()"),
        )
    }

    @Test
    fun `skips spaces, tabs and line breaks without emitting spurious tokens`() {
        val tokens = tokenize("let  \t x\n=\n5;")
        assertEquals(
            listOf(
                TokenType.LET,
                TokenType.IDENTIFIER,
                TokenType.ASSIGN,
                TokenType.NUMBERLITERAL,
                TokenType.SEMICOLON,
                TokenType.EOF,
            ),
            tokens.map { it.type },
        )
    }

    @Test
    fun `the token position follows the line breaks`() {
        val tokens = tokenize("let\nx")
        val xToken = tokens[1]
        assertEquals(2, xToken.start.line)
        assertEquals(1, xToken.start.column)
    }

    @Test
    fun `first example of the assignment`() {
        val source =
            """
            let name: string = "Joe";
            let lastName: string = "Doe";
            println(name + " " + lastName);
            """.trimIndent()

        assertEquals(
            listOf(
                TokenType.LET, TokenType.IDENTIFIER, TokenType.COLON, TokenType.STRINGTYPE,
                TokenType.ASSIGN, TokenType.STRINGLITERAL, TokenType.SEMICOLON,
                TokenType.LET, TokenType.IDENTIFIER, TokenType.COLON, TokenType.STRINGTYPE,
                TokenType.ASSIGN, TokenType.STRINGLITERAL, TokenType.SEMICOLON,
                TokenType.PRINTLN, TokenType.LEFTPAREN, TokenType.IDENTIFIER, TokenType.PLUS,
                TokenType.STRINGLITERAL, TokenType.PLUS, TokenType.IDENTIFIER, TokenType.RIGHTPAREN,
                TokenType.SEMICOLON, TokenType.EOF,
            ),
            types(source),
        )
    }

    @Test
    fun `second example of the assignment`() {
        val source =
            """
            let a: number = 12;
            let b: number = 4;
            let c: number = a / b;
            println("Result: " + c);
            """.trimIndent()

        assertEquals(
            listOf(
                TokenType.LET, TokenType.IDENTIFIER, TokenType.COLON, TokenType.NUMBERTYPE,
                TokenType.ASSIGN, TokenType.NUMBERLITERAL, TokenType.SEMICOLON,
                TokenType.LET, TokenType.IDENTIFIER, TokenType.COLON, TokenType.NUMBERTYPE,
                TokenType.ASSIGN, TokenType.NUMBERLITERAL, TokenType.SEMICOLON,
                TokenType.LET, TokenType.IDENTIFIER, TokenType.COLON, TokenType.NUMBERTYPE,
                TokenType.ASSIGN, TokenType.IDENTIFIER, TokenType.DIVIDE, TokenType.IDENTIFIER, TokenType.SEMICOLON,
                TokenType.PRINTLN, TokenType.LEFTPAREN, TokenType.STRINGLITERAL, TokenType.PLUS,
                TokenType.IDENTIFIER, TokenType.RIGHTPAREN, TokenType.SEMICOLON, TokenType.EOF,
            ),
            types(source),
        )
    }

    @Test
    fun `third example of the assignment`() {
        val source =
            """
            let a: number = 12;
            let b: number = 4;
            a = a / b;
            println("Result: " + a);
            """.trimIndent()

        assertEquals(
            listOf(
                TokenType.LET, TokenType.IDENTIFIER, TokenType.COLON, TokenType.NUMBERTYPE,
                TokenType.ASSIGN, TokenType.NUMBERLITERAL, TokenType.SEMICOLON,
                TokenType.LET, TokenType.IDENTIFIER, TokenType.COLON, TokenType.NUMBERTYPE,
                TokenType.ASSIGN, TokenType.NUMBERLITERAL, TokenType.SEMICOLON,
                TokenType.IDENTIFIER, TokenType.ASSIGN, TokenType.IDENTIFIER, TokenType.DIVIDE,
                TokenType.IDENTIFIER, TokenType.SEMICOLON,
                TokenType.PRINTLN, TokenType.LEFTPAREN, TokenType.STRINGLITERAL, TokenType.PLUS,
                TokenType.IDENTIFIER, TokenType.RIGHTPAREN, TokenType.SEMICOLON, TokenType.EOF,
            ),
            types(source),
        )
    }

    @Test
    fun `an invalid character throws a LexicalError carrying its position`() {
        val error = assertFailsWith<LexicalError> { tokenize("let x = @;") }
        assertEquals(1, error.start.line)
        assertEquals(9, error.start.column)
    }

    @Test
    fun `an unterminated string throws a LexicalError`() {
        assertFailsWith<LexicalError> { tokenize("\"hola") }
    }

    @Test
    fun `recognises the const keyword`() {
        assertEquals(listOf(TokenType.CONST, TokenType.EOF), types("const"))
    }

    @Test
    fun `recognises the boolean keyword`() {
        assertEquals(listOf(TokenType.BOOLEANTYPE, TokenType.EOF), types("boolean"))
    }

    @Test
    fun `recognises the if keyword`() {
        assertEquals(listOf(TokenType.IF, TokenType.EOF), types("if"))
    }

    @Test
    fun `recognises the else keyword`() {
        assertEquals(listOf(TokenType.ELSE, TokenType.EOF), types("else"))
    }

    @Test
    fun `recognises the readInput keyword`() {
        assertEquals(listOf(TokenType.READINPUT, TokenType.EOF), types("readInput"))
    }

    @Test
    fun `recognises the readEnv keyword`() {
        assertEquals(listOf(TokenType.READENV, TokenType.EOF), types("readEnv"))
    }

    @Test
    fun `recognises true and false as BOOLEANLITERAL`() {
        assertEquals(listOf(TokenType.BOOLEANLITERAL, TokenType.EOF), types("true"))
        assertEquals(listOf(TokenType.BOOLEANLITERAL, TokenType.EOF), types("false"))
        val tokens = tokenize("true")
        assertEquals("true", tokens[0].value)
    }

    @Test
    fun `recognises the left and right braces with their position`() {
        val tokens = tokenize("{}")
        assertEquals(
            listOf(TokenType.LEFTBRACE, TokenType.RIGHTBRACE, TokenType.EOF),
            tokens.map { it.type },
        )
        assertEquals(Position(1, 1), tokens[0].start)
        assertEquals(Position(1, 2), tokens[1].start)
    }

    @Test
    fun `the identifier constante is not mistaken for the const keyword`() {
        val tokens = tokenize("constante")
        assertEquals(TokenType.IDENTIFIER, tokens[0].type)
        assertEquals("constante", tokens[0].value)
    }

    @Test
    fun `the identifier booleanx is not mistaken for the boolean keyword`() {
        val tokens = tokenize("booleanx")
        assertEquals(TokenType.IDENTIFIER, tokens[0].type)
        assertEquals("booleanx", tokens[0].value)
    }

    @Test
    fun `the identifier truex is not mistaken for true`() {
        val tokens = tokenize("truex")
        assertEquals(TokenType.IDENTIFIER, tokens[0].type)
        assertEquals("truex", tokens[0].value)
    }

    @Test
    fun `the identifier falsex is not mistaken for false`() {
        val tokens = tokenize("falsex")
        assertEquals(TokenType.IDENTIFIER, tokens[0].type)
        assertEquals("falsex", tokens[0].value)
    }

    @Test
    fun `the identifier ifx is not mistaken for the if keyword`() {
        val tokens = tokenize("ifx")
        assertEquals(TokenType.IDENTIFIER, tokens[0].type)
        assertEquals("ifx", tokens[0].value)
    }

    @Test
    fun `the identifier elsewhere is not mistaken for the else keyword`() {
        val tokens = tokenize("elsewhere")
        assertEquals(TokenType.IDENTIFIER, tokens[0].type)
        assertEquals("elsewhere", tokens[0].value)
    }

    @Test
    fun `the identifier readInputX is not mistaken for readInput`() {
        val tokens = tokenize("readInputX")
        assertEquals(TokenType.IDENTIFIER, tokens[0].type)
        assertEquals("readInputX", tokens[0].value)
    }

    @Test
    fun `the identifier readEnvX is not mistaken for readEnv`() {
        val tokens = tokenize("readEnvX")
        assertEquals(TokenType.IDENTIFIER, tokens[0].type)
        assertEquals("readEnvX", tokens[0].value)
    }

    @Test
    fun `a mixed 1_0 and 1_1 sample with const and boolean`() {
        val source = "const flag: boolean = true;"

        val tokens = tokenize(source)
        assertEquals(
            listOf(
                TokenType.CONST,
                TokenType.IDENTIFIER,
                TokenType.COLON,
                TokenType.BOOLEANTYPE,
                TokenType.ASSIGN,
                TokenType.BOOLEANLITERAL,
                TokenType.SEMICOLON,
                TokenType.EOF,
            ),
            tokens.map { it.type },
        )
        assertEquals(Position(1, 1), tokens[0].start)
        val booleanLiteralToken = tokens[5]
        assertEquals(TokenType.BOOLEANLITERAL, booleanLiteralToken.type)
        assertEquals(Position(1, 23), booleanLiteralToken.start)
    }

    @Test
    fun `a mixed sample with if, else, braces, readInput and readEnv`() {
        val source = "if (cond) { println(readInput()); } else { println(readEnv()); }"

        assertEquals(
            listOf(
                TokenType.IF, TokenType.LEFTPAREN, TokenType.IDENTIFIER, TokenType.RIGHTPAREN,
                TokenType.LEFTBRACE, TokenType.PRINTLN, TokenType.LEFTPAREN, TokenType.READINPUT,
                TokenType.LEFTPAREN, TokenType.RIGHTPAREN, TokenType.RIGHTPAREN, TokenType.SEMICOLON,
                TokenType.RIGHTBRACE, TokenType.ELSE, TokenType.LEFTBRACE, TokenType.PRINTLN,
                TokenType.LEFTPAREN, TokenType.READENV, TokenType.LEFTPAREN, TokenType.RIGHTPAREN,
                TokenType.RIGHTPAREN, TokenType.SEMICOLON, TokenType.RIGHTBRACE, TokenType.EOF,
            ),
            types(source),
        )
    }
}
