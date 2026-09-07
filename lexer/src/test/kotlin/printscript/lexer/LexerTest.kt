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
    fun `reconoce la keyword let`() {
        assertEquals(listOf(TokenType.LET, TokenType.EOF), types("let"))
    }

    @Test
    fun `reconoce la keyword println`() {
        assertEquals(listOf(TokenType.PRINTLN, TokenType.EOF), types("println"))
    }

    @Test
    fun `reconoce un identificador comun`() {
        val tokens = tokenize("nombre")
        assertEquals(TokenType.IDENTIFIER, tokens[0].type)
        assertEquals("nombre", tokens[0].value)
    }

    @Test
    fun `identificador que empieza igual que una keyword no se confunde`() {
        val tokens = tokenize("letx")
        assertEquals(TokenType.IDENTIFIER, tokens[0].type)
        assertEquals("letx", tokens[0].value)
    }

    @Test
    fun `reconoce number y string como tipos`() {
        assertEquals(listOf(TokenType.NUMBERTYPE, TokenType.EOF), types("number"))
        assertEquals(listOf(TokenType.STRINGTYPE, TokenType.EOF), types("string"))
    }

    @Test
    fun `reconoce numero entero`() {
        val tokens = tokenize("12")
        assertEquals(TokenType.NUMBERLITERAL, tokens[0].type)
        assertEquals("12", tokens[0].value)
    }

    @Test
    fun `reconoce numero decimal`() {
        val tokens = tokenize("3.14")
        assertEquals(TokenType.NUMBERLITERAL, tokens[0].type)
        assertEquals("3.14", tokens[0].value)
    }

    @Test
    fun `reconoce string con comillas dobles`() {
        val tokens = tokenize("\"hola\"")
        assertEquals(TokenType.STRINGLITERAL, tokens[0].type)
        assertEquals("hola", tokens[0].value)
    }

    @Test
    fun `reconoce string con comillas simples`() {
        val tokens = tokenize("'hola'")
        assertEquals(TokenType.STRINGLITERAL, tokens[0].type)
        assertEquals("hola", tokens[0].value)
    }

    @Test
    fun `reconoce cada operador aritmetico`() {
        assertEquals(
            listOf(TokenType.PLUS, TokenType.MINUS, TokenType.MULTIPLY, TokenType.DIVIDE, TokenType.EOF),
            types("+ - * /"),
        )
    }

    @Test
    fun `reconoce cada simbolo`() {
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
    fun `saltea espacios tabs y saltos de linea sin generar tokens espurios`() {
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
    fun `la posicion del token respeta los saltos de linea`() {
        val tokens = tokenize("let\nx")
        val xToken = tokens[1]
        assertEquals(2, xToken.start.line)
        assertEquals(1, xToken.start.column)
    }

    @Test
    fun `ejemplo 1 de la consigna`() {
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
    fun `ejemplo 2 de la consigna`() {
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
    fun `ejemplo 3 de la consigna`() {
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
    fun `caracter invalido lanza LexicalError con posicion`() {
        val error = assertFailsWith<LexicalError> { tokenize("let x = @;") }
        assertEquals(1, error.start.line)
        assertEquals(9, error.start.column)
    }

    @Test
    fun `string sin cerrar lanza LexicalError`() {
        assertFailsWith<LexicalError> { tokenize("\"hola") }
    }

    @Test
    fun `reconoce la keyword const`() {
        assertEquals(listOf(TokenType.CONST, TokenType.EOF), types("const"))
    }

    @Test
    fun `reconoce la keyword boolean`() {
        assertEquals(listOf(TokenType.BOOLEANTYPE, TokenType.EOF), types("boolean"))
    }

    @Test
    fun `reconoce la keyword if`() {
        assertEquals(listOf(TokenType.IF, TokenType.EOF), types("if"))
    }

    @Test
    fun `reconoce la keyword else`() {
        assertEquals(listOf(TokenType.ELSE, TokenType.EOF), types("else"))
    }

    @Test
    fun `reconoce la keyword readInput`() {
        assertEquals(listOf(TokenType.READINPUT, TokenType.EOF), types("readInput"))
    }

    @Test
    fun `reconoce la keyword readEnv`() {
        assertEquals(listOf(TokenType.READENV, TokenType.EOF), types("readEnv"))
    }

    @Test
    fun `reconoce true y false como BOOLEANLITERAL`() {
        assertEquals(listOf(TokenType.BOOLEANLITERAL, TokenType.EOF), types("true"))
        assertEquals(listOf(TokenType.BOOLEANLITERAL, TokenType.EOF), types("false"))
        val tokens = tokenize("true")
        assertEquals("true", tokens[0].value)
    }

    @Test
    fun `reconoce llave izquierda y derecha con posicion`() {
        val tokens = tokenize("{}")
        assertEquals(
            listOf(TokenType.LEFTBRACE, TokenType.RIGHTBRACE, TokenType.EOF),
            tokens.map { it.type },
        )
        assertEquals(Position(1, 1), tokens[0].start)
        assertEquals(Position(1, 2), tokens[1].start)
    }

    @Test
    fun `identificador constante no se confunde con la keyword const`() {
        val tokens = tokenize("constante")
        assertEquals(TokenType.IDENTIFIER, tokens[0].type)
        assertEquals("constante", tokens[0].value)
    }

    @Test
    fun `identificador booleanx no se confunde con la keyword boolean`() {
        val tokens = tokenize("booleanx")
        assertEquals(TokenType.IDENTIFIER, tokens[0].type)
        assertEquals("booleanx", tokens[0].value)
    }

    @Test
    fun `identificador truex no se confunde con la keyword true`() {
        val tokens = tokenize("truex")
        assertEquals(TokenType.IDENTIFIER, tokens[0].type)
        assertEquals("truex", tokens[0].value)
    }

    @Test
    fun `identificador falsex no se confunde con la keyword false`() {
        val tokens = tokenize("falsex")
        assertEquals(TokenType.IDENTIFIER, tokens[0].type)
        assertEquals("falsex", tokens[0].value)
    }

    @Test
    fun `identificador ifx no se confunde con la keyword if`() {
        val tokens = tokenize("ifx")
        assertEquals(TokenType.IDENTIFIER, tokens[0].type)
        assertEquals("ifx", tokens[0].value)
    }

    @Test
    fun `identificador elsewhere no se confunde con la keyword else`() {
        val tokens = tokenize("elsewhere")
        assertEquals(TokenType.IDENTIFIER, tokens[0].type)
        assertEquals("elsewhere", tokens[0].value)
    }

    @Test
    fun `identificador readInputX no se confunde con la keyword readInput`() {
        val tokens = tokenize("readInputX")
        assertEquals(TokenType.IDENTIFIER, tokens[0].type)
        assertEquals("readInputX", tokens[0].value)
    }

    @Test
    fun `identificador readEnvX no se confunde con la keyword readEnv`() {
        val tokens = tokenize("readEnvX")
        assertEquals(TokenType.IDENTIFIER, tokens[0].type)
        assertEquals("readEnvX", tokens[0].value)
    }

    @Test
    fun `ejemplo mixto de tokens 1_0 y 1_1 con const y boolean`() {
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
    fun `ejemplo mixto de if else llaves readInput y readEnv`() {
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
