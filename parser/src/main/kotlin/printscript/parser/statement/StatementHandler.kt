package printscript.parser.statement

import printscript.ast.Statement
import printscript.parser.expression.ExpressionParser
import printscript.parser.result.ASTResult
import printscript.parser.stream.TokenStream

fun interface StatementHandler {
    fun parse(
        stream: TokenStream,
        expressionParser: ExpressionParser,
        statementParser: StatementParser,
    ): ASTResult<Statement>
}
