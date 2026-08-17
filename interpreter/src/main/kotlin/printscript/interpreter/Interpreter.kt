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
import printscript.common.TokenType

class Interpreter(private val output: (String) -> Unit = { text -> println(text) }) {

    private val environment = Environment()

    fun interpret(statements: Iterator<Statement>) {
        while (statements.hasNext()) {
            execute(statements.next())
        }
    }

    private fun execute(statement: Statement) {
        when (statement) {
            is VariableDeclaration -> {
                val expression = statement.value
                val value = if (expression != null) evaluate(expression) else null
                environment.declare(statement.name, value)
            }

            is Assignment ->
                environment.assign(statement.name, evaluate(statement.value))

            is PrintCall ->
                output(textOf(evaluate(statement.value)))

            else -> throw UnknownStatementError(statement)
        }
    }

    private fun evaluate(expression: Expression): Value =
        when (expression) {
            is NumberLiteral -> NumberValue(expression.value)

            is StringLiteral -> StringValue(expression.value)

            is Identifier -> environment.lookup(expression.name)

            is BinaryExpression -> applyOperator(
                expression,
                evaluate(expression.left),
                evaluate(expression.right)
            )

            else -> throw UnknownExpressionError(expression)
        }

    private fun applyOperator(expression: BinaryExpression, left: Value, right: Value): Value {
        val operator = expression.operator

        if (operator == TokenType.PLUS) {
            return addOrConcatenate(left, right)
        }

        // other operators only work between numbers
        if (left !is NumberValue || right !is NumberValue) {
            throw TypeMismatchError(typeName(left), typeName(right))
        }

        return when (operator) {
            TokenType.MINUS -> NumberValue(left.value - right.value)
            TokenType.MULTIPLY -> NumberValue(left.value * right.value)
            TokenType.DIVIDE -> NumberValue(left.value / right.value)
            else -> throw UnknownExpressionError(expression)
        }
    }

    private fun addOrConcatenate(left: Value, right: Value): Value =
        if (left is NumberValue && right is NumberValue) {
            NumberValue(left.value + right.value)
        } else {
            StringValue(textOf(left) + textOf(right))
        }

    private fun textOf(value: Value): String =
        when (value) {
            is NumberValue -> formatNumber(value.value)
            is StringValue -> value.value
        }

    private fun formatNumber(number: Double): String =
        if (number % 1.0 == 0.0) {
            number.toLong().toString()
        } else {
            number.toString()
        }

    private fun typeName(value: Value): String =
        when (value) {
            is NumberValue -> "number"
            is StringValue -> "string"
        }
}
