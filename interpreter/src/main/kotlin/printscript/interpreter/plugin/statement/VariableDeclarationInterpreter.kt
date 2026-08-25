package printscript.interpreter.plugin.statement

import printscript.ast.Statement
import printscript.ast.VariableDeclaration
import printscript.interpreter.Environment
import printscript.interpreter.InterpreterInterface
import printscript.interpreter.UnknownStatementError
import printscript.interpreter.plugin.StatementInterpreter

class VariableDeclarationInterpreter : StatementInterpreter {

    override fun matches(statement: Statement) = statement is VariableDeclaration

    override fun execute(statement: Statement, env: Environment, interpreter: InterpreterInterface) {
        if (statement !is VariableDeclaration) throw UnknownStatementError(statement)

        val expression = statement.value
        val value = if (expression != null) interpreter.evaluate(expression) else null
        env.declare(statement.name, value)
    }
}
