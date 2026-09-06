package printscript.formatter.handler

import printscript.ast.Assignment
import printscript.ast.NumberLiteral
import printscript.ast.PrintCall
import printscript.ast.StringLiteral
import printscript.formatter.Formatter
import printscript.formatter.FormatterRules
import printscript.formatter.handler.expression.BinaryExpressionHandler
import printscript.formatter.handler.expression.IdentifierHandler
import printscript.formatter.handler.expression.NumberLiteralHandler
import printscript.formatter.handler.expression.StringLiteralHandler
import printscript.formatter.handler.statement.AssignmentHandler
import printscript.formatter.handler.statement.PrintCallHandler
import printscript.formatter.handler.statement.VariableDeclarationHandler
import kotlin.test.Test
import kotlin.test.assertFailsWith

// cada handler tiene un guard que falla si le llega un nodo que no es suyo
// por el flujo normal nunca pasa, pq el Registry pregunta applies() antes

class HandlerGuardsTest {
    private val formatter = Formatter(FormatterRules())

    @Test
    fun `expression handlers reject nodes that are not theirs`() {
        val number = NumberLiteral(1.0)
        val string = StringLiteral("hi")

        val cases =
            listOf(
                NumberLiteralHandler() to string,
                StringLiteralHandler() to number,
                IdentifierHandler() to number,
                BinaryExpressionHandler() to number,
            )

        for ((handler, foreignNode) in cases) {
            assertFailsWith<IllegalStateException> { handler.handle(foreignNode, formatter) }
        }
    }

    @Test
    fun `statement handlers reject nodes that are not theirs`() {
        val print = PrintCall(NumberLiteral(1.0))
        val assignment = Assignment("x", NumberLiteral(1.0))

        val cases =
            listOf(
                VariableDeclarationHandler() to print,
                AssignmentHandler() to print,
                PrintCallHandler() to assignment,
            )

        for ((handler, foreignNode) in cases) {
            assertFailsWith<IllegalStateException> { handler.handle(foreignNode, formatter) }
        }
    }
}
