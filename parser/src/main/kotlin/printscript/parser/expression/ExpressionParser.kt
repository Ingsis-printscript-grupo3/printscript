package printscript.parser.expression

import printscript.ast.Expression
import printscript.common.LanguageVersion
import printscript.common.Position
import printscript.common.TokenType
import printscript.parser.result.ASTResult
import printscript.parser.stream.TokenStream
import printscript.parser.version.VersionFeatures

class ExpressionParser(
    private val stream: TokenStream,
    private val version: LanguageVersion,
    private val prefixParselets: Map<TokenType, PrefixParselet> = DefaultExpressionParselets.prefix(version),
    private val infixParselets: Map<TokenType, InfixParselet> = DefaultExpressionParselets.infix,
) {
    fun parseExpression(minPrecedence: Int = 0): ASTResult<Expression> {
        var left =
            when (val result = parsePrimary()) {
                is ASTResult.Failure -> return result
                is ASTResult.Success -> result.value
            }

        while (true) {
            val token = stream.peek() ?: break
            val parselet = infixParselets[token.type] ?: break
            if (parselet.precedence < minPrecedence) break

            stream.advance()
            val operatorToken = stream.previous() ?: break
            left =
                when (val result = parselet.parse(left, operatorToken, stream, this)) {
                    is ASTResult.Failure -> return result
                    is ASTResult.Success -> result.value
                }
        }
        return ASTResult.Success(left)
    }

    private fun parsePrimary(): ASTResult<Expression> {
        val token = stream.peek()
        if (token == null) {
            val pos = stream.previous()?.end ?: Position(0, 0)
            return ASTResult.Failure("Expected a value or expression.", pos, pos)
        }

        val parselet = prefixParselets[token.type]
        if (parselet == null) {
            // igual que en el StatementParser: la feature puede existir en otra version
            return VersionFeatures.unavailable(token, version)
                ?: ASTResult.Failure("Expected a value or expression.", token.start, token.end)
        }
        stream.advance()
        return parselet.parse(token, stream, this)
    }
}
