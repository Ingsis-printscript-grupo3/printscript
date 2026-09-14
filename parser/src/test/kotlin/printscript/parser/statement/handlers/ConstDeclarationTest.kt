package printscript.parser.statement.handlers

import printscript.ast.BooleanLiteral
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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConstDeclarationTest {
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
    fun `a const with an initializer sets isConst to true`() {
        // const x: number = 5;
        val statements =
            parseStatements(
                token(TokenType.CONST, "const", line = 2, column = 1),
                token(TokenType.IDENTIFIER, "x"),
                token(TokenType.COLON, ":"),
                token(TokenType.NUMBERTYPE, "number"),
                token(TokenType.ASSIGN, "="),
                token(TokenType.NUMBERLITERAL, "5"),
                token(TokenType.SEMICOLON, ";"),
            )

        val declaration = statements[0] as VariableDeclaration
        assertTrue(declaration.isConst)
        assertEquals(Position(2, 1), declaration.position)
    }

    @Test
    fun `a const without an initializer fails`() {
        // const x: number;
        val results =
            parse(
                LanguageVersion.V1_1,
                token(TokenType.CONST, "const"),
                token(TokenType.IDENTIFIER, "x"),
                token(TokenType.COLON, ":"),
                token(TokenType.NUMBERTYPE, "number"),
                token(TokenType.SEMICOLON, ";"),
            )
        val failure = results[0] as ParseResult.Failure
        assert(failure.message.contains("'const' requires an initializer"))
    }

    @Test
    fun `let keeps its optional initializer`() {
        // let x: number;
        val statements =
            parseStatements(
                token(TokenType.LET, "let"),
                token(TokenType.IDENTIFIER, "x"),
                token(TokenType.COLON, ":"),
                token(TokenType.NUMBERTYPE, "number"),
                token(TokenType.SEMICOLON, ";"),
            )
        val declaration = statements[0] as VariableDeclaration
        assertFalse(declaration.isConst)
        assertNull(declaration.value)
    }

    @Test
    fun `boolean is a valid type for both let and const`() {
        // let a: boolean = true; const b: boolean = false;
        val statements =
            parseStatements(
                token(TokenType.LET, "let"),
                token(TokenType.IDENTIFIER, "a"),
                token(TokenType.COLON, ":"),
                token(TokenType.BOOLEANTYPE, "boolean"),
                token(TokenType.ASSIGN, "="),
                token(TokenType.BOOLEANLITERAL, "true"),
                token(TokenType.SEMICOLON, ";"),
                token(TokenType.CONST, "const"),
                token(TokenType.IDENTIFIER, "b"),
                token(TokenType.COLON, ":"),
                token(TokenType.BOOLEANTYPE, "boolean"),
                token(TokenType.ASSIGN, "="),
                token(TokenType.BOOLEANLITERAL, "false"),
                token(TokenType.SEMICOLON, ";"),
            )

        val first = statements[0] as VariableDeclaration
        assertEquals("boolean", first.type)
        assertEquals(true, (first.value as BooleanLiteral).value)

        val second = statements[1] as VariableDeclaration
        assertEquals(true, second.isConst)
        assertEquals(false, (second.value as BooleanLiteral).value)
    }

    @Test
    fun `const under version 1_0 fails naming the feature and the version`() {
        val results =
            parse(
                LanguageVersion.V1_0,
                token(TokenType.CONST, "const"),
                token(TokenType.IDENTIFIER, "x"),
                token(TokenType.COLON, ":"),
                token(TokenType.NUMBERTYPE, "number"),
                token(TokenType.ASSIGN, "="),
                token(TokenType.NUMBERLITERAL, "5"),
                token(TokenType.SEMICOLON, ";"),
            )
        val failure = results[0] as ParseResult.Failure
        assert(failure.message.contains("const declarations"))
        assert(failure.message.contains("1.0"))
    }

    @Test
    fun `the boolean type under version 1_0 fails naming the feature and the version`() {
        val results =
            parse(
                LanguageVersion.V1_0,
                token(TokenType.LET, "let"),
                token(TokenType.IDENTIFIER, "x"),
                token(TokenType.COLON, ":"),
                token(TokenType.BOOLEANTYPE, "boolean"),
                token(TokenType.SEMICOLON, ";"),
            )
        val failure = results[0] as ParseResult.Failure
        assert(failure.message.contains("boolean type"))
        assert(failure.message.contains("1.0"))
    }
}
