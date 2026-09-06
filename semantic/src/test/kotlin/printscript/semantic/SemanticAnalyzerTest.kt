package printscript.semantic

import printscript.ast.NumberLiteral
import printscript.ast.PrintCall
import printscript.ast.StringLiteral
import printscript.ast.VariableDeclaration
import printscript.common.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SemanticAnalyzerTest {
    @Test
    fun `yields a success per well typed statement`() {
        val statements =
            listOf(
                VariableDeclaration("x", "number", NumberLiteral(1.0)),
                PrintCall(printscript.ast.Identifier("x")),
            )

        val results = SemanticAnalyzer().analyze(statements.iterator()).asSequence().toList()

        assertEquals(2, results.size)
        results.forEach { assertIs<SemanticResult.Success<*>>(it) }
    }

    @Test
    fun `stops at the first failure and reports the statement position`() {
        val badStatement = VariableDeclaration("x", "number", StringLiteral("oops"), Position(7, 3))
        val statements = listOf(badStatement, PrintCall(NumberLiteral(1.0)))

        val results = SemanticAnalyzer().analyze(statements.iterator()).asSequence().toList()

        assertEquals(1, results.size)
        val failure = results.single()
        assertIs<SemanticResult.Failure>(failure)
        assertEquals(Position(7, 3), failure.position)
    }

    @Test
    fun `an empty program yields no results`() {
        val empty = emptyList<printscript.ast.Statement>().iterator()
        val results = SemanticAnalyzer().analyze(empty).asSequence().toList()

        assertTrue(results.isEmpty())
    }
}
