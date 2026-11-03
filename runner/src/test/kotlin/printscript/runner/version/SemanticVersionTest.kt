package printscript.runner.version

import printscript.ast.Statement
import printscript.common.LanguageVersion
import printscript.common.Position
import printscript.lexer.LexerFactory
import printscript.parser.ParserFactory
import printscript.parser.result.ParseResult
import printscript.semantic.SemanticAnalyzer
import printscript.semantic.SemanticResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

// el codigo se parsea con 1.1 para que el semantico de 1.0 reciba nodos que no soporta
class SemanticVersionTest {
    private fun parse11(code: String): Iterator<Statement> {
        val tokens = LexerFactory.create(code, LanguageVersion.V1_1).tokenize()
        return ParserFactory.create(tokens, LanguageVersion.V1_1).parse().asSequence().map { result ->
            assertIs<ParseResult.Success>(result).statement
        }.iterator()
    }

    private fun analyze(
        definition: LanguageDefinition,
        code: String,
    ): List<SemanticResult<Statement>> =
        SemanticAnalyzer(definition.semanticStatements, definition.semanticExpressions)
            .analyze(parse11(code))
            .asSequence()
            .toList()

    @Test
    fun `each version maps to its own definition`() {
        assertSame(PrintScript10, LanguageVersion.V1_0.definition())
        assertSame(PrintScript11, LanguageVersion.V1_1.definition())
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
