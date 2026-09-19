package printscript.interpreter

import printscript.ast.Expression
import printscript.ast.Statement
import printscript.ast.registry.Handler
import printscript.ast.registry.Registry
import printscript.common.LanguageVersion
import printscript.interpreter.env.EnvProvider
import printscript.interpreter.env.SystemEnvProvider
import printscript.interpreter.input.ConsoleInput
import printscript.interpreter.input.InputProvider
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

    constructor(
        version: LanguageVersion,
        output: Output = ConsoleOutput(),
        input: InputProvider = ConsoleInput(),
        env: EnvProvider = SystemEnvProvider(),
    ) : this(
        statementInterpreters =
            when (version) {
                LanguageVersion.V1_0 -> InterpreterFactory.default10StatementInterpreters(output)
                LanguageVersion.V1_1 -> InterpreterFactory.default11StatementInterpreters(output)
            },
        expressionEvaluators =
            when (version) {
                LanguageVersion.V1_0 -> InterpreterFactory.default10ExpressionEvaluators()
                LanguageVersion.V1_1 -> InterpreterFactory.default11ExpressionEvaluators(input, env)
            },
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
