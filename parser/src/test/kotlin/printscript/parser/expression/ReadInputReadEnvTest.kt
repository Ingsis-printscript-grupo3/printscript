package printscript.parser.expression

import printscript.ast.Identifier
import printscript.ast.PrintCall
import printscript.ast.ReadEnv
import printscript.ast.ReadInput
import printscript.ast.Statement
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

class ReadInputReadEnvTest {
    private fun token(
        type: TokenType,
        value: String = "",
        line: Int = 1,
        column: Int = 1,
    ) = Token(type, Position(line, column), Position(line, column + value.length), value)

    private fun parse(
        version: LanguageVersion,
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
    fun `a standalone readInput inside an assignment`() {
        // x = readInput("name:");
        val statements =
            parseStatements(
                token(TokenType.IDENTIFIER, "x", line = 1, column = 1),
                token(TokenType.ASSIGN, "=", line = 1, column = 3),
                token(TokenType.READINPUT, "readInput", line = 1, column = 5),
                token(TokenType.LEFTPAREN, "(", line = 1, column = 14),
                token(TokenType.STRINGLITERAL, "name:", line = 1, column = 15),
                token(TokenType.RIGHTPAREN, ")", line = 1, column = 20),
                token(TokenType.SEMICOLON, ";", line = 1, column = 21),
            )

        val assignment = statements[0] as printscript.ast.Assignment
        val readInput = assignment.value as ReadInput
        assertEquals(Position(1, 5), readInput.position)
    }

    @Test
    fun `readInput nested inside a println`() {
        // println(readInput(x));
        val statements =
            parseStatements(
                token(TokenType.PRINTLN, "println", line = 2, column = 1),
                token(TokenType.LEFTPAREN, "(", line = 2, column = 8),
                token(TokenType.READINPUT, "readInput", line = 2, column = 9),
                token(TokenType.LEFTPAREN, "(", line = 2, column = 18),
                token(TokenType.IDENTIFIER, "x", line = 2, column = 19),
                token(TokenType.RIGHTPAREN, ")", line = 2, column = 20),
                token(TokenType.RIGHTPAREN, ")", line = 2, column = 21),
                token(TokenType.SEMICOLON, ";", line = 2, column = 22),
            )

        val printCall = statements[0] as PrintCall
        val readInput = printCall.value as ReadInput
        assertIs<Identifier>(readInput.argument)
        assertEquals(Position(2, 9), readInput.position)
    }

    @Test
    fun `readEnv nested inside a println`() {
        // println(readEnv(x));
        val statements =
            parseStatements(
                token(TokenType.PRINTLN, "println", line = 1, column = 1),
                token(TokenType.LEFTPAREN, "(", line = 1, column = 8),
                token(TokenType.READENV, "readEnv", line = 1, column = 9),
                token(TokenType.LEFTPAREN, "(", line = 1, column = 16),
                token(TokenType.IDENTIFIER, "x", line = 1, column = 17),
                token(TokenType.RIGHTPAREN, ")", line = 1, column = 18),
                token(TokenType.RIGHTPAREN, ")", line = 1, column = 19),
                token(TokenType.SEMICOLON, ";", line = 1, column = 20),
            )

        val printCall = statements[0] as PrintCall
        val readEnv = printCall.value as ReadEnv
        assertEquals(Position(1, 9), readEnv.position)
    }

    @Test
    fun `the opening parenthesis of readInput is missing`() {
        val results =
            parse(
                LanguageVersion.V1_1,
                token(TokenType.PRINTLN, "println"),
                token(TokenType.LEFTPAREN, "("),
                token(TokenType.READINPUT, "readInput"),
                token(TokenType.IDENTIFIER, "x"),
                token(TokenType.RIGHTPAREN, ")"),
                token(TokenType.SEMICOLON, ";"),
            )
        val failure = results[0] as ParseResult.Failure
        assert(failure.message.contains("Expected '('"))
    }

    @Test
    fun `readInput under version 1_0 fails naming the feature and the version`() {
        val results =
            parse(
                LanguageVersion.V1_0,
                token(TokenType.PRINTLN, "println"),
                token(TokenType.LEFTPAREN, "("),
                token(TokenType.READINPUT, "readInput"),
                token(TokenType.LEFTPAREN, "("),
                token(TokenType.IDENTIFIER, "x"),
                token(TokenType.RIGHTPAREN, ")"),
                token(TokenType.RIGHTPAREN, ")"),
                token(TokenType.SEMICOLON, ";"),
            )
        val failure = results[0] as ParseResult.Failure
        assert(failure.message.contains("readInput"))
        assert(failure.message.contains("1.0"))
    }

    @Test
    fun `readEnv under version 1_0 fails naming the feature and the version`() {
        val results =
            parse(
                LanguageVersion.V1_0,
                token(TokenType.PRINTLN, "println"),
                token(TokenType.LEFTPAREN, "("),
                token(TokenType.READENV, "readEnv"),
                token(TokenType.LEFTPAREN, "("),
                token(TokenType.IDENTIFIER, "x"),
                token(TokenType.RIGHTPAREN, ")"),
                token(TokenType.RIGHTPAREN, ")"),
                token(TokenType.SEMICOLON, ";"),
            )
        val failure = results[0] as ParseResult.Failure
        assert(failure.message.contains("readEnv"))
        assert(failure.message.contains("1.0"))
    }

    @Test
    fun `a boolean literal under version 1_0 fails naming the feature and the version`() {
        val results =
            parse(
                LanguageVersion.V1_0,
                token(TokenType.PRINTLN, "println"),
                token(TokenType.LEFTPAREN, "("),
                token(TokenType.BOOLEANLITERAL, "true"),
                token(TokenType.RIGHTPAREN, ")"),
                token(TokenType.SEMICOLON, ";"),
            )
        val failure = results[0] as ParseResult.Failure
        assert(failure.message.contains("boolean"))
        assert(failure.message.contains("1.0"))
    }
}
