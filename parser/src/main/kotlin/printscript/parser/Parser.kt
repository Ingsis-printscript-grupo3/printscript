package printscript.parser

import printscript.common.LanguageVersion
import printscript.common.Token
import printscript.parser.expression.ExpressionParser
import printscript.parser.result.ASTResult
import printscript.parser.result.ParseResult
import printscript.parser.statement.StatementParser
import printscript.parser.stream.TokenStream

class Parser(
    tokens: Iterator<Token>,
    version: LanguageVersion,
) : ParserInterface {
    private val stream = TokenStream(tokens)
    private val expressionParser = ExpressionParser(stream, version)
    private val statementParser = StatementParser(stream, expressionParser, version)

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
