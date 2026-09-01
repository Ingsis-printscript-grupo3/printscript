package printscript.interpreter.plugin

import printscript.interpreter.output.Output
import printscript.interpreter.plugin.statement.AssignmentInterpreter
import printscript.interpreter.plugin.statement.PrintCallInterpreter
import printscript.interpreter.plugin.statement.VariableDeclarationInterpreter

// default registry for PrintScript 1.0, same pattern as the parser's
// DefaultStatementHandlers. PrintCallInterpreter needs the Output, so this is a
// function instead of a fixed val.
object DefaultStatementInterpreters {
    fun list(output: Output): List<StatementInterpreter<*>> =
        listOf(
            VariableDeclarationInterpreter(),
            AssignmentInterpreter(),
            PrintCallInterpreter(output),
        )
}
