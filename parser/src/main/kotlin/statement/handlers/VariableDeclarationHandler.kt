package printscript.parser.statement.handlers

import printscript.ast.Expression
import printscript.ast.Statement
import printscript.ast.VariableDeclaration
import printscript.common.Position
import printscript.common.TokenType
import printscript.parser.expression.ExpressionParser
import printscript.parser.result.ASTResult
import printscript.parser.statement.StatementHandler
import printscript.parser.stream.TokenStream

object VariableDeclarationHandler : StatementHandler {
    override fun parse(
        stream: TokenStream,
        expressionParser: ExpressionParser,
    ): ASTResult<Statement> {
        val nameTokenResult = stream.consume(TokenType.IDENTIFIER, "Expected variable name.")
        if (nameTokenResult is ASTResult.Failure) return nameTokenResult
        val nameToken = (nameTokenResult as ASTResult.Success).value

        val colonResult = stream.consume(TokenType.COLON, "Expected ':'.")
        if (colonResult is ASTResult.Failure) return colonResult

        val typeToken =
            if (stream.match(TokenType.NUMBERTYPE, TokenType.STRINGTYPE)) {
                stream.previous()
            } else {
                val errorToken = stream.peek()
                val pos = errorToken?.start ?: stream.previous()?.end ?: Position(0, 0)
                return ASTResult.Failure("Syntax Error: Expected 'number' or 'string'.", pos, errorToken?.end ?: pos)
            }

        var initializer: Expression? = null
        if (stream.match(TokenType.ASSIGN)) {
            val exprResult = expressionParser.parseExpression()
            if (exprResult is ASTResult.Failure) return exprResult
            initializer = (exprResult as ASTResult.Success).value
        }

        val semiResult = stream.consume(TokenType.SEMICOLON, "Expected ';'.")
        if (semiResult is ASTResult.Failure) return semiResult

        return ASTResult.Success(VariableDeclaration(nameToken.value, typeToken?.value ?: "", initializer))
    }
}
