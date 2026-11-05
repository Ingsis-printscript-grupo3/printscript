package printscript.runner.version

import printscript.ast.Statement
import printscript.common.Position
import printscript.semantic.SemanticAnalyzer
import printscript.semantic.SemanticResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SemanticVersionTest {
    private fun analyze(
        definition: LanguageDefinition,
        code: String,
    ): List<SemanticResult<Statement>> =
        SemanticAnalyzer(definition.semanticStatements, definition.semanticExpressions)
            .analyze(parse11(code))
            .asSequence()
            .toList()

    @Test
    fun `1_1 keeps every semantic handler of 1_0`() {
        val statements11 = PrintScript11.semanticStatements.map { it::class }
        val expressions11 = PrintScript11.semanticExpressions.map { it::class }

        assertTrue(statements11.containsAll(PrintScript10.semanticStatements.map { it::class }))
        assertTrue(expressions11.containsAll(PrintScript10.semanticExpressions.map { it::class }))
    }

    @Test
    fun `1_0 rejects an if statement`() {
        val results = analyze(PrintScript10, "if (true) { println(1); }")

        val failure = assertIs<SemanticResult.Failure>(results.single())
        assertEquals("Unknown statement type.", failure.message)
        assertEquals(Position(1, 1), failure.position)
    }

    @Test
    fun `1_0 rejects readInput`() {
        val results = analyze(PrintScript10, "let x: string = readInput(\"prompt:\");")

        val failure = assertIs<SemanticResult.Failure>(results.single())
        assertEquals("Unknown expression type.", failure.message)
        assertEquals(Position(1, 17), failure.position)
    }

    @Test
    fun `1_0 rejects readEnv`() {
        val results = analyze(PrintScript10, "let x: string = readEnv(\"HOME\");")

        val failure = assertIs<SemanticResult.Failure>(results.single())
        assertEquals("Unknown expression type.", failure.message)
        assertEquals(Position(1, 17), failure.position)
    }

    @Test
    fun `1_0 rejects boolean literals`() {
        val results = analyze(PrintScript10, "println(true);")

        val failure = assertIs<SemanticResult.Failure>(results.single())
        assertEquals("Unknown expression type.", failure.message)
        assertEquals(Position(1, 9), failure.position)
    }

    @Test
    fun `1_1 accepts everything 1_0 rejects`() {
        val code =
            """
            const flag: boolean = true;
            if (flag) {
                let name: string = readInput("name:");
            } else {
                let home: string = readEnv("HOME");
            }
            """.trimIndent()

        val results = analyze(PrintScript11, code)

        assertEquals(2, results.size)
        assertTrue(results.all { it is SemanticResult.Success })
    }
}
