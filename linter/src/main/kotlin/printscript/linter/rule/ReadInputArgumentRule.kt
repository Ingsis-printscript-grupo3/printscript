package printscript.linter.rule

import printscript.ast.Assignment
import printscript.ast.BinaryExpression
import printscript.ast.Expression
import printscript.ast.IfStatement
import printscript.ast.PrintCall
import printscript.ast.ReadEnv
import printscript.ast.ReadInput
import printscript.ast.Statement
import printscript.ast.VariableDeclaration
import printscript.linter.Warning

class ReadInputArgumentRule : LinterRule {
    override fun check(statement: Statement): List<Warning> =
        when (statement) {
            is VariableDeclaration -> {
                val value = statement.value
                if (value != null) checkExpression(value) else emptyList()
            }
            is Assignment -> checkExpression(statement.value)
            is PrintCall -> checkExpression(statement.value)
            is IfStatement -> checkExpression(statement.condition)
            else -> emptyList()
        }

    private fun checkExpression(expression: Expression): List<Warning> =
        readInputsIn(expression)
            .filterNot { it.argument.isLiteralOrIdentifier() }
            .map { Warning(message = MESSAGE, position = it.position) }

    private fun readInputsIn(expression: Expression): List<ReadInput> =
        when (expression) {
            is ReadInput -> listOf(expression) + readInputsIn(expression.argument)
            is ReadEnv -> readInputsIn(expression.argument)
            is BinaryExpression -> readInputsIn(expression.left) + readInputsIn(expression.right)
            else -> emptyList()
        }

    private companion object {
        const val MESSAGE = "readInput can only be called with an identifier or a literal, not an expression"
    }
}
