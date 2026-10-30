package printscript.parser.statement.handlers

import printscript.ast.Block
import printscript.ast.IfStatement
import printscript.ast.PrintCall
import printscript.ast.VariableDeclaration
import printscript.common.LanguageVersion
import printscript.common.Position
import printscript.common.Token
import printscript.common.TokenType
import printscript.parser.Parser
import printscript.parser.parseResults
import printscript.parser.parseStatements
import printscript.parser.result.ParseResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class IfStatementHandlerTest {
    @Test
    fun `an if without else still requires its block`() {
        val statements = parseStatements("\n\n\nif (x) { println(x); }")

        val ifStatement = statements[0] as IfStatement
        assertEquals(Position(4, 1), ifStatement.position)
        assertEquals(1, ifStatement.thenBranch.statements.size)
        assertIs<PrintCall>(ifStatement.thenBranch.statements[0])
        assertNull(ifStatement.elseBranch)
    }

    @Test
    fun `an if with an else branch`() {
        val statements = parseStatements("if (x) { println(x); } else { println(x); }")

        val ifStatement = statements[0] as IfStatement
        val elseBranch = ifStatement.elseBranch as Block
        assertEquals(1, elseBranch.statements.size)
    }

    @Test
    fun `a block can hold several statements`() {
        val statements = parseStatements("if (x) { let y: number = 1; println(y); }")

        val ifStatement = statements[0] as IfStatement
        assertEquals(2, ifStatement.thenBranch.statements.size)
        assertIs<VariableDeclaration>(ifStatement.thenBranch.statements[0])
        assertIs<PrintCall>(ifStatement.thenBranch.statements[1])
    }

    @Test
    fun `an if without braces around its block fails`() {
        val results = parseResults("if (x) println(x);")

        val failure = results[0] as ParseResult.Failure
        assert(failure.message.contains("Expected '{'"))
    }

    @Test
    fun `the closing brace of the block is missing`() {
        val results = parseResults("if (x) { println(x);")

        val failure = results[0] as ParseResult.Failure
        assert(failure.message.contains("Expected '}'"))
    }

    @Test
    fun `else if is rejected explicitly`() {
        val results = parseResults("if (x) { } else if (y) { }")

        val failure = results[0] as ParseResult.Failure
        assert(failure.message.contains("'else if'"))
    }

    // el lexer 1.0 no emite IF: se arma el token para verificar que el parser 1.0 tambien lo rechaza
    @Test
    fun `an if is not a valid statement under version 1_0`() {
        val position = Position(1, 1)
        val tokens =
            listOf(
                Token(TokenType.IF, position, position, "if"),
                Token(TokenType.EOF, position, position, ""),
            )

        val results = Parser(tokens.iterator(), LanguageVersion.V1_0).parse().asSequence().toList()

        val failure = results[0] as ParseResult.Failure
        assert(failure.message.contains("Unexpected token 'if'"))
    }
}
