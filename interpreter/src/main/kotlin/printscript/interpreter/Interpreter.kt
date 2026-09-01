package printscript.interpreter

import printscript.ast.Assignment
import printscript.ast.BinaryExpression
import printscript.ast.Expression
import printscript.ast.Identifier
import printscript.ast.NumberLiteral
import printscript.ast.PrintCall
import printscript.ast.Statement
import printscript.ast.StringLiteral
import printscript.ast.VariableDeclaration
import printscript.interpreter.output.ConsoleOutput
import printscript.interpreter.output.Output
import printscript.interpreter.plugin.DefaultExpressionEvaluators
import printscript.interpreter.plugin.DefaultStatementInterpreters
import printscript.interpreter.plugin.ExpressionEvaluator
import printscript.interpreter.plugin.StatementInterpreter

class Interpreter(
    private val statementInterpreters: List<StatementInterpreter<*>>,
    private val expressionEvaluators: List<ExpressionEvaluator<*>>,
) : InterpreterInterface {
    private val environment = Environment()

    // convenience constructor with PrintScript 1.0's default plugins
    // output is where println goes: ConsoleOutput, BucketOutput, or both via MultiOutput
    constructor(output: Output = ConsoleOutput()) : this(
        statementInterpreters = DefaultStatementInterpreters.list(output),
        expressionEvaluators = DefaultExpressionEvaluators.list,
    )

    override fun interpret(statements: Iterator<Statement>) {
        while (statements.hasNext()) {
            execute(statements.next())
        }
    }

    // ask each plugin whether the statement is theirs, same as the lexer does with its readers
    private fun execute(statement: Statement) {
        statementExhaustivenessWitness(statement)

        val plugin =
            statementInterpreters.firstOrNull { it.matches(statement) }
                ?: throw UnknownStatementError(statement)

        plugin.execute(statement, environment, this)
    }

    override fun evaluate(expression: Expression): Value {
        expressionExhaustivenessWitness(expression)

        val plugin =
            expressionEvaluators.firstOrNull { it.matches(expression) }
                ?: throw UnknownExpressionError(expression)

        return plugin.evaluate(expression, environment, this)
    }

    // compile-time safety net: a private exhaustive `when` with no `else`.
    // it doesn't decide anything (every branch is empty): its sole purpose is to make
    // the compiler stop compiling this module when a new Statement subtype is added
    // to the sealed interface in ast.
    // what to do when it fails: add the new branch here AND register its
    // StatementInterpreter in DefaultStatementInterpreters (the completeness test via
    // Statement::class.sealedSubclasses will also fail if you forget to register it).
    private fun statementExhaustivenessWitness(statement: Statement) {
        when (statement) {
            is VariableDeclaration -> {}
            is Assignment -> {}
            is PrintCall -> {}
        }
    }

    // same mechanism as statementExhaustivenessWitness, for Expression
    private fun expressionExhaustivenessWitness(expression: Expression) {
        when (expression) {
            is NumberLiteral -> {}
            is StringLiteral -> {}
            is Identifier -> {}
            is BinaryExpression -> {}
        }
    }
}
