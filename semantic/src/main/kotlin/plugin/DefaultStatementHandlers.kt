package printscript.semantic.plugin

import printscript.semantic.plugin.statement.AssignmentHandler
import printscript.semantic.plugin.statement.PrintCallHandler
import printscript.semantic.plugin.statement.VariableDeclarationHandler

// default registry for PrintScript 1.0, same pattern as the parser's DefaultStatementHandlers
object DefaultStatementHandlers {
    val list: List<StatementHandler<*>> =
        listOf(
            VariableDeclarationHandler(),
            AssignmentHandler(),
            PrintCallHandler(),
        )
}
