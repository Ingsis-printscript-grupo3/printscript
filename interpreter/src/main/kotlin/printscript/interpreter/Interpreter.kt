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
import printscript.interpreter.plugin.ExpressionEvaluator
import printscript.interpreter.plugin.StatementInterpreter
import printscript.interpreter.plugin.expression.BinaryExpressionEvaluator
import printscript.interpreter.plugin.expression.IdentifierEvaluator
import printscript.interpreter.plugin.expression.NumberLiteralEvaluator
import printscript.interpreter.plugin.expression.StringLiteralEvaluator
import printscript.interpreter.plugin.statement.AssignmentInterpreter
import printscript.interpreter.plugin.statement.PrintCallInterpreter
import printscript.interpreter.plugin.statement.VariableDeclarationInterpreter
import kotlin.reflect.KClass

class Interpreter(
    private val statementInterpreters: Map<KClass<out Statement>, StatementInterpreter<out Statement>>,
    private val expressionEvaluators: Map<KClass<out Expression>, ExpressionEvaluator<out Expression>>
) : InterpreterInterface {
    private val environment = Environment()

    // Constructor secundario para mantener compatibilidad con los tests actuales
    constructor(output: (String) -> Unit = { println(it) }) : this(
        statementInterpreters = mapOf(
            VariableDeclaration::class to VariableDeclarationInterpreter(),
            Assignment::class to AssignmentInterpreter(),
            PrintCall::class to PrintCallInterpreter(output)
        ),
        expressionEvaluators = mapOf(
            NumberLiteral::class to NumberLiteralEvaluator(),
            StringLiteral::class to StringLiteralEvaluator(),
            Identifier::class to IdentifierEvaluator(),
            BinaryExpression::class to BinaryExpressionEvaluator()
        )
    )

    override fun interpret(statements: Iterator<Statement>) {
        while (statements.hasNext()) {
            execute(statements.next())
        }
    }

    private fun execute(statement: Statement) {
        @Suppress("UNCHECKED_CAST")
        val plugin = statementInterpreters[statement::class] as? StatementInterpreter<Statement>
            ?: throw UnknownStatementError(statement)
            
        plugin.execute(statement, environment, this)
    }

    override fun evaluate(expression: Expression): Value {
        @Suppress("UNCHECKED_CAST")
        val plugin = expressionEvaluators[expression::class] as? ExpressionEvaluator<Expression>
            ?: throw UnknownExpressionError(expression)
            
        return plugin.evaluate(expression, environment, this)
    }
}
