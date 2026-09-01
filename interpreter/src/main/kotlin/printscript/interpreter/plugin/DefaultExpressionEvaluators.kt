package printscript.interpreter.plugin

import printscript.interpreter.plugin.expression.BinaryExpressionEvaluator
import printscript.interpreter.plugin.expression.IdentifierEvaluator
import printscript.interpreter.plugin.expression.NumberLiteralEvaluator
import printscript.interpreter.plugin.expression.StringLiteralEvaluator

// default registry for PrintScript 1.0, same pattern as the parser's DefaultStatementHandlers
object DefaultExpressionEvaluators {
    val list: List<ExpressionEvaluator<*>> =
        listOf(
            NumberLiteralEvaluator(),
            StringLiteralEvaluator(),
            IdentifierEvaluator(),
            BinaryExpressionEvaluator(),
        )
}
