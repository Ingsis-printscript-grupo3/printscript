package printscript.interpreter.plugin.statement

import printscript.ast.VariableDeclaration
import printscript.interpreter.Environment
import printscript.interpreter.InterpreterInterface
import printscript.interpreter.plugin.StatementInterpreter

class VariableDeclarationInterpreter : StatementInterpreter<VariableDeclaration>(VariableDeclaration::class.java) {
    override fun doExecute(
        statement: VariableDeclaration,
        env: Environment,
        interpreter: InterpreterInterface,
    ) {
        val expression = statement.value
        val value = if (expression != null) interpreter.evaluate(expression) else null
        env.declare(statement.name, value)
    }
}
