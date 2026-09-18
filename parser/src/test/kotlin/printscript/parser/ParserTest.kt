package printscript.parser

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import printscript.ast.Assignment
import printscript.ast.BinaryExpression
import printscript.ast.Identifier
import printscript.ast.NumberLiteral
import printscript.ast.PrintCall
import printscript.ast.Statement
import printscript.ast.StringLiteral
import printscript.ast.VariableDeclaration
import printscript.common.LanguageVersion
import printscript.common.Position
import printscript.common.Token
import printscript.common.TokenType
import printscript.parser.result.ParseResult
import kotlin.test.assertEquals

class ParserTest {
    private fun pos() = Position(1, 1) // posicion dummy para los tests

    private fun createToken(
        type: TokenType,
        value: String = "",
    ) = Token(type, pos(), pos(), value)

    private fun parse(
        vararg tokens: Token,
        version: LanguageVersion = LanguageVersion.DEFAULT,
    ): List<Statement> {
        val tokenList = tokens.toList() + createToken(TokenType.EOF)
        val parser = Parser(tokenList.iterator(), version)
        return parser.parse().asSequence().map { result ->
            when (result) {
                is ParseResult.Success -> result.statement
                is ParseResult.Failure -> throw SyntaxError(result.message, result.start, result.end)
            }
        }.toList()
    }

    @Test
    fun `parses a variable declaration with a number`() {
        // let x: number = 5;
        val statements =
            parse(
                createToken(TokenType.LET),
                createToken(TokenType.IDENTIFIER, "x"),
                createToken(TokenType.COLON),
                createToken(TokenType.NUMBERTYPE, "number"),
                createToken(TokenType.ASSIGN),
                createToken(TokenType.NUMBERLITERAL, "5"),
                createToken(TokenType.SEMICOLON),
            )

        assertEquals(1, statements.size)
        val stmt = statements[0] as VariableDeclaration
        assertEquals("x", stmt.name)
        assertEquals("number", stmt.type)
        val value = stmt.value as NumberLiteral
        assertEquals(5.0, value.value)
    }

    @Test
    fun `parses a variable declaration with a string`() {
        // let msg: string = "hello";
        val statements =
            parse(
                createToken(TokenType.LET),
                createToken(TokenType.IDENTIFIER, "msg"),
                createToken(TokenType.COLON),
                createToken(TokenType.STRINGTYPE, "string"),
                createToken(TokenType.ASSIGN),
                createToken(TokenType.STRINGLITERAL, "hello"),
                createToken(TokenType.SEMICOLON),
            )

        assertEquals(1, statements.size)
        val stmt = statements[0] as VariableDeclaration
        assertEquals("msg", stmt.name)
        assertEquals("string", stmt.type)
        val value = stmt.value as StringLiteral
        assertEquals("hello", value.value)
    }

    @Test
    fun `parses a variable declaration without an initializer`() {
        // let x: number;
        val statements =
            parse(
                createToken(TokenType.LET),
                createToken(TokenType.IDENTIFIER, "x"),
                createToken(TokenType.COLON),
                createToken(TokenType.NUMBERTYPE, "number"),
                createToken(TokenType.SEMICOLON),
            )

        assertEquals(1, statements.size)
        val stmt = statements[0] as VariableDeclaration
        assertEquals("x", stmt.name)
        assertEquals("number", stmt.type)
        assertEquals(null, stmt.value)
    }

    @Test
    fun `parses an assignment to an existing variable`() {
        // x = 10;
        val statements =
            parse(
                createToken(TokenType.IDENTIFIER, "x"),
                createToken(TokenType.ASSIGN),
                createToken(TokenType.NUMBERLITERAL, "10"),
                createToken(TokenType.SEMICOLON),
            )

        assertEquals(1, statements.size)
        val stmt = statements[0] as Assignment
        assertEquals("x", stmt.name)
        val value = stmt.value as NumberLiteral
        assertEquals(10.0, value.value)
    }

