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
import printscript.parser.SyntaxError
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
                is ParseResult.Failure -> throw SyntaxError(result.message, result.start, result.end)
            }
        }

    @Test
    fun `an if without else still requires its block`() {
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
    fun `an if with an else branch`() {
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
    fun `a block can hold several statements`() {
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
    fun `an if without braces around its block fails`() {
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
    fun `the closing brace of the block is missing`() {
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
    fun `else if is rejected explicitly`() {
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
    fun `an if under version 1_0 fails naming the feature and the version`() {
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
