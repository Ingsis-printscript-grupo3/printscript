package printscript.parser

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import printscript.ast.*
import printscript.common.Position
import printscript.common.Token
import printscript.common.TokenType
import kotlin.test.assertEquals

class ParserTest {

    private fun pos() = Position(1, 1) // Posición dummy para los tests

    private fun createToken(type: TokenType, value: String = "") = Token(type, pos(), pos(), value)

    private fun parse(vararg tokens: Token): List<Statement> {
        val tokenList = tokens.toList() + createToken(TokenType.EOF)
        val parser = Parser(tokenList)
        return parser.parse()
    }

    @Test
    fun `test happy path - variable declaration with number`() {
        // let x: number = 5;
        val statements = parse(
            createToken(TokenType.LET),
            createToken(TokenType.IDENTIFIER, "x"),
            createToken(TokenType.COLON),
            createToken(TokenType.NUMBERTYPE, "number"),
            createToken(TokenType.ASSIGN),
            createToken(TokenType.NUMBERLITERAL, "5"),
            createToken(TokenType.SEMICOLON)
        )

        assertEquals(1, statements.size)
        val stmt = statements[0] as VariableDeclaration
        assertEquals("x", stmt.name)
        assertEquals("number", stmt.type)
        val value = stmt.value as NumberLiteral
        assertEquals(5.0, value.value)
    }

    @Test
    fun `test happy path - variable declaration with string`() {
        // let msg: string = "hello";
        val statements = parse(
            createToken(TokenType.LET),
            createToken(TokenType.IDENTIFIER, "msg"),
            createToken(TokenType.COLON),
            createToken(TokenType.STRINGTYPE, "string"),
            createToken(TokenType.ASSIGN),
            createToken(TokenType.STRINGLITERAL, "hello"),
            createToken(TokenType.SEMICOLON)
        )

        assertEquals(1, statements.size)
        val stmt = statements[0] as VariableDeclaration
        assertEquals("msg", stmt.name)
        assertEquals("string", stmt.type)
        val value = stmt.value as StringLiteral
        assertEquals("hello", value.value)
    }

    @Test
    fun `test happy path - variable declaration without assignment`() {
        // let x: number;
        val statements = parse(
            createToken(TokenType.LET),
            createToken(TokenType.IDENTIFIER, "x"),
            createToken(TokenType.COLON),
            createToken(TokenType.NUMBERTYPE, "number"),
            createToken(TokenType.SEMICOLON)
        )

        assertEquals(1, statements.size)
        val stmt = statements[0] as VariableDeclaration
        assertEquals("x", stmt.name)
        assertEquals("number", stmt.type)
        assertEquals(null, stmt.value)
    }

    @Test
    fun `test happy path - variable assignment`() {
        // x = 10;
        val statements = parse(
            createToken(TokenType.IDENTIFIER, "x"),
            createToken(TokenType.ASSIGN),
            createToken(TokenType.NUMBERLITERAL, "10"),
            createToken(TokenType.SEMICOLON)
        )

        assertEquals(1, statements.size)
        val stmt = statements[0] as Assignment
        assertEquals("x", stmt.name)
        val value = stmt.value as NumberLiteral
        assertEquals(10.0, value.value)
    }

    @Test
    fun `test happy path - println with complex arithmetic expression`() {
        // println((5 + 2) * 3);
        val statements = parse(
            createToken(TokenType.PRINTLN),
            createToken(TokenType.LEFTPAREN),
            createToken(TokenType.LEFTPAREN),
            createToken(TokenType.NUMBERLITERAL, "5"),
            createToken(TokenType.PLUS),
            createToken(TokenType.NUMBERLITERAL, "2"),
            createToken(TokenType.RIGHTTPAREN),
            createToken(TokenType.MULTIPLY),
            createToken(TokenType.NUMBERLITERAL, "3"),
            createToken(TokenType.RIGHTTPAREN),
            createToken(TokenType.SEMICOLON)
        )

        assertEquals(1, statements.size)
        val stmt = statements[0] as PrintCall
        val binaryMult = stmt.value as BinaryExpression
        assertEquals(TokenType.MULTIPLY, binaryMult.operator)
        val binaryAdd = binaryMult.left as BinaryExpression
        assertEquals(TokenType.PLUS, binaryAdd.operator)
        assertEquals(5.0, (binaryAdd.left as NumberLiteral).value)
        assertEquals(2.0, (binaryAdd.right as NumberLiteral).value)
        assertEquals(3.0, (binaryMult.right as NumberLiteral).value)
    }

