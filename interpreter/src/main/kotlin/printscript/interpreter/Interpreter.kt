package printscript.interpreter

import printscript.ast.Expression
import printscript.ast.Statement
import printscript.interpreter.output.ConsoleOutput
import printscript.interpreter.output.Output
import printscript.interpreter.plugin.ExpressionEvaluator
import printscript.interpreter.plugin.StatementInterpreter
import printscript.interpreter.plugin.expression.BinaryExpressionEvaluator
import printscript.interpreter.plugin.expression.IdentifierEvaluator
import printscript.interpreter.plugin.expression.NumberLiteralEvaluator
import printscript.interpreter.plugin.expression.StringLiteralEvaluator
import printscript.interpreter.plugin.statement.AssignmentInterpreter
import printscript.interpreter.plugin.statement.PrintCallInterpreter
import printscript.interpreter.plugin.statement.VariableDeclarationInterpreter

class Interpreter(
    private val statementInterpreters: List<StatementInterpreter>,
    private val expressionEvaluators: List<ExpressionEvaluator>,
) : InterpreterInterface {
    private val environment = Environment()

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

    // le pregunto a c plugin si el statement es suyo, igual q el lexer con los readers
    private fun execute(statement: Statement) {
        val plugin =
            statementInterpreters.firstOrNull { it.matches(statement) }
                ?: throw UnknownStatementError(statement)

        plugin.execute(statement, environment, this)
    }

    override fun evaluate(expression: Expression): Value {
        val plugin =
            expressionEvaluators.firstOrNull { it.matches(expression) }
                ?: throw UnknownExpressionError(expression)

        return plugin.evaluate(expression, environment, this)
    }
}
