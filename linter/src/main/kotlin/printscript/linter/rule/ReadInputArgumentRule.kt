package printscript.linter.rule

import printscript.ast.Assignment
import printscript.ast.BinaryExpression
import printscript.ast.BooleanLiteral
import printscript.ast.Expression
import printscript.ast.Identifier
import printscript.ast.IfStatement
import printscript.ast.NumberLiteral
import printscript.ast.PrintCall
import printscript.ast.ReadEnv
import printscript.ast.ReadInput
import printscript.ast.Statement
import printscript.ast.StringLiteral
import printscript.ast.VariableDeclaration
import printscript.linter.Warning

class ReadInputArgumentRule : LinterRule {
    override fun check(statement: Statement): List<Warning> =
        readInputsIn(rootExpressionOf(statement))
            .filterNot { isLiteralOrIdentifier(it.argument) }
            .map { Warning(message = MESSAGE, position = it.position) }

    // readInput es una Expression, asi que hay que ir a buscarla al valor del statement
    private fun rootExpressionOf(statement: Statement): Expression? =
        when (statement) {
            is VariableDeclaration -> statement.value
            is Assignment -> statement.value
            is PrintCall -> statement.value
            is IfStatement -> statement.condition
            else -> null
        }

    private fun readInputsIn(expression: Expression?): List<ReadInput> =
        when (expression) {
            null -> emptyList()
            is ReadInput -> listOf(expression) + readInputsIn(expression.argument)
            is ReadEnv -> readInputsIn(expression.argument)
            is BinaryExpression -> readInputsIn(expression.left) + readInputsIn(expression.right)
            else -> emptyList()
        }

    private fun isLiteralOrIdentifier(expression: Expression): Boolean =
        expression is Identifier ||
            expression is NumberLiteral ||
            expression is StringLiteral ||
            expression is BooleanLiteral

    private companion object {
        const val MESSAGE = "readInput can only be called with an identifier or a literal, not an expression"
    }
}
