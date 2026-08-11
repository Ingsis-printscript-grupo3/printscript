package printscript.parser

import printscript.parser.stream.TokenStream
import printscript.parser.expression.ExpressionParser
import printscript.parser.statement.StatementParser
import printscript.common.Token

class Parser(tokens: Iterator<Token>) : ParserInterface {
    private val stream = TokenStream(tokens)
    private val expressionParser = ExpressionParser(stream)
    private val statementParser = StatementParser(stream, expressionParser)

    override fun parse(): Iterator<ParseResult> = iterator {
        while (!stream.isAtEnd()) {
            try {
                yield(ParseResult.Success(statementParser.parseStatement()))
            } catch (e: SyntaxException) {
                yield(ParseResult.Failure(e.errorMessage, e.start, e.end))
                break
            } catch (e: Exception) {
                val token = stream.peek()
                yield(ParseResult.Failure(e.message ?: "Unknown error", token.start, token.end))
                break
            }
        }
    }
}
