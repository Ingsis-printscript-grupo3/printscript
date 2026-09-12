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
import java.io.Writer

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

    // leo el siguiente antes, para saber si el actual es el ultimo
    override fun format(
        statements: Iterator<Statement>,
        output: Writer,
    ) {
        var next: Statement? = if (statements.hasNext()) statements.next() else null
        var first = true
        while (next != null) {
            val current = next
            next = if (statements.hasNext()) statements.next() else null
            if (!first) output.write("\n")
            output.write(formatStatement(current))
            if (current is PrintCall && next != null) {
                output.write("\n".repeat(rules.lineBreaksAfterPrintln))
            }
            first = false
        }
        output.write("\n")
        output.flush()
    }

    fun formatStatement(stmt: Statement): String = statementRegistry.resolve(stmt, this)

    fun formatExpression(expr: Expression): String = expressionRegistry.resolve(expr, this)

    fun assignmentOperator(): String = if (rules.spaceAroundAssignment) " = " else "="
}
