package printscript.formatter

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

class PrintScriptFormatter(
    private val rules: FormatterRules,
) : Formatter {
    override fun format(statements: List<Statement>): String {
        val builder = StringBuilder()
        statements.forEachIndexed { index, stmt ->
            if (index > 0) {
                builder.append(if (stmt is PrintCall) "\n".repeat(rules.lineBreaksBeforePrintln) else "\n")
            }
            builder.append(formatStatement(stmt))
        }
        builder.append("\n")
        return builder.toString()
    }

    private fun formatStatement(stmt: Statement): String =
        when (stmt) {
            is VariableDeclaration -> formatVariableDeclaration(stmt)
            is Assignment -> "${stmt.name}${assignmentOperator()}${formatExpression(stmt.value)};"
            is PrintCall -> "println(${formatExpression(stmt.value)});"
        }

    private fun formatVariableDeclaration(stmt: VariableDeclaration): String {
        val colon = "${if (rules.spaceBeforeColon) " " else ""}:${if (rules.spaceAfterColon) " " else ""}"
        val declaration = "let ${stmt.name}$colon${stmt.type}"
        return stmt.value?.let { "$declaration${assignmentOperator()}${formatExpression(it)};" }
            ?: "$declaration;"
    }

    private fun assignmentOperator(): String = if (rules.spaceAroundAssignment) " = " else "="

    private fun formatExpression(expr: Expression): String =
        when (expr) {
            is NumberLiteral -> expr.value.toString()
            is StringLiteral -> "\"${expr.value}\""
            is Identifier -> expr.name
            is BinaryExpression -> "${formatExpression(expr.left)} ${formatOperator(expr.operator)} ${formatExpression(expr.right)}"
        }

    private fun formatOperator(operator: TokenType): String =
        when (operator) {
            TokenType.PLUS -> "+"
            TokenType.MINUS -> "-"
            TokenType.MULTIPLY -> "*"
            TokenType.DIVIDE -> "/"
            else -> throw IllegalStateException("Token '$operator' is not a valid binary operator")
        }
}
