package printscript.linter.rule

import printscript.ast.Assignment
import printscript.ast.BinaryExpression
import printscript.ast.Block
import printscript.ast.Expression
import printscript.ast.IfStatement
import printscript.ast.PrintCall
import printscript.ast.ReadEnv
import printscript.ast.ReadInput
import printscript.ast.Statement
import printscript.ast.VariableDeclaration
import printscript.linter.Warning

class ReadInputCallArgumentRule : LinterRule {
    override fun check(statement: Statement): List<Warning> {
        val warnings = mutableListOf<Warning>()
        checkStatement(statement, warnings)
        return warnings
    }

    private fun checkStatement(
        statement: Statement,
        warnings: MutableList<Warning>,
    ) {
        when (statement) {
            is VariableDeclaration -> statement.value?.let { checkExpression(it, warnings) }
            is Assignment -> checkExpression(statement.value, warnings)
            is PrintCall -> checkExpression(statement.value, warnings)
            is IfStatement -> {
                checkExpression(statement.condition, warnings)
                checkStatement(statement.thenBranch, warnings)
                statement.elseBranch?.let { checkStatement(it, warnings) }
            }
            is Block -> statement.statements.forEach { checkStatement(it, warnings) }
        }
    }

    private fun checkExpression(
        expression: Expression,
        warnings: MutableList<Warning>,
    ) {
        when (expression) {
            is ReadInput -> {
                if (expression.argument is BinaryExpression) {
                    warnings.add(
                        Warning(
                            message = "readInput can only be called with an identifier or a literal, not an expression",
                            position = expression.position,
                        ),
                    )
                }
                checkExpression(expression.argument, warnings)
            }
            is BinaryExpression -> {
                checkExpression(expression.left, warnings)
                checkExpression(expression.right, warnings)
            }
            is ReadEnv -> checkExpression(expression.argument, warnings)
            else -> Unit
        }
    }
}
