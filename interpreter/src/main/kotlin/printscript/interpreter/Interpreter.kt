package printscript.interpreter

import printscript.ast.Expression
import printscript.ast.Statement
import printscript.ast.registry.Handler
import printscript.ast.registry.Registry
import printscript.interpreter.output.ConsoleOutput
import printscript.interpreter.output.Output
import printscript.interpreter.plugin.InterpreterContext
import printscript.interpreter.plugin.expression.BinaryExpressionEvaluator
import printscript.interpreter.plugin.expression.IdentifierEvaluator
import printscript.interpreter.plugin.expression.NumberLiteralEvaluator
import printscript.interpreter.plugin.expression.StringLiteralEvaluator
import printscript.interpreter.plugin.statement.AssignmentInterpreter
import printscript.interpreter.plugin.statement.PrintCallInterpreter
import printscript.interpreter.plugin.statement.VariableDeclarationInterpreter

class Interpreter(
    private val statementInterpreters: List<Handler<Statement, InterpreterContext, Unit>>,
    private val expressionEvaluators: List<Handler<Expression, InterpreterContext, Value>>,
) : InterpreterInterface {
    private val environment = Environment()

    private val statementRegistry = Registry(statementInterpreters)
    private val expressionRegistry = Registry(expressionEvaluators)

    // constructor de conveniencia con los plugins de PrintScript 1.0
    // output es el destino de los println: ConsoleOutput, BucketOutput, o los dos con MultiOutput
    constructor(output: Output = ConsoleOutput()) : this(
        statementInterpreters =
            listOf(
                VariableDeclarationInterpreter(),
                AssignmentInterpreter(),
                PrintCallInterpreter(output),
            ),
        expressionEvaluators =
            listOf(
                NumberLiteralEvaluator(),
                StringLiteralEvaluator(),
                IdentifierEvaluator(),
                BinaryExpressionEvaluator(),
            ),
    )

    override fun interpret(statements: Iterator<Statement>) {
        while (statements.hasNext()) {
            execute(statements.next())
        }
    }

    // le pregunto al registry si hay un handler para el statement, igual q el lexer con los readers
    private fun execute(statement: Statement) {
        statementRegistry.resolveOrNull(statement, InterpreterContext(environment, this))
            ?: throw UnknownStatementError(statement)
    }

    override fun evaluate(expression: Expression): Value =
        expressionRegistry.resolveOrNull(expression, InterpreterContext(environment, this))
            ?: throw UnknownExpressionError(expression)
}
