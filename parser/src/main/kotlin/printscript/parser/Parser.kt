package printscript.parser

import printscript.common.Token
import printscript.common.TokenType
import printscript.parser.expression.DefaultExpressionParselets
import printscript.parser.expression.ExpressionParser
import printscript.parser.expression.InfixParselet
import printscript.parser.expression.PrefixParselet
import printscript.parser.result.ASTResult
import printscript.parser.result.ParseResult
import printscript.parser.statement.StatementHandler
import printscript.parser.statement.StatementParser
import printscript.parser.stream.TokenStream

// los parselets y handlers se reciben por constructor y no se arman adentro: asi
// quien compone el parser puede armar la combinacion que necesite sin tocar esta clase
class Parser(
    tokens: Iterator<Token>,
    prefixParselets: Map<TokenType, PrefixParselet>,
    statementHandlers: Map<TokenType, StatementHandler>,
    infixParselets: Map<TokenType, InfixParselet> = DefaultExpressionParselets.infix,
) {
    private val stream = TokenStream(tokens)
    private val expressionParser = ExpressionParser(stream, prefixParselets, infixParselets)
    private val statementParser = StatementParser(stream, expressionParser, statementHandlers)

    fun parse(): Iterator<ParseResult> =
        iterator {
            while (!stream.isAtEnd()) {
                val result = statementParser.parseStatement()
                when (result) {
                    is ASTResult.Success -> {
                        yield(ParseResult.Success(result.value))
                    }
                    is ASTResult.Failure -> {
                        yield(ParseResult.Failure(result.message, result.start, result.end))
                        break
                    }
                }
            }
        }
}
