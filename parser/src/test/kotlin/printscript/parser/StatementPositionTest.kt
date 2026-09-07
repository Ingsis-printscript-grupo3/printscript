package printscript.parser

import printscript.ast.Statement
import printscript.common.Position
import printscript.common.Token
import printscript.common.TokenType
import printscript.parser.result.ParseResult
import kotlin.test.Test
import kotlin.test.assertEquals

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
    fun `a declaration takes the position of the variable name`() {
        val statements =
            parse(
                token(TokenType.LET, "let", line = 3, column = 1),
                token(TokenType.IDENTIFIER, "x", line = 3, column = 5),
                token(TokenType.COLON, ":", line = 3, column = 6),
                token(TokenType.NUMBERTYPE, "number", line = 3, column = 8),
                token(TokenType.SEMICOLON, ";", line = 3, column = 14),
            )

        assertEquals(Position(3, 5), statements[0].position)
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

        assertEquals(listOf(Position(1, 5), Position(2, 1)), statements.map { it.position })
    }
}
