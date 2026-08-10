package printscript.parser.expression
import printscript.ast.*
import printscript.common.TokenType
import printscript.parser.stream.TokenStream

class ExpressionParser(private val stream: TokenStream) {

    fun parseExpression(): Expression {
        var left = parseTerm()
        while (stream.match(TokenType.PLUS, TokenType.MINUS)) {
            val operator = stream.previous().type
            val right = parseTerm()
            left = BinaryExpression(left, operator, right)
        }
        return left
    }

    private fun parseTerm(): Expression {
        var left = parseFactor()
        while (stream.match(TokenType.MULTIPLY, TokenType.DIVIDE)) {
            val operator = stream.previous().type
            val right = parseFactor()
            left = BinaryExpression(left, operator, right)
        }
        return left
    }

    private fun parseFactor(): Expression {
        if (stream.match(TokenType.NUMBERLITERAL)) {
            return NumberLiteral(stream.previous().value.toDouble())
        }
        if (stream.match(TokenType.STRINGLITERAL)) {
            return StringLiteral(stream.previous().value)
        }
        if (stream.match(TokenType.IDENTIFIER)) {
            return Identifier(stream.previous().value)
        }
        if (stream.match(TokenType.LEFTPAREN)) {
            val expr = parseExpression()
            stream.consume(TokenType.RIGHTTPAREN, "Expected ')' closing the expression.")
            return expr
        }
        val errorToken = stream.peek()
        throw RuntimeException("Syntax Error [Line ${errorToken.start.line}]: Expected a value or expression.")
    }
}
