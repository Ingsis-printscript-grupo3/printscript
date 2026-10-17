package printscript.semantic

import printscript.common.LanguageVersion
import printscript.common.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

// el codigo se parsea con 1.1 para que el analizador 1.0 reciba nodos que no soporta
class VersionSupportTest {
    @Test
    fun `the 1_0 analyzer rejects an if statement`() {
        val code = "if (true) { println(1); }"
        val statements = parse(code, LanguageVersion.V1_1)

        val results = SemanticAnalyzer(LanguageVersion.V1_0).analyze(statements).asSequence().toList()

        val failure = assertIs<SemanticResult.Failure>(results.single())
        assertEquals("Unknown statement type.", failure.message)
        assertEquals(Position(1, 1), failure.position)
    }

    @Test
    fun `the 1_0 analyzer rejects readInput`() {
        val code = "let x: string = readInput(\"prompt:\");"
        val statements = parse(code, LanguageVersion.V1_1)

        val results = SemanticAnalyzer(LanguageVersion.V1_0).analyze(statements).asSequence().toList()

        val failure = assertIs<SemanticResult.Failure>(results.single())
        assertEquals("Unknown expression type.", failure.message)
        assertEquals(Position(1, 17), failure.position)
    }
}
