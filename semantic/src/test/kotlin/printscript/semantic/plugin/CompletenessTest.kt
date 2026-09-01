package printscript.semantic.plugin

import printscript.ast.Expression
import printscript.ast.Statement
import kotlin.test.Test
import kotlin.test.assertEquals

// test safety net: if a Statement or Expression subtype is added and its handler
// isn't registered in DefaultStatementHandlers / DefaultExpressionHandlers, this test
// fails (in addition to the compile-time witness in StatementValidator/ExpressionResolver).
class CompletenessTest {
    @Test
    fun `every Statement subtype has a registered StatementHandler`() {
        val registered = DefaultStatementHandlers.list.map { it.type.kotlin }.toSet()
        val subtypes = Statement::class.sealedSubclasses.toSet()

        assertEquals(subtypes, registered, "Missing a registered StatementHandler for some Statement subtype")
    }

    @Test
    fun `every Expression subtype has a registered ExpressionHandler`() {
        val registered = DefaultExpressionHandlers.list.map { it.type.kotlin }.toSet()
        val subtypes = Expression::class.sealedSubclasses.toSet()

        assertEquals(subtypes, registered, "Missing a registered ExpressionHandler for some Expression subtype")
    }
}
