package printscript.interpreter

import printscript.ast.Expression
import printscript.ast.Statement
import printscript.ast.registry.Handler
import printscript.ast.registry.Registry
import printscript.interpreter.plugin.InterpreterContext

class Interpreter(
    private val statementInterpreters: List<Handler<Statement, InterpreterContext, Unit>>,
    private val expressionEvaluators: List<Handler<Expression, InterpreterContext, Value>>,
) : InterpreterInterface {
    private val environment = Environment()

    private val statementRegistry = Registry(statementInterpreters)
    private val expressionRegistry = Registry(expressionEvaluators)

    override fun interpret(statements: Iterator<Statement>) {
        while (statements.hasNext()) {
            execute(statements.next())
        }
    }

    // Query registry for a statement handler, similar to lexer with readers
    private fun execute(statement: Statement) {
        statementRegistry.resolveOrNull(statement, InterpreterContext(environment, this))
            ?: throw UnknownStatementError(statement)
    }

    override fun evaluate(expression: Expression): Value =
        expressionRegistry.resolveOrNull(expression, InterpreterContext(environment, this))
            ?: throw UnknownExpressionError(expression)
}
