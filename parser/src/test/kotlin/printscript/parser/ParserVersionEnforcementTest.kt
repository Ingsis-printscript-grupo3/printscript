package printscript.parser

import printscript.ast.BooleanLiteral
import printscript.ast.IfStatement
import printscript.ast.ReadEnv
import printscript.ast.ReadInput
import printscript.ast.VariableDeclaration
import printscript.common.LanguageVersion
import printscript.common.Position
import printscript.common.Token
import printscript.common.TokenType
import printscript.parser.expression.ExpressionParser
import printscript.parser.result.ASTResult
import printscript.parser.statement.StatementParser
import printscript.parser.stream.TokenStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ParserVersionEnforcementTest {
    private fun pos() = Position(1, 1)

    private fun token(
        type: TokenType,
        value: String = "",
    ) = Token(type, pos(), pos(), value)

    @Test
    fun `StatementParser under 1_0 rejects if statement`() {
        val tokens =
            listOf(
                token(TokenType.IF, "if"),
                token(TokenType.LEFTPAREN, "("),
                token(TokenType.IDENTIFIER, "x"),
                token(TokenType.RIGHTPAREN, ")"),
                token(TokenType.LEFTBRACE, "{"),
                token(TokenType.RIGHTBRACE, "}"),
                token(TokenType.EOF),
            )
        val stream = TokenStream(tokens.iterator())
        val exprParser = ExpressionParser(stream, LanguageVersion.V1_0)
        val statementParser = StatementParser(stream, exprParser, LanguageVersion.V1_0)

        val result = statementParser.parseStatement()
        assertIs<ASTResult.Failure>(result)
        assertTrue(result.message.contains("if statements"))
        assertTrue(result.message.contains("1.0"))
    }

    @Test
    fun `StatementParser under 1_0 rejects const statement`() {
        val tokens =
            listOf(
                token(TokenType.CONST, "const"),
                token(TokenType.IDENTIFIER, "x"),
                token(TokenType.COLON, ":"),
                token(TokenType.NUMBERTYPE, "number"),
                token(TokenType.ASSIGN, "="),
                token(TokenType.NUMBERLITERAL, "1"),
                token(TokenType.SEMICOLON, ";"),
                token(TokenType.EOF),
            )
        val stream = TokenStream(tokens.iterator())
        val exprParser = ExpressionParser(stream, LanguageVersion.V1_0)
        val statementParser = StatementParser(stream, exprParser, LanguageVersion.V1_0)

        val result = statementParser.parseStatement()
        assertIs<ASTResult.Failure>(result)
        assertTrue(result.message.contains("const declarations"))
        assertTrue(result.message.contains("1.0"))
    }

    @Test
    fun `StatementParser under 1_1 parses if statement`() {
        val tokens =
            listOf(
                token(TokenType.IF, "if"),
                token(TokenType.LEFTPAREN, "("),
                token(TokenType.IDENTIFIER, "x"),
                token(TokenType.RIGHTPAREN, ")"),
                token(TokenType.LEFTBRACE, "{"),
                token(TokenType.RIGHTBRACE, "}"),
                token(TokenType.EOF),
            )
        val stream = TokenStream(tokens.iterator())
        val exprParser = ExpressionParser(stream, LanguageVersion.V1_1)
        val statementParser = StatementParser(stream, exprParser, LanguageVersion.V1_1)

        val result = statementParser.parseStatement()
        assertIs<ASTResult.Success<*>>(result)
        assertIs<IfStatement>(result.value)
    }

    @Test
    fun `StatementParser under 1_1 parses const statement`() {
        val tokens =
            listOf(
                token(TokenType.CONST, "const"),
                token(TokenType.IDENTIFIER, "x"),
                token(TokenType.COLON, ":"),
                token(TokenType.NUMBERTYPE, "number"),
                token(TokenType.ASSIGN, "="),
                token(TokenType.NUMBERLITERAL, "1"),
                token(TokenType.SEMICOLON, ";"),
                token(TokenType.EOF),
            )
        val stream = TokenStream(tokens.iterator())
        val exprParser = ExpressionParser(stream, LanguageVersion.V1_1)
        val statementParser = StatementParser(stream, exprParser, LanguageVersion.V1_1)

        val result = statementParser.parseStatement()
        assertIs<ASTResult.Success<*>>(result)
        val decl = assertIs<VariableDeclaration>(result.value)
        assertTrue(decl.isConst)
        assertEquals("x", decl.name)
    }

    @Test
    fun `ExpressionParser under 1_0 rejects boolean literal`() {
        val tokens = listOf(token(TokenType.BOOLEANLITERAL, "true"), token(TokenType.EOF))
        val stream = TokenStream(tokens.iterator())
        val exprParser = ExpressionParser(stream, LanguageVersion.V1_0)

        val result = exprParser.parseExpression()
        assertIs<ASTResult.Failure>(result)
        assertTrue(result.message.contains("boolean literals"))
        assertTrue(result.message.contains("1.0"))
    }

    @Test
    fun `ExpressionParser under 1_0 rejects readInput`() {
        val tokens =
            listOf(
                token(TokenType.READINPUT, "readInput"),
                token(TokenType.LEFTPAREN, "("),
                token(TokenType.STRINGLITERAL, "msg"),
                token(TokenType.RIGHTPAREN, ")"),
                token(TokenType.EOF),
            )
        val stream = TokenStream(tokens.iterator())
        val exprParser = ExpressionParser(stream, LanguageVersion.V1_0)

        val result = exprParser.parseExpression()
        assertIs<ASTResult.Failure>(result)
        assertTrue(result.message.contains("readInput"))
        assertTrue(result.message.contains("1.0"))
    }

    @Test
    fun `ExpressionParser under 1_0 rejects readEnv`() {
        val tokens =
            listOf(
                token(TokenType.READENV, "readEnv"),
                token(TokenType.LEFTPAREN, "("),
                token(TokenType.STRINGLITERAL, "PORT"),
                token(TokenType.RIGHTPAREN, ")"),
                token(TokenType.EOF),
            )
        val stream = TokenStream(tokens.iterator())
        val exprParser = ExpressionParser(stream, LanguageVersion.V1_0)

        val result = exprParser.parseExpression()
        assertIs<ASTResult.Failure>(result)
        assertTrue(result.message.contains("readEnv"))
        assertTrue(result.message.contains("1.0"))
    }

    @Test
    fun `ExpressionParser under 1_1 parses boolean literal`() {
        val tokens = listOf(token(TokenType.BOOLEANLITERAL, "true"), token(TokenType.EOF))
        val stream = TokenStream(tokens.iterator())
        val exprParser = ExpressionParser(stream, LanguageVersion.V1_1)

        val result = exprParser.parseExpression()
        assertIs<ASTResult.Success<*>>(result)
        assertIs<BooleanLiteral>(result.value)
    }

    @Test
    fun `ExpressionParser under 1_1 parses readInput and readEnv`() {
        val inputTokens =
            listOf(
                token(TokenType.READINPUT, "readInput"),
                token(TokenType.LEFTPAREN, "("),
                token(TokenType.STRINGLITERAL, "msg"),
                token(TokenType.RIGHTPAREN, ")"),
                token(TokenType.EOF),
            )
        val inputStream = TokenStream(inputTokens.iterator())
        val inputResult = ExpressionParser(inputStream, LanguageVersion.V1_1).parseExpression()
        assertIs<ASTResult.Success<*>>(inputResult)
        assertIs<ReadInput>(inputResult.value)

        val envTokens =
            listOf(
                token(TokenType.READENV, "readEnv"),
                token(TokenType.LEFTPAREN, "("),
                token(TokenType.STRINGLITERAL, "PORT"),
                token(TokenType.RIGHTPAREN, ")"),
                token(TokenType.EOF),
            )
        val envStream = TokenStream(envTokens.iterator())
        val envResult = ExpressionParser(envStream, LanguageVersion.V1_1).parseExpression()
        assertIs<ASTResult.Success<*>>(envResult)
        assertIs<ReadEnv>(envResult.value)
    }
}
