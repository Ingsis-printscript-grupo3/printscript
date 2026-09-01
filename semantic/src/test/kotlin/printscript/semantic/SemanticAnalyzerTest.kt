package printscript.semantic

import printscript.ast.Assignment
import printscript.ast.NumberLiteral
import printscript.ast.PrintCall
import printscript.ast.Statement
import printscript.ast.StringLiteral
import printscript.ast.VariableDeclaration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SemanticAnalyzerTest {
    @Test
    fun `a valid program yields a Success for every statement`() {
        val statements =
            listOf(
                VariableDeclaration("x", "number", NumberLiteral(1.0)),
                PrintCall(NumberLiteral(1.0)),
            )

        val results = SemanticAnalyzer().analyze(statements.iterator()).asSequence().toList()

        assertEquals(2, results.size)
        results.forEach { assertIs<SemanticResult.Success<Statement>>(it) }
    }

    @Test
    fun `analysis stops at the first semantic failure`() {
        val statements =
            listOf(
                VariableDeclaration("x", "number", NumberLiteral(1.0)),
                Assignment("x", StringLiteral("oops")),
                PrintCall(NumberLiteral(1.0)),
            )

        val results = SemanticAnalyzer().analyze(statements.iterator()).asSequence().toList()

        assertEquals(2, results.size)
        assertIs<SemanticResult.Success<Statement>>(results[0])
        assertIs<SemanticResult.Failure>(results[1])
        assertTrue(results.size < statements.size, "should not analyze the statement after the error")
    }
}
