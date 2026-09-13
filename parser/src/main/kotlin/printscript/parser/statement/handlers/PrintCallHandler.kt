package printscript.parser.statement.handlers

import printscript.ast.PrintCall
import printscript.ast.Statement
import printscript.common.TokenType
import printscript.parser.expression.ExpressionParser
import printscript.parser.result.ASTResult
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

        val leftParenResult = stream.consume(TokenType.LEFTPAREN, "Expected '('.")
        if (leftParenResult is ASTResult.Failure) return leftParenResult

        val value =
            when (val result = expressionParser.parseExpression()) {
                is ASTResult.Failure -> return result
                is ASTResult.Success -> result.value
            }

        val rightParenResult = stream.consume(TokenType.RIGHTPAREN, "Expected ')'.")
        if (rightParenResult is ASTResult.Failure) return rightParenResult

        val semiResult = stream.consume(TokenType.SEMICOLON, "Expected ';'.")
        if (semiResult is ASTResult.Failure) return semiResult

        return ASTResult.Success(PrintCall(value, printlnToken.start))
    }
}
