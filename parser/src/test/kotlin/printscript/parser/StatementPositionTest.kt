package printscript.parser

import printscript.ast.Statement
import printscript.ast.VariableDeclaration
import printscript.common.Position
import printscript.common.Token
import printscript.common.TokenType
import printscript.parser.result.ParseResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class StatementPositionTest {
    private fun token(
        type: TokenType,
        value: String = "",
        line: Int = 1,
        column: Int = 1,
    ) = Token(type, Position(line, column), Position(line, column + value.length), value)

    private fun parse(vararg tokens: Token): List<Statement> {
        val tokenList = tokens.toList() + token(TokenType.EOF)
        return Parser(tokenList.iterator()).parse().asSequence().map { result ->
            when (result) {
                is ParseResult.Success -> result.statement
                is ParseResult.Failure -> throw SyntaxError(result.message, result.start, result.end)
            }
        }.toList()
    }

    @Test
    fun `a declaration takes the position of the let keyword`() {
        val statements =
            parse(
                token(TokenType.LET, "let", line = 3, column = 1),
                token(TokenType.IDENTIFIER, "x", line = 3, column = 5),
                token(TokenType.COLON, ":", line = 3, column = 6),
                token(TokenType.NUMBERTYPE, "number", line = 3, column = 8),
                token(TokenType.SEMICOLON, ";", line = 3, column = 14),
            )

        assertEquals(Position(3, 1), statements[0].position)
    }

    // position apunta al let y namePosition al nombre: son distintas a proposito,
    // y el linter usa la segunda para senalar el identificador
    @Test
    fun `a declaration carries the position of its name apart from the one of the let`() {
        val statements =
            parse(
                token(TokenType.LET, "let", line = 3, column = 1),
                token(TokenType.IDENTIFIER, "miVariable", line = 3, column = 5),
                token(TokenType.COLON, ":", line = 3, column = 15),
                token(TokenType.NUMBERTYPE, "number", line = 3, column = 17),
                token(TokenType.SEMICOLON, ";", line = 3, column = 23),
            )

        val declaration = statements[0]
        assertIs<VariableDeclaration>(declaration)
        assertEquals(Position(3, 1), declaration.position)
        assertEquals(Position(3, 5), declaration.namePosition)
    }

    @Test
    fun `a const declaration also carries the position of its name`() {
        val statements =
            parse(
                token(TokenType.CONST, "const", line = 1, column = 1),
                token(TokenType.IDENTIFIER, "miConstante", line = 1, column = 7),
                token(TokenType.COLON, ":", line = 1, column = 18),
                token(TokenType.NUMBERTYPE, "number", line = 1, column = 20),
                token(TokenType.ASSIGN, "=", line = 1, column = 27),
                token(TokenType.NUMBERLITERAL, "3", line = 1, column = 29),
                token(TokenType.SEMICOLON, ";", line = 1, column = 30),
            )

        val declaration = statements[0]
        assertIs<VariableDeclaration>(declaration)
        assertEquals(Position(1, 1), declaration.position)
        assertEquals(Position(1, 7), declaration.namePosition)
    }

    @Test
    fun `an assignment takes the position of the variable name`() {
        val statements =
            parse(
                token(TokenType.IDENTIFIER, "x", line = 7, column = 2),
                token(TokenType.ASSIGN, "=", line = 7, column = 4),
                token(TokenType.NUMBERLITERAL, "5", line = 7, column = 6),
                token(TokenType.SEMICOLON, ";", line = 7, column = 7),
            )

        assertEquals(Position(7, 2), statements[0].position)
    }

    @Test
    fun `a println takes the position of the println keyword`() {
        val statements =
            parse(
                token(TokenType.PRINTLN, "println", line = 9, column = 3),
                token(TokenType.LEFTPAREN, "(", line = 9, column = 10),
                token(TokenType.IDENTIFIER, "x", line = 9, column = 11),
                token(TokenType.RIGHTPAREN, ")", line = 9, column = 12),
                token(TokenType.SEMICOLON, ";", line = 9, column = 13),
            )

        assertEquals(Position(9, 3), statements[0].position)
    }

    @Test
    fun `every statement keeps its own position`() {
        val statements =
            parse(
                token(TokenType.LET, "let", line = 1, column = 1),
                token(TokenType.IDENTIFIER, "x", line = 1, column = 5),
                token(TokenType.COLON, ":", line = 1, column = 6),
                token(TokenType.NUMBERTYPE, "number", line = 1, column = 8),
                token(TokenType.SEMICOLON, ";", line = 1, column = 14),
                token(TokenType.PRINTLN, "println", line = 2, column = 1),
                token(TokenType.LEFTPAREN, "(", line = 2, column = 8),
                token(TokenType.IDENTIFIER, "x", line = 2, column = 9),
                token(TokenType.RIGHTPAREN, ")", line = 2, column = 10),
                token(TokenType.SEMICOLON, ";", line = 2, column = 11),
            )

        assertEquals(listOf(Position(1, 1), Position(2, 1)), statements.map { it.position })
    }
}
