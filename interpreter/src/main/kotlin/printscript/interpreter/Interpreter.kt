package printscript.interpreter

import printscript.ast.Expression
import printscript.ast.Statement
import printscript.ast.registry.Handler
import printscript.ast.registry.Registry
import printscript.interpreter.output.ConsoleOutput
import printscript.interpreter.output.Output
import printscript.interpreter.plugin.InterpreterContext

class Interpreter(
    private val statementInterpreters: List<Handler<Statement, InterpreterContext, Unit>>,
    private val expressionEvaluators: List<Handler<Expression, InterpreterContext, Value>>,
) : InterpreterInterface {
    private val environment = Environment()

    private val statementRegistry = Registry(statementInterpreters)
    private val expressionRegistry = Registry(expressionEvaluators)

    // Convenience constructor with PrintScript 1.0 plugins
    // output is the destination for println: ConsoleOutput, BucketOutput, or both with MultiOutput
    constructor(output: Output = ConsoleOutput()) : this(
        statementInterpreters = InterpreterFactory.default10StatementInterpreters(output),
        expressionEvaluators = InterpreterFactory.default10ExpressionEvaluators(),
    )

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
