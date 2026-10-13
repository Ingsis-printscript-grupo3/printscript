package printscript.linter

import printscript.ast.Assignment
import printscript.ast.BinaryExpression
import printscript.ast.Block
import printscript.ast.BooleanLiteral
import printscript.ast.Expression
import printscript.ast.Identifier
import printscript.ast.IfStatement
import printscript.ast.NumberLiteral
import printscript.ast.PositionedNode
import printscript.ast.PrintCall
import printscript.ast.ReadEnv
import printscript.ast.ReadInput
import printscript.ast.Statement
import printscript.ast.StringLiteral
import printscript.ast.VariableDeclaration

internal fun childrenOf(node: PositionedNode): List<PositionedNode> =
    when (node) {
        is Statement -> childrenOf(node)
        is Expression -> childrenOf(node)
        else -> emptyList() // PositionedNode no es sealed; solo Statement y Expression la implementan
    }

// sin else: un Statement nuevo no compila hasta declarar sus hijos aca
private fun childrenOf(statement: Statement): List<PositionedNode> =
    when (statement) {
        is VariableDeclaration -> listOfNotNull(statement.value)
        is Assignment -> listOf(statement.value)
        is PrintCall -> listOf(statement.value)
        is IfStatement -> listOfNotNull(statement.condition, statement.thenBranch, statement.elseBranch)
        is Block -> statement.statements
    }

private fun childrenOf(expression: Expression): List<PositionedNode> =
    when (expression) {
        is BinaryExpression -> listOf(expression.left, expression.right)
        is ReadInput -> listOf(expression.argument)
        is ReadEnv -> listOf(expression.argument)
        is NumberLiteral, is StringLiteral, is BooleanLiteral, is Identifier -> emptyList()
    }
