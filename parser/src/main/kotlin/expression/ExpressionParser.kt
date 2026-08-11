package printscript.parser.expression
import printscript.ast.*
import printscript.common.Position
import printscript.common.TokenType
import printscript.parser.result.ASTResult
import printscript.parser.stream.TokenStream

class ExpressionParser(private val stream: TokenStream) {

    fun parseExpression(): ASTResult<Expression> {
        val leftResult = parseTerm()
        if (leftResult is ASTResult.Failure) return leftResult
        var left = (leftResult as ASTResult.Success).value

        while (stream.match(TokenType.PLUS, TokenType.MINUS)) {
            val operator = stream.previous()?.type ?: return ASTResult.Failure("Missing operator", Position(0,0), Position(0,0))
            val rightResult = parseTerm()
            if (rightResult is ASTResult.Failure) return rightResult
            val right = (rightResult as ASTResult.Success).value
            left = BinaryExpression(left, operator, right)
        }
        return ASTResult.Success(left)
    }

    private fun parseTerm(): ASTResult<Expression> {
        val leftResult = parseFactor()
        if (leftResult is ASTResult.Failure) return leftResult
        var left = (leftResult as ASTResult.Success).value
        
        while (stream.match(TokenType.MULTIPLY, TokenType.DIVIDE)) {
            val operator = stream.previous()?.type ?: return ASTResult.Failure("Missing operator", Position(0,0), Position(0,0))
            val rightResult = parseFactor()
            if (rightResult is ASTResult.Failure) return rightResult
            val right = (rightResult as ASTResult.Success).value
            left = BinaryExpression(left, operator, right)
        }
        return ASTResult.Success(left)
    }

    private fun parseFactor(): ASTResult<Expression> {
        if (stream.match(TokenType.NUMBERLITERAL)) {
            val value = stream.previous()?.value?.toDouble() ?: 0.0
            return ASTResult.Success(NumberLiteral(value))
        }
        if (stream.match(TokenType.STRINGLITERAL)) {
            val value = stream.previous()?.value ?: ""
            return ASTResult.Success(StringLiteral(value))
        }
        if (stream.match(TokenType.IDENTIFIER)) {
            val value = stream.previous()?.value ?: ""
            return ASTResult.Success(Identifier(value))
        }
        if (stream.match(TokenType.LEFTPAREN)) {
            val exprResult = parseExpression()
            if (exprResult is ASTResult.Failure) return exprResult
            
            val consumeResult = stream.consume(TokenType.RIGHTTPAREN, "Expected ')' closing the expression.")
            if (consumeResult is ASTResult.Failure) return consumeResult
            
            return exprResult
        }
        val errorToken = stream.peek()
        val pos = errorToken?.start ?: stream.previous()?.end ?: Position(0, 0)
        return ASTResult.Failure("Syntax Error [Line ${pos.line}]: Expected a value or expression.", pos, errorToken?.end ?: pos)
    }
}