    @Test
    fun `parses a println with a complex arithmetic expression`() {
        // println((5 + 2) * 3);
        val statements =
            parse(
                createToken(TokenType.PRINTLN),
                createToken(TokenType.LEFTPAREN),
                createToken(TokenType.LEFTPAREN),
                createToken(TokenType.NUMBERLITERAL, "5"),
                createToken(TokenType.PLUS),
                createToken(TokenType.NUMBERLITERAL, "2"),
                createToken(TokenType.RIGHTPAREN),
                createToken(TokenType.MULTIPLY),
                createToken(TokenType.NUMBERLITERAL, "3"),
                createToken(TokenType.RIGHTPAREN),
                createToken(TokenType.SEMICOLON),
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
    fun `parses a string concatenation`() {
        // println("Result: " + a);
        val statements =
            parse(
                createToken(TokenType.PRINTLN),
                createToken(TokenType.LEFTPAREN),
                createToken(TokenType.STRINGLITERAL, "Result: "),
                createToken(TokenType.PLUS),
                createToken(TokenType.IDENTIFIER, "a"),
                createToken(TokenType.RIGHTPAREN),
                createToken(TokenType.SEMICOLON),
            )

        assertEquals(1, statements.size)
        val stmt = statements[0] as PrintCall
        val binaryPlus = stmt.value as BinaryExpression
        assertEquals(TokenType.PLUS, binaryPlus.operator)
        assertEquals("Result: ", (binaryPlus.left as StringLiteral).value)
        assertEquals("a", (binaryPlus.right as Identifier).name)
    }

    @Test
    fun `fails when the semicolon is missing`() {
        // println(5)
        val exception =
            assertThrows<SyntaxError> {
                parse(
                    createToken(TokenType.PRINTLN),
                    createToken(TokenType.LEFTPAREN),
                    createToken(TokenType.NUMBERLITERAL, "5"),
                    createToken(TokenType.RIGHTPAREN),
                    // Falta TokenType.SEMICOLON
                )
            }
        assert(exception.message.contains("Expected ';'"))
    }

    @Test
    fun `fails when a closing parenthesis is missing in the expression`() {
        // println((5 + 2 * 3);
        val exception =
            assertThrows<SyntaxError> {
                parse(
                    createToken(TokenType.PRINTLN),
                    createToken(TokenType.LEFTPAREN),
                    createToken(TokenType.LEFTPAREN),
                    createToken(TokenType.NUMBERLITERAL, "5"),
                    createToken(TokenType.PLUS),
                    createToken(TokenType.NUMBERLITERAL, "2"),
                    createToken(TokenType.MULTIPLY),
                    createToken(TokenType.NUMBERLITERAL, "3"),
                    // Falta TokenType.RIGHTPAREN de la expresión
                    createToken(TokenType.RIGHTPAREN),
                    createToken(TokenType.SEMICOLON),
                )
            }
        assert(exception.message.contains("Expected ')'"))
    }

    @Test
    fun `fails when the declaration has a number where the name goes`() {
        // let 5 : number = 5;
        val exception =
            assertThrows<SyntaxError> {
                parse(
                    createToken(TokenType.LET),
                    // Inesperado
                    createToken(TokenType.NUMBERLITERAL, "5"),
                    createToken(TokenType.COLON),
                    createToken(TokenType.NUMBERTYPE, "number"),
                    createToken(TokenType.ASSIGN),
                    createToken(TokenType.NUMBERLITERAL, "5"),
                    createToken(TokenType.SEMICOLON),
                )
            }
        assert(exception.message.contains("Expected variable name"))
    }

    @Test
    fun `fails when the declaration has no type`() {
        // let x: = 5;
        val exception =
            assertThrows<SyntaxError> {
                parse(
                    createToken(TokenType.LET),
                    createToken(TokenType.IDENTIFIER, "x"),
                    createToken(TokenType.COLON),
                    // Inesperado, se esperaba number o string
                    createToken(TokenType.ASSIGN),
                    createToken(TokenType.NUMBERLITERAL, "5"),
                    createToken(TokenType.SEMICOLON),
                )
            }
        assert(exception.message.contains("Expected 'number', 'string' or 'boolean'"))
    }

    @Test
    fun `fails when the file starts with a token that begins no statement`() {
        // )
        val exception = assertThrows<SyntaxError> { parse(createToken(TokenType.RIGHTPAREN, ")")) }

        assert(exception.message.contains("Unexpected token ')'"))
    }

    @Test
    fun `fails when the input ends in the middle of an expression`() {
        // let x: number = 5 +
        val exception =
            assertThrows<SyntaxError> {
                parse(
                    createToken(TokenType.LET),
                    createToken(TokenType.IDENTIFIER, "x"),
                    createToken(TokenType.COLON),
                    createToken(TokenType.NUMBERTYPE, "number"),
                    createToken(TokenType.ASSIGN),
                    createToken(TokenType.NUMBERLITERAL, "5"),
                    createToken(TokenType.PLUS),
                    // el archivo se corta con el operador colgado
                )
            }

        assert(exception.message.contains("Expected a value or expression"))
    }

    @Test
    fun `fails when readInput is not closed`() {
        // let name: string = readInput("a";
        val exception =
            assertThrows<SyntaxError> {
                parse(
                    createToken(TokenType.LET),
                    createToken(TokenType.IDENTIFIER, "name"),
                    createToken(TokenType.COLON),
                    createToken(TokenType.STRINGTYPE, "string"),
                    createToken(TokenType.ASSIGN),
                    createToken(TokenType.READINPUT),
                    createToken(TokenType.LEFTPAREN),
                    createToken(TokenType.STRINGLITERAL, "a"),
                    // falta el RIGHTPAREN que cierra readInput
                    createToken(TokenType.SEMICOLON),
                    version = LanguageVersion.V1_1,
                )
            }

        assert(exception.message.contains("Expected ')' closing 'readInput'"))
    }
}
