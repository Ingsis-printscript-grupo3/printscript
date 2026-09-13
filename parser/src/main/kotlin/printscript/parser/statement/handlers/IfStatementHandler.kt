package printscript.parser.statement.handlers

import printscript.ast.Block
import printscript.ast.Expression
import printscript.ast.IfStatement
import printscript.ast.Statement
import printscript.common.TokenType
import printscript.parser.expression.ExpressionParser
import printscript.parser.result.ASTResult
import printscript.parser.statement.StatementHandler
import printscript.parser.statement.StatementParser
import printscript.parser.stream.TokenStream

object IfStatementHandler : StatementHandler {
    override fun parse(
        stream: TokenStream,
        expressionParser: ExpressionParser,
        statementParser: StatementParser,
    ): ASTResult<Statement> {
        // el StatementParser consume el if antes de despachar el handler.
        // en 1.0 este handler ni se registra, asi que no hay nada de version que mirar aca
        val ifToken = checkNotNull(stream.previous()) { "the if handler runs after its keyword" }

        val condition =
            when (val result = parseCondition(stream, expressionParser)) {
                is ASTResult.Failure -> return result
                is ASTResult.Success -> result.value
            }

        val thenBranch =
            when (val result = parseBlock(stream, statementParser)) {
                is ASTResult.Failure -> return result
                is ASTResult.Success -> result.value
            }

        var elseBranch: Block? = null
        if (stream.match(TokenType.ELSE)) {
            val nextToken = stream.peek()
            if (nextToken?.type == TokenType.IF) {
                return ASTResult.Failure(
                    "'else if' is not supported; use nested blocks: else { if (...) { ... } }.",
                    nextToken.start,
                    nextToken.end,
                )
            }
            elseBranch =
                when (val result = parseBlock(stream, statementParser)) {
                    is ASTResult.Failure -> return result
                    is ASTResult.Success -> result.value
                }
        }

        return ASTResult.Success(IfStatement(condition, thenBranch, elseBranch, ifToken.start))
    }

    private fun parseCondition(
        stream: TokenStream,
        expressionParser: ExpressionParser,
    ): ASTResult<Expression> {
        val leftParenResult = stream.consume(TokenType.LEFTPAREN, "Expected '(' after 'if'.")
        if (leftParenResult is ASTResult.Failure) return leftParenResult

        val conditionResult = expressionParser.parseExpression()
        if (conditionResult is ASTResult.Failure) return conditionResult

        val rightParenResult = stream.consume(TokenType.RIGHTPAREN, "Expected ')' after if condition.")
        if (rightParenResult is ASTResult.Failure) return rightParenResult

        return conditionResult
    }

    private fun parseBlock(
        stream: TokenStream,
        statementParser: StatementParser,
    ): ASTResult<Block> {
        val openToken =
            when (val result = stream.consume(TokenType.LEFTBRACE, "Expected '{' to open a block.")) {
                is ASTResult.Failure -> return result
                is ASTResult.Success -> result.value
            }

        val statements = mutableListOf<Statement>()
        while (stream.peek()?.type != TokenType.RIGHTBRACE) {
            if (stream.isAtEnd()) {
                // siempre hay un token previo: el '{' que se acaba de consumir
                val pos = checkNotNull(stream.previous()) { "the block was opened by a '{'" }.end
                return ASTResult.Failure("Expected '}' to close block.", pos, pos)
            }
            statements +=
                when (val result = statementParser.parseStatement()) {
                    is ASTResult.Failure -> return result
                    is ASTResult.Success -> result.value
                }
        }

        val closeResult = stream.consume(TokenType.RIGHTBRACE, "Expected '}' to close block.")
        if (closeResult is ASTResult.Failure) return closeResult

        return ASTResult.Success(Block(statements, openToken.start))
    }
}
