package printscript.parser


import printscript.parser.stream.TokenStream
import printscript.parser.expression.ExpressionParser
import printscript.parser.statement.StatementParser
import printscript.common.Token
import printscript.ast.Statement


class Parser(tokens: List<Token>) : ParserInterface {
    private val stream = TokenStream(tokens)
    private val expressionParser = ExpressionParser(stream)
    private val statementParser = StatementParser(stream, expressionParser)

    override fun parse(): List<Statement> {
        val statements = mutableListOf<Statement>()
        while (!stream.isAtEnd()) {
            statements.add(statementParser.parseStatement())
        }
        return statements
    }
}
