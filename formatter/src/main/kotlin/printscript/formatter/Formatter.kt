package printscript.formatter

import printscript.ast.Expression
import printscript.ast.PrintCall
import printscript.ast.Statement
import printscript.ast.registry.Registry
import printscript.formatter.handler.expression.BinaryExpressionHandler
import printscript.formatter.handler.expression.IdentifierHandler
import printscript.formatter.handler.expression.NumberLiteralHandler
import printscript.formatter.handler.expression.StringLiteralHandler
import printscript.formatter.handler.statement.AssignmentHandler
import printscript.formatter.handler.statement.PrintCallHandler
import printscript.formatter.handler.statement.VariableDeclarationHandler

class Formatter(
    val rules: FormatterRules,
) : FormatterInterface {
    private val statementRegistry: Registry<Statement, Formatter, String> =
        Registry(
            listOf(
                VariableDeclarationHandler(),
                AssignmentHandler(),
                PrintCallHandler(),
            ),
        )

    private val expressionRegistry: Registry<Expression, Formatter, String> =
        Registry(
            listOf(
                NumberLiteralHandler(),
                StringLiteralHandler(),
                IdentifierHandler(),
                BinaryExpressionHandler(),
            ),
        )

    // Recorre el programa y pega cada statement con su separador
    override fun format(statements: List<Statement>): String {
        val builder = StringBuilder()
        statements.forEachIndexed { index, stmt ->
            if (index > 0) builder.append("\n")
            builder.append(formatStatement(stmt))
            val isLast = index == statements.lastIndex
            if (stmt is PrintCall && !isLast) builder.append("\n".repeat(rules.lineBreaksAfterPrintln))
        }
        builder.append("\n")
        return builder.toString()
    }

    fun formatStatement(stmt: Statement): String = statementRegistry.resolve(stmt, this)

    fun formatExpression(expr: Expression): String = expressionRegistry.resolve(expr, this)

    fun assignmentOperator(): String = if (rules.spaceAroundAssignment) " = " else "="
}
