package printscript.parser

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import printscript.ast.Assignment
import printscript.ast.BinaryExpression
import printscript.ast.Identifier
import printscript.ast.NumberLiteral
import printscript.ast.Statement
import printscript.ast.VariableDeclaration
import printscript.common.LanguageVersion
import printscript.common.Position
import printscript.common.Token
import printscript.common.TokenType
import printscript.parser.result.ParseResult
import kotlin.test.assertEquals

class UnaryMinusStatementTest {
    private fun pos() = Position(1, 1)

    private fun createToken(
        type: TokenType,
        value: String = "",
    ) = Token(type, pos(), pos(), value)

    private fun parse(
        vararg tokens: Token,
        version: LanguageVersion = LanguageVersion.V1_1,
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
    fun `parses a variable declaration with a negative number in version 1_0`() {
        // let x: number = -5; in V1_0
        val statements =
            parse(
                createToken(TokenType.LET),
                createToken(TokenType.IDENTIFIER, "x"),
                createToken(TokenType.COLON),
                createToken(TokenType.NUMBERTYPE, "number"),
                createToken(TokenType.ASSIGN),
                createToken(TokenType.MINUS, "-"),
                createToken(TokenType.NUMBERLITERAL, "5"),
                createToken(TokenType.SEMICOLON),
                version = LanguageVersion.V1_0,
            )

        assertEquals(1, statements.size)
        val stmt = statements[0] as VariableDeclaration
        assertEquals("x", stmt.name)
        val value = stmt.value as NumberLiteral
        assertEquals(-5.0, value.value)
    }

    @Test
    fun `parses a const declaration with a negative number`() {
        // const x: number = -42;
        val statements =
            parse(
                createToken(TokenType.CONST),
                createToken(TokenType.IDENTIFIER, "x"),
                createToken(TokenType.COLON),
                createToken(TokenType.NUMBERTYPE, "number"),
                createToken(TokenType.ASSIGN),
                createToken(TokenType.MINUS, "-"),
                createToken(TokenType.NUMBERLITERAL, "42"),
                createToken(TokenType.SEMICOLON),
            )

        assertEquals(1, statements.size)
        val stmt = statements[0] as VariableDeclaration
        assertEquals("x", stmt.name)
        val value = stmt.value as NumberLiteral
        assertEquals(-42.0, value.value)
        assert(stmt.isConst)
    }

    @Test
    fun `parses an assignment statement with a negative number`() {
        // x = -15;
        val statements =
            parse(
                createToken(TokenType.IDENTIFIER, "x"),
                createToken(TokenType.ASSIGN),
                createToken(TokenType.MINUS, "-"),
                createToken(TokenType.NUMBERLITERAL, "15"),
                createToken(TokenType.SEMICOLON),
            )

        assertEquals(1, statements.size)
        val stmt = statements[0] as Assignment
        assertEquals("x", stmt.name)
        val value = stmt.value as NumberLiteral
        assertEquals(-15.0, value.value)
    }

    @Test
    fun `parses an assignment statement with a negative identifier`() {
        // x = -y;
        val statements =
            parse(
                createToken(TokenType.IDENTIFIER, "x"),
                createToken(TokenType.ASSIGN),
                createToken(TokenType.MINUS, "-"),
                createToken(TokenType.IDENTIFIER, "y"),
                createToken(TokenType.SEMICOLON),
            )

        assertEquals(1, statements.size)
        val stmt = statements[0] as Assignment
        val binary = stmt.value as BinaryExpression
        assertEquals(TokenType.MINUS, binary.operator)
        assertEquals(0.0, (binary.left as NumberLiteral).value)
        assertEquals("y", (binary.right as Identifier).name)
    }

    @Test
    fun `parses a variable declaration with a parenthesized negative expression`() {
        // let x: number = -(a + b);
        val statements =
            parse(
                createToken(TokenType.LET),
                createToken(TokenType.IDENTIFIER, "x"),
                createToken(TokenType.COLON),
                createToken(TokenType.NUMBERTYPE, "number"),
                createToken(TokenType.ASSIGN),
                createToken(TokenType.MINUS, "-"),
                createToken(TokenType.LEFTPAREN, "("),
                createToken(TokenType.IDENTIFIER, "a"),
                createToken(TokenType.PLUS, "+"),
                createToken(TokenType.IDENTIFIER, "b"),
                createToken(TokenType.RIGHTPAREN, ")"),
                createToken(TokenType.SEMICOLON),
            )

        assertEquals(1, statements.size)
        val stmt = statements[0] as VariableDeclaration
        val binary = stmt.value as BinaryExpression
        assertEquals(TokenType.MINUS, binary.operator)
        assertEquals(0.0, (binary.left as NumberLiteral).value)
        val right = binary.right as BinaryExpression
        assertEquals(TokenType.PLUS, right.operator)
        assertEquals("a", (right.left as Identifier).name)
        assertEquals("b", (right.right as Identifier).name)
    }

    @Test
    fun `fails when assignment ends with trailing unary minus`() {
        // x = -;
        val exception =
            assertThrows<SyntaxError> {
                parse(
                    createToken(TokenType.IDENTIFIER, "x"),
                    createToken(TokenType.ASSIGN),
                    createToken(TokenType.MINUS, "-"),
                    createToken(TokenType.SEMICOLON),
                )
            }

        assert(exception.message.contains("Expected a value or expression"))
    }

    @Test
    fun `fails when const declaration ends with trailing unary minus`() {
        // const x: number = -;
        val exception =
            assertThrows<SyntaxError> {
                parse(
                    createToken(TokenType.CONST),
                    createToken(TokenType.IDENTIFIER, "x"),
                    createToken(TokenType.COLON),
                    createToken(TokenType.NUMBERTYPE, "number"),
                    createToken(TokenType.ASSIGN),
                    createToken(TokenType.MINUS, "-"),
                    createToken(TokenType.SEMICOLON),
                )
            }

        assert(exception.message.contains("Expected a value or expression"))
    }

    @Test
    fun `fails when statement contains unary minus followed by multiplication`() {
        // let x: number = - * 5;
        val exception =
            assertThrows<SyntaxError> {
                parse(
                    createToken(TokenType.LET),
                    createToken(TokenType.IDENTIFIER, "x"),
                    createToken(TokenType.COLON),
                    createToken(TokenType.NUMBERTYPE, "number"),
                    createToken(TokenType.ASSIGN),
                    createToken(TokenType.MINUS, "-"),
                    createToken(TokenType.MULTIPLY, "*"),
                    createToken(TokenType.NUMBERLITERAL, "5"),
                    createToken(TokenType.SEMICOLON),
                )
            }

        assert(exception.message.contains("Expected a value or expression"))
    }
}
