package printscript.parser.statement.handlers

import printscript.ast.Block
import printscript.ast.Expression
import printscript.ast.IfStatement
import printscript.ast.Statement
import printscript.common.TokenType
import printscript.parser.expression.ExpressionParser
import printscript.parser.result.ASTResult
import printscript.parser.result.unwrap
import printscript.parser.statement.StatementHandler
import printscript.parser.statement.StatementParser
import printscript.parser.stream.TokenStream

object IfStatementHandler : StatementHandler {
    override fun parse(
        stream: TokenStream,
        expressionParser: ExpressionParser,
        statementParser: StatementParser,
    ): ASTResult<Statement> {
        val ifToken = checkNotNull(stream.previous()) { "the if handler runs after its keyword" }
        val condition = parseCondition(stream, expressionParser).unwrap { return it }
        val thenBranch = parseBlock(stream, statementParser).unwrap { return it }
        val elseBranch = parseElseBranch(stream, statementParser).unwrap { return it }
        return ASTResult.Success(IfStatement(condition, thenBranch, elseBranch, ifToken.start))
    }

    private fun parseElseBranch(
        stream: TokenStream,
        statementParser: StatementParser,
    ): ASTResult<Block?> {
        if (!stream.match(TokenType.ELSE)) return ASTResult.Success(null)
        val nextToken = stream.peek()
        if (nextToken?.type == TokenType.IF) {
            return ASTResult.Failure(
                "'else if' is not supported; use nested blocks: else { if (...) { ... } }.",
                nextToken.start,
                nextToken.end,
            )
        }
        return parseBlock(stream, statementParser)
    }

    private fun parseCondition(
        stream: TokenStream,
        expressionParser: ExpressionParser,
    ): ASTResult<Expression> {
        val leftParen = stream.consume(TokenType.LEFTPAREN, "Expected '(' after 'if'.")
        if (leftParen is ASTResult.Failure) return leftParen

        val conditionResult = expressionParser.parseExpression()
        if (conditionResult is ASTResult.Failure) return conditionResult

        val rightParen = stream.consume(TokenType.RIGHTPAREN, "Expected ')' after if condition.")
        if (rightParen is ASTResult.Failure) return rightParen

        return conditionResult
    }

    private fun parseBlock(
        stream: TokenStream,
        statementParser: StatementParser,
    ): ASTResult<Block> {
        val openToken = stream.consume(TokenType.LEFTBRACE, "Expected '{' to open a block.").unwrap { return it }
        val statements = parseBlockStatements(stream, statementParser).unwrap { return it }
        val closeResult = stream.consume(TokenType.RIGHTBRACE, "Expected '}' to close block.")
        if (closeResult is ASTResult.Failure) return closeResult

        return ASTResult.Success(Block(statements, openToken.start))
    }

    private fun parseBlockStatements(
        stream: TokenStream,
        statementParser: StatementParser,
    ): ASTResult<List<Statement>> {
        val statements = mutableListOf<Statement>()
        while (stream.peek()?.type != TokenType.RIGHTBRACE) {
            if (stream.isAtEnd()) {
                val pos = checkNotNull(stream.previous()) { "the block was opened by a '{'" }.end
                return ASTResult.Failure("Expected '}' to close block.", pos, pos)
            }
            statements += statementParser.parseStatement().unwrap { return it }
        }
        return ASTResult.Success(statements)
    }
}
