package printscript.semantic

import printscript.ast.Assignment
import printscript.ast.NumberLiteral
import printscript.ast.PrintCall
import printscript.ast.StringLiteral
import printscript.ast.VariableDeclaration
import printscript.common.LanguageVersion
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

        val results = SemanticAnalyzer(LanguageVersion.V1_1).analyze(statements.iterator()).asSequence().toList()

        assertEquals(2, results.size)
        results.forEach { assertIs<SemanticResult.Success<*>>(it) }
    }

    @Test
    fun `stops at the first failure and reports the statement position`() {
        val badStatement = VariableDeclaration("x", "number", StringLiteral("oops"), Position(7, 3))
        val statements = listOf(badStatement, PrintCall(NumberLiteral(1.0)))

        val results = SemanticAnalyzer(LanguageVersion.V1_1).analyze(statements.iterator()).asSequence().toList()

        assertEquals(1, results.size)
        val failure = results.single()
        assertIs<SemanticResult.Failure>(failure)
        assertEquals(Position(7, 3), failure.position)
    }

    @Test
    fun `an empty program yields no results`() {
        val empty = emptyList<printscript.ast.Statement>().iterator()
        val results = SemanticAnalyzer(LanguageVersion.V1_1).analyze(empty).asSequence().toList()

        assertTrue(results.isEmpty())
    }

    @Test
    fun `semantic analyzer accepts const declaration with initializer in 1_1`() {
        val statements =
            listOf(
                VariableDeclaration("x", "number", NumberLiteral(10.0), isConst = true),
                PrintCall(printscript.ast.Identifier("x")),
            )

        val results = SemanticAnalyzer(LanguageVersion.V1_1).analyze(statements.iterator()).asSequence().toList()

        assertEquals(2, results.size)
        results.forEach { assertIs<SemanticResult.Success<*>>(it) }
    }

    @Test
    fun `semantic analyzer fails when reassigning a const variable and reports statement position`() {
        val statements =
            listOf(
                VariableDeclaration("x", "number", NumberLiteral(10.0), isConst = true),
                Assignment("x", NumberLiteral(20.0), Position(2, 1)),
            )

        val results = SemanticAnalyzer(LanguageVersion.V1_1).analyze(statements.iterator()).asSequence().toList()

        assertEquals(2, results.size)
        assertIs<SemanticResult.Success<*>>(results[0])
        val failure = results[1]
        assertIs<SemanticResult.Failure>(failure)
        assertEquals(Position(2, 1), failure.position)
        assertTrue(failure.message.contains("Cannot reassign constant 'x'"))
    }

    @Test
    fun `semantic analyzer rejects const declaration in 1_0`() {
        val statements =
            listOf(
                VariableDeclaration("x", "number", NumberLiteral(10.0), Position(1, 1), isConst = true),
            )

        val results = SemanticAnalyzer(LanguageVersion.V1_0).analyze(statements.iterator()).asSequence().toList()

        assertEquals(1, results.size)
        val failure = results.single()
        assertIs<SemanticResult.Failure>(failure)
        assertEquals(Position(1, 1), failure.position)
        assertTrue(failure.message.contains("not supported in PrintScript 1.0"))
    }
}
