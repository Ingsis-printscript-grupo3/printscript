package printscript.interpreter.plugin

import printscript.ast.Expression
import printscript.ast.Statement
import printscript.interpreter.output.BucketOutput
import kotlin.test.Test
import kotlin.test.assertEquals

class CompletenessTest {
    @Test
    fun `every Statement subtype has a registered StatementInterpreter`() {
        val registered = DefaultStatementInterpreters.list(BucketOutput()).map { it.type.kotlin }.toSet()
        val subtypes = Statement::class.sealedSubclasses.toSet()

        assertEquals(subtypes, registered, "Missing a registered StatementInterpreter for some Statement subtype")
    }

    @Test
    fun `every Expression subtype has a registered ExpressionEvaluator`() {
        val registered = DefaultExpressionEvaluators.list.map { it.type.kotlin }.toSet()
        val subtypes = Expression::class.sealedSubclasses.toSet()

        assertEquals(subtypes, registered, "Missing a registered ExpressionEvaluator for some Expression subtype")
    }
}
