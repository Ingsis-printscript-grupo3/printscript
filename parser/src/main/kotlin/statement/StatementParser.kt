package printscript.parser.statement

import printscript.ast.*
import printscript.common.Position
import printscript.common.TokenType
import printscript.parser.stream.TokenStream
import printscript.parser.expression.ExpressionParser
import printscript.parser.result.ASTResult

class StatementParser(
    private val stream: TokenStream,
    private val expressionParser: ExpressionParser
) {

    fun parseStatement(): ASTResult<Statement> {
        if (stream.match(TokenType.LET)) return parseDeclaration()
        if (stream.match(TokenType.PRINTLN)) return parsePrintCall()
        if (stream.match(TokenType.IDENTIFIER)) return parseAssignment()

        val errorToken = stream.peek()
        val pos = errorToken?.start ?: stream.previous()?.end ?: Position(0, 0)
        return ASTResult.Failure("Syntax Error [Line ${pos.line}]: Unexpected token '${errorToken?.value}'.", pos, errorToken?.end ?: pos)
    }

    private fun parseDeclaration(): ASTResult<Statement> {
        val nameTokenResult = stream.consume(TokenType.IDENTIFIER, "Expected variable name.")
        if (nameTokenResult is ASTResult.Failure) return nameTokenResult
        val nameToken = (nameTokenResult as ASTResult.Success).value
        
        val colonResult = stream.consume(TokenType.COLON, "Expected ':'.")
        if (colonResult is ASTResult.Failure) return colonResult
        
        val typeToken = if (stream.match(TokenType.NUMBERTYPE, TokenType.STRINGTYPE)) {
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

    private fun parsePrintCall(): ASTResult<Statement> {
        val leftParenResult = stream.consume(TokenType.LEFTPAREN, "Expected '('.")
        if (leftParenResult is ASTResult.Failure) return leftParenResult
        
        val exprResult = expressionParser.parseExpression()
        if (exprResult is ASTResult.Failure) return exprResult
        val value = (exprResult as ASTResult.Success).value
        
        val rightParenResult = stream.consume(TokenType.RIGHTTPAREN, "Expected ')'.")
        if (rightParenResult is ASTResult.Failure) return rightParenResult
        
        val semiResult = stream.consume(TokenType.SEMICOLON, "Expected ';'.")
        if (semiResult is ASTResult.Failure) return semiResult
        
        return ASTResult.Success(PrintCall(value))
    }

    private fun parseAssignment(): ASTResult<Statement> {
        val nameToken = stream.previous() ?: return ASTResult.Failure("Expected identifier", Position(0,0), Position(0,0))
        
        val assignResult = stream.consume(TokenType.ASSIGN, "Expected '='.")
        if (assignResult is ASTResult.Failure) return assignResult
        
        val exprResult = expressionParser.parseExpression()
        if (exprResult is ASTResult.Failure) return exprResult
        val value = (exprResult as ASTResult.Success).value
        
        val semiResult = stream.consume(TokenType.SEMICOLON, "Expected ';'.")
        if (semiResult is ASTResult.Failure) return semiResult
        
        return ASTResult.Success(Assignment(nameToken.value, value))
    }
}