    @Test
    fun `test happy path - string concatenation`() {
        // println("Result: " + a);
        val statements = parse(
            createToken(TokenType.PRINTLN),
            createToken(TokenType.LEFTPAREN),
            createToken(TokenType.STRINGLITERAL, "Result: "),
            createToken(TokenType.PLUS),
            createToken(TokenType.IDENTIFIER, "a"),
            createToken(TokenType.RIGHTTPAREN),
            createToken(TokenType.SEMICOLON)
        )

        assertEquals(1, statements.size)
        val stmt = statements[0] as PrintCall
        val binaryPlus = stmt.value as BinaryExpression
        assertEquals(TokenType.PLUS, binaryPlus.operator)
        assertEquals("Result: ", (binaryPlus.left as StringLiteral).value)
        assertEquals("a", (binaryPlus.right as Identifier).name)
    }

    @Test
    fun `test unhappy path - missing semicolon`() {
        // println(5)
        val exception = assertThrows<RuntimeException> {
            parse(
                createToken(TokenType.PRINTLN),
                createToken(TokenType.LEFTPAREN),
                createToken(TokenType.NUMBERLITERAL, "5"),
                createToken(TokenType.RIGHTTPAREN)
                // Falta TokenType.SEMICOLON
            )
        }
        assert(exception.message!!.contains("Expected ';'"))
    }

    @Test
    fun `test unhappy path - missing closing parenthesis in expression`() {
        // println((5 + 2 * 3);
        val exception = assertThrows<RuntimeException> {
            parse(
                createToken(TokenType.PRINTLN),
                createToken(TokenType.LEFTPAREN),
                createToken(TokenType.LEFTPAREN),
                createToken(TokenType.NUMBERLITERAL, "5"),
                createToken(TokenType.PLUS),
                createToken(TokenType.NUMBERLITERAL, "2"),
                createToken(TokenType.MULTIPLY),
                createToken(TokenType.NUMBERLITERAL, "3"),
                // Falta TokenType.RIGHTTPAREN de la expresión
                createToken(TokenType.RIGHTTPAREN),
                createToken(TokenType.SEMICOLON)
            )
        }
        assert(exception.message!!.contains("Expected ')'"))
    }

    @Test
    fun `test unhappy path - unexpected token in declaration`() {
        // let 5 : number = 5;
        val exception = assertThrows<RuntimeException> {
            parse(
                createToken(TokenType.LET),
                createToken(TokenType.NUMBERLITERAL, "5"), // Inesperado
                createToken(TokenType.COLON),
                createToken(TokenType.NUMBERTYPE, "number"),
                createToken(TokenType.ASSIGN),
                createToken(TokenType.NUMBERLITERAL, "5"),
                createToken(TokenType.SEMICOLON)
            )
        }
        assert(exception.message!!.contains("Expected variable name"))
    }

    @Test
    fun `test unhappy path - missing type in declaration`() {
        // let x: = 5;
        val exception = assertThrows<RuntimeException> {
            parse(
                createToken(TokenType.LET),
                createToken(TokenType.IDENTIFIER, "x"),
                createToken(TokenType.COLON),
                createToken(TokenType.ASSIGN), // Inesperado, se esperaba number o string
                createToken(TokenType.NUMBERLITERAL, "5"),
                createToken(TokenType.SEMICOLON)
            )
        }
        assert(exception.message!!.contains("Expected 'number' or 'string'"))
    }
}
