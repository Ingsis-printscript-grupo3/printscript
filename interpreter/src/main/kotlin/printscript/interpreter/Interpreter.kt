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

class Interpreter(private val output: (String) -> Unit = { texto -> println(texto) }) {

    private val environment = Environment()

    fun interpretar(statements: List<Statement>) {
        statements.forEach { ejecutar(it) }
    }

    private fun ejecutar(statement: Statement) {
        when (statement) {
            is VariableDeclaration -> {
                val expresion = statement.value
                val valor = if (expresion != null) evaluar(expresion) else null
                environment.declare(statement.name, valor)
            }

            is Assignment ->
                environment.assign(statement.name, evaluar(statement.value))

            is PrintCall ->
                output(textoDe(evaluar(statement.value)))

            else -> throw UnknownStatementError(statement)
        }
    }

    private fun evaluar(expression: Expression): Value =
        when (expression) {
            is NumberLiteral -> NumberValue(expression.value)

            is StringLiteral -> StringValue(expression.value)

            is Identifier -> environment.lookup(expression.name)

            is BinaryExpression -> aplicarOperador(
                expression,
                evaluar(expression.left),
                evaluar(expression.right)
            )

            else -> throw UnknownExpressionError(expression)
        }

    private fun aplicarOperador(expression: BinaryExpression, left: Value, right: Value): Value {
        val operator = expression.operator

        if (operator == TokenType.PLUS) {
            return sumarOConcatenar(left, right)
        }

        // los demas operadores solo funcionan entre numbers
        if (left !is NumberValue || right !is NumberValue) {
            throw TypeMismatchError(nombreDeTipo(left), nombreDeTipo(right))
        }

        return when (operator) {
            TokenType.MINUS -> NumberValue(left.value - right.value)
            TokenType.MULTIPLY -> NumberValue(left.value * right.value)
            TokenType.DIVIDE -> NumberValue(left.value / right.value)
            else -> throw UnknownExpressionError(expression)
        }
    }

    private fun sumarOConcatenar(left: Value, right: Value): Value =
        if (left is NumberValue && right is NumberValue) {
            NumberValue(left.value + right.value)
        } else {
            StringValue(textoDe(left) + textoDe(right))
        }

    private fun textoDe(value: Value): String =
        when (value) {
            is NumberValue -> formatearNumero(value.value)
            is StringValue -> value.value
        }

    private fun formatearNumero(numero: Double): String =
        if (numero % 1.0 == 0.0) {
            numero.toLong().toString()
        } else {
            numero.toString()
        }

    private fun nombreDeTipo(value: Value): String =
        when (value) {
            is NumberValue -> "number"
            is StringValue -> "string"
        }
}
