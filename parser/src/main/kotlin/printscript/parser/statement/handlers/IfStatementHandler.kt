package printscript.parser.statement.handlers

import printscript.ast.Block
import printscript.ast.Expression
import printscript.ast.IfStatement
import printscript.ast.Statement
import printscript.common.LanguageVersion
import printscript.common.Position
import printscript.common.TokenType
import printscript.parser.expression.ExpressionParser
import printscript.parser.result.ASTResult
import printscript.parser.statement.StatementHandler
import printscript.parser.statement.StatementParser
import printscript.parser.stream.TokenStream
import printscript.parser.version.VersionGate

class IfStatementHandler(private val version: LanguageVersion) : StatementHandler {
    override fun parse(
        stream: TokenStream,
        expressionParser: ExpressionParser,
        statementParser: StatementParser,
    ): ASTResult<Statement> {
        val ifToken =
            stream.previous()
                ?: return ASTResult.Failure("Expected 'if'.", Position(0, 0), Position(0, 0))
        VersionGate.check("if statements", LanguageVersion.V1_1, version, ifToken.start, ifToken.end)?.let { return it }

        val conditionResult = parseCondition(stream, expressionParser)
        if (conditionResult is ASTResult.Failure) return conditionResult
        val condition = (conditionResult as ASTResult.Success).value

        val thenBranchResult = parseBlock(stream, statementParser)
        if (thenBranchResult is ASTResult.Failure) return thenBranchResult
        val thenBranch = (thenBranchResult as ASTResult.Success).value

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
            val elseBranchResult = parseBlock(stream, statementParser)
            if (elseBranchResult is ASTResult.Failure) return elseBranchResult
            elseBranch = (elseBranchResult as ASTResult.Success).value
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
        val openResult = stream.consume(TokenType.LEFTBRACE, "Expected '{' to open a block.")
        if (openResult is ASTResult.Failure) return openResult
        val openToken = (openResult as ASTResult.Success).value

        val statements = mutableListOf<Statement>()
        while (stream.peek()?.type != TokenType.RIGHTBRACE) {
            if (stream.isAtEnd()) {
                val pos = stream.previous()?.end ?: Position(0, 0)
                return ASTResult.Failure("Expected '}' to close block.", pos, pos)
            }
            val statementResult = statementParser.parseStatement()
            if (statementResult is ASTResult.Failure) return statementResult
            statements += (statementResult as ASTResult.Success).value
        }

        val closeResult = stream.consume(TokenType.RIGHTBRACE, "Expected '}' to close block.")
        if (closeResult is ASTResult.Failure) return closeResult

        return ASTResult.Success(Block(statements, openToken.start))
    }
}
