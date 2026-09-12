package printscript.formatter

import printscript.ast.Expression
import printscript.ast.PrintCall
import printscript.ast.Statement
import printscript.ast.registry.Registry
import printscript.formatter.handler.expression.BinaryExpressionHandler
import printscript.formatter.handler.expression.BooleanLiteralHandler
import printscript.formatter.handler.expression.IdentifierHandler
import printscript.formatter.handler.expression.NumberLiteralHandler
import printscript.formatter.handler.expression.ReadEnvHandler
import printscript.formatter.handler.expression.ReadInputHandler
import printscript.formatter.handler.expression.StringLiteralHandler
import printscript.formatter.handler.statement.AssignmentHandler
import printscript.formatter.handler.statement.BlockHandler
import printscript.formatter.handler.statement.IfStatementHandler
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
                IfStatementHandler(),
                BlockHandler(),
            ),
        )

    private val expressionRegistry: Registry<Expression, Formatter, String> =
        Registry(
            listOf(
                NumberLiteralHandler(),
                StringLiteralHandler(),
                IdentifierHandler(),
                BinaryExpressionHandler(),
                BooleanLiteralHandler(),
                ReadInputHandler(),
                ReadEnvHandler(),
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
            if (next != null) output.write(lineBreaksAfter(current))
            first = false
        }
        output.flush()
    }

    fun formatStatement(stmt: Statement): String = statementRegistry.resolve(stmt, this)

    fun formatExpression(expr: Expression): String = expressionRegistry.resolve(expr, this)

    fun assignmentOperator(): String = if (rules.spaceAroundAssignment) " = " else "="

    fun lineBreaksAfter(statement: Statement): String =
        if (statement is PrintCall) "\n".repeat(rules.lineBreaksAfterPrintln) else ""
}
