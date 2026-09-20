package printscript.parser.statement.handlers

import printscript.ast.PrintCall
import printscript.ast.Statement
import printscript.common.Token
import printscript.common.TokenType
import printscript.parser.expression.ExpressionParser
import printscript.parser.result.ASTResult
import printscript.parser.result.unwrap
import printscript.parser.statement.StatementHandler
import printscript.parser.statement.StatementParser
import printscript.parser.stream.TokenStream

object PrintCallHandler : StatementHandler {
    override fun parse(
        stream: TokenStream,
        expressionParser: ExpressionParser,
        statementParser: StatementParser,
    ): ASTResult<Statement> {
        // el StatementParser consume el println antes de despachar el handler
        val printlnToken = checkNotNull(stream.previous()) { "the println handler runs after its keyword" }

        stream.consume(TokenType.LEFTPAREN, "Expected '('.").unwrap { return it }
        val value = expressionParser.parseExpression().unwrap { return it }
        closeCall(stream).unwrap { return it }

        return ASTResult.Success(PrintCall(value, printlnToken.start))
    }

    private fun closeCall(stream: TokenStream): ASTResult<Token> {
        val rightParen = stream.consume(TokenType.RIGHTPAREN, "Expected ')'.")
        if (rightParen is ASTResult.Failure) return rightParen
        return stream.consume(TokenType.SEMICOLON, "Expected ';'.")
    }
}
