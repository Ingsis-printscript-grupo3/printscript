package printscript.parser.statement.handlers

import printscript.ast.Block
import printscript.ast.IfStatement
import printscript.ast.PrintCall
import printscript.ast.Statement
import printscript.ast.VariableDeclaration
import printscript.common.LanguageVersion
import printscript.common.Position
import printscript.common.Token
import printscript.common.TokenType
import printscript.parser.Parser
import printscript.parser.result.ParseResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class IfStatementHandlerTest {
    private fun token(
        type: TokenType,
        value: String = "",
        line: Int = 1,
        column: Int = 1,
    ) = Token(type, Position(line, column), Position(line, column + value.length), value)

    private fun parse(
        version: LanguageVersion = LanguageVersion.V1_1,
        vararg tokens: Token,
    ): List<ParseResult> {
        val tokenList = tokens.toList() + token(TokenType.EOF)
        return Parser(tokenList.iterator(), version).parse().asSequence().toList()
    }

    private fun parseStatements(vararg tokens: Token): List<Statement> =
        parse(LanguageVersion.V1_1, *tokens).map { result ->
            when (result) {
                is ParseResult.Success -> result.statement
                is ParseResult.Failure -> throw RuntimeException(result.message)
            }
        }

    @Test
    fun `if sin else, con bloque obligatorio`() {
        // if (x) { println(x); }
        val statements =
            parseStatements(
                token(TokenType.IF, "if", line = 4, column = 1),
                token(TokenType.LEFTPAREN, "("),
                token(TokenType.IDENTIFIER, "x"),
                token(TokenType.RIGHTPAREN, ")"),
                token(TokenType.LEFTBRACE, "{"),
                token(TokenType.PRINTLN, "println"),
                token(TokenType.LEFTPAREN, "("),
                token(TokenType.IDENTIFIER, "x"),
                token(TokenType.RIGHTPAREN, ")"),
                token(TokenType.SEMICOLON, ";"),
                token(TokenType.RIGHTBRACE, "}"),
            )

        val ifStatement = statements[0] as IfStatement
        assertEquals(Position(4, 1), ifStatement.position)
        assertEquals(1, ifStatement.thenBranch.statements.size)
        assertIs<PrintCall>(ifStatement.thenBranch.statements[0])
        assertNull(ifStatement.elseBranch)
    }

    @Test
    fun `if con else`() {
        // if (x) { println(x); } else { println(x); }
        val statements =
            parseStatements(
                token(TokenType.IF, "if"),
                token(TokenType.LEFTPAREN, "("),
                token(TokenType.IDENTIFIER, "x"),
                token(TokenType.RIGHTPAREN, ")"),
                token(TokenType.LEFTBRACE, "{"),
                token(TokenType.PRINTLN, "println"),
                token(TokenType.LEFTPAREN, "("),
                token(TokenType.IDENTIFIER, "x"),
                token(TokenType.RIGHTPAREN, ")"),
                token(TokenType.SEMICOLON, ";"),
                token(TokenType.RIGHTBRACE, "}"),
                token(TokenType.ELSE, "else"),
                token(TokenType.LEFTBRACE, "{"),
                token(TokenType.PRINTLN, "println"),
                token(TokenType.LEFTPAREN, "("),
                token(TokenType.IDENTIFIER, "x"),
                token(TokenType.RIGHTPAREN, ")"),
                token(TokenType.SEMICOLON, ";"),
                token(TokenType.RIGHTBRACE, "}"),
            )

        val ifStatement = statements[0] as IfStatement
        val elseBranch = ifStatement.elseBranch as Block
        assertEquals(1, elseBranch.statements.size)
    }

    @Test
    fun `un bloque puede contener varios statements`() {
        // if (x) { let y: number = 1; println(y); }
        val statements =
            parseStatements(
                token(TokenType.IF, "if"),
                token(TokenType.LEFTPAREN, "("),
                token(TokenType.IDENTIFIER, "x"),
                token(TokenType.RIGHTPAREN, ")"),
                token(TokenType.LEFTBRACE, "{"),
                token(TokenType.LET, "let"),
                token(TokenType.IDENTIFIER, "y"),
                token(TokenType.COLON, ":"),
                token(TokenType.NUMBERTYPE, "number"),
                token(TokenType.ASSIGN, "="),
                token(TokenType.NUMBERLITERAL, "1"),
                token(TokenType.SEMICOLON, ";"),
                token(TokenType.PRINTLN, "println"),
                token(TokenType.LEFTPAREN, "("),
                token(TokenType.IDENTIFIER, "y"),
                token(TokenType.RIGHTPAREN, ")"),
                token(TokenType.SEMICOLON, ";"),
                token(TokenType.RIGHTBRACE, "}"),
            )

        val ifStatement = statements[0] as IfStatement
        assertEquals(2, ifStatement.thenBranch.statements.size)
        assertIs<VariableDeclaration>(ifStatement.thenBranch.statements[0])
        assertIs<PrintCall>(ifStatement.thenBranch.statements[1])
    }

    @Test
    fun `if sin llaves alrededor del bloque falla`() {
        // if (x) println(x);
        val results =
            parse(
                LanguageVersion.V1_1,
                token(TokenType.IF, "if"),
                token(TokenType.LEFTPAREN, "("),
                token(TokenType.IDENTIFIER, "x"),
                token(TokenType.RIGHTPAREN, ")"),
                token(TokenType.PRINTLN, "println"),
                token(TokenType.LEFTPAREN, "("),
                token(TokenType.IDENTIFIER, "x"),
                token(TokenType.RIGHTPAREN, ")"),
                token(TokenType.SEMICOLON, ";"),
            )
        val failure = results[0] as ParseResult.Failure
        assert(failure.message.contains("Expected '{'"))
    }

    @Test
    fun `falta la llave de cierre del bloque`() {
        val results =
            parse(
                LanguageVersion.V1_1,
                token(TokenType.IF, "if"),
                token(TokenType.LEFTPAREN, "("),
                token(TokenType.IDENTIFIER, "x"),
                token(TokenType.RIGHTPAREN, ")"),
                token(TokenType.LEFTBRACE, "{"),
                token(TokenType.PRINTLN, "println"),
                token(TokenType.LEFTPAREN, "("),
                token(TokenType.IDENTIFIER, "x"),
                token(TokenType.RIGHTPAREN, ")"),
                token(TokenType.SEMICOLON, ";"),
                // falta '}'
            )
        val failure = results[0] as ParseResult.Failure
        assert(failure.message.contains("Expected '}'"))
    }

    @Test
    fun `else if es rechazado explicitamente`() {
        val results =
            parse(
                LanguageVersion.V1_1,
                token(TokenType.IF, "if"),
                token(TokenType.LEFTPAREN, "("),
                token(TokenType.IDENTIFIER, "x"),
                token(TokenType.RIGHTPAREN, ")"),
                token(TokenType.LEFTBRACE, "{"),
                token(TokenType.RIGHTBRACE, "}"),
                token(TokenType.ELSE, "else"),
                token(TokenType.IF, "if"),
                token(TokenType.LEFTPAREN, "("),
                token(TokenType.IDENTIFIER, "y"),
                token(TokenType.RIGHTPAREN, ")"),
                token(TokenType.LEFTBRACE, "{"),
                token(TokenType.RIGHTBRACE, "}"),
            )
        val failure = results[0] as ParseResult.Failure
        assert(failure.message.contains("'else if'"))
    }

    @Test
    fun `if bajo version 1_0 falla nombrando la feature y la version`() {
        val results =
            parse(
                LanguageVersion.V1_0,
                token(TokenType.IF, "if"),
                token(TokenType.LEFTPAREN, "("),
                token(TokenType.IDENTIFIER, "x"),
                token(TokenType.RIGHTPAREN, ")"),
                token(TokenType.LEFTBRACE, "{"),
                token(TokenType.RIGHTBRACE, "}"),
            )
        val failure = results[0] as ParseResult.Failure
        assert(failure.message.contains("if statements"))
        assert(failure.message.contains("1.0"))
    }
}
