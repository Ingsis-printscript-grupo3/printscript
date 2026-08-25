package printscript.parser

import printscript.parser.stream.TokenStream
import printscript.parser.expression.DefaultExpressionParselets
import printscript.parser.expression.ExpressionParser
import printscript.parser.statement.DefaultStatementHandlers
import printscript.parser.statement.StatementParser
import printscript.common.Token
import printscript.parser.result.ASTResult
import printscript.parser.result.ParseResult

class Parser(tokens: Iterator<Token>) : ParserInterface {
    private val stream = TokenStream(tokens)
    private val expressionParser = ExpressionParser(stream, DefaultExpressionParselets.infix)
    private val statementParser = StatementParser(stream, expressionParser, DefaultStatementHandlers.map)

    override fun parse(): Iterator<ParseResult> = iterator {
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
