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

class Formatter(
    private val rules: FormatterRules,
) : FormatterInterface {
    // Recorre el programa y pega cada statement con su separador
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

        // el valor inicial es opcional
        return stmt.value?.let { "$declaration${assignmentOperator()}${formatExpression(it)};" }
            ?: "$declaration;"
    }

    private fun assignmentOperator(): String = if (rules.spaceAroundAssignment) " = " else "="

    private fun formatExpression(expr: Expression): String =
        when (expr) {
            is NumberLiteral -> formatNumber(expr.value)
            is StringLiteral -> "\"${expr.value}\""
            is Identifier -> expr.name
            is BinaryExpression -> formatBinaryExpression(expr)
        }

    private fun formatNumber(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

    private fun formatBinaryExpression(expr: BinaryExpression): String {
        val left = formatOperand(expr.left, expr.operator, isRightOperand = false)
        val right = formatOperand(expr.right, expr.operator, isRightOperand = true)
        return "$left ${formatOperator(expr.operator)} $right"
    }

    private fun formatOperand(
        operand: Expression,
        parentOperator: TokenType,
        isRightOperand: Boolean,
    ): String {
        val text = formatExpression(operand)
        val needsParentheses =
            operand is BinaryExpression &&
                needsParentheses(operand.operator, parentOperator, isRightOperand)
        return if (needsParentheses) "($text)" else text
    }

    private fun needsParentheses(
        childOperator: TokenType,
        parentOperator: TokenType,
        isRightOperand: Boolean,
    ): Boolean {
        val childPrecedence = precedenceOf(childOperator)
        val parentPrecedence = precedenceOf(parentOperator)
        if (childPrecedence < parentPrecedence) return true
        val parentIsNotAssociative = parentOperator == TokenType.MINUS || parentOperator == TokenType.DIVIDE
        return isRightOperand && childPrecedence == parentPrecedence && parentIsNotAssociative
    }

    private fun precedenceOf(operator: TokenType): Int =
        when (operator) {
            TokenType.MULTIPLY, TokenType.DIVIDE -> 2
            else -> 1
        }

    private fun formatOperator(operator: TokenType): String =
        when (operator) {
            TokenType.PLUS -> "+"
            TokenType.MINUS -> "-"
            TokenType.MULTIPLY -> "*"
            TokenType.DIVIDE -> "/"
            else -> error("Token '$operator' is not a valid binary operator")
        }
}
