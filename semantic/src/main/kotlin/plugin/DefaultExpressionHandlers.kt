package printscript.semantic.plugin

import printscript.semantic.plugin.expression.BinaryExpressionHandler
import printscript.semantic.plugin.expression.IdentifierHandler
import printscript.semantic.plugin.expression.NumberLiteralHandler
import printscript.semantic.plugin.expression.StringLiteralHandler

// default registry for PrintScript 1.0, same pattern as the parser's DefaultStatementHandlers
object DefaultExpressionHandlers {
    val list: List<ExpressionHandler<*>> =
        listOf(
            NumberLiteralHandler(),
            StringLiteralHandler(),
            IdentifierHandler(),
            BinaryExpressionHandler(),
        )
}
