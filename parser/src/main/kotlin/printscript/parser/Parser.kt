package printscript.parser

import printscript.common.LanguageVersion
import printscript.common.Token
import printscript.common.TokenType
import printscript.parser.expression.DefaultExpressionParselets
import printscript.parser.expression.ExpressionParser
import printscript.parser.expression.InfixParselet
import printscript.parser.expression.PrefixParselet
import printscript.parser.result.ASTResult
import printscript.parser.result.ParseResult
import printscript.parser.statement.DefaultStatementHandlers
import printscript.parser.statement.StatementHandler
import printscript.parser.statement.StatementParser
import printscript.parser.stream.TokenStream

// los parselets y handlers se reciben por constructor y no se arman adentro: asi
// ParserFactory puede componer otra combinacion sin tocar esta clase
class Parser(
    tokens: Iterator<Token>,
    version: LanguageVersion,
    prefixParselets: Map<TokenType, PrefixParselet> = DefaultExpressionParselets.prefix(version),
    infixParselets: Map<TokenType, InfixParselet> = DefaultExpressionParselets.infix,
    statementHandlers: Map<TokenType, StatementHandler> = DefaultStatementHandlers.map(version),
) : ParserInterface {
    private val stream = TokenStream(tokens)
    private val expressionParser = ExpressionParser(stream, version, prefixParselets, infixParselets)
    private val statementParser = StatementParser(stream, expressionParser, version, statementHandlers)

    override fun parse(): Iterator<ParseResult> =
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
