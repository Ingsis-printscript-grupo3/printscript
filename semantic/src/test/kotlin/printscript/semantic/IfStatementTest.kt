package printscript.semantic

import printscript.common.LanguageVersion
import printscript.common.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class IfStatementTest {
    @Test
    fun `an if with a boolean literal condition succeeds`() {
        val code = "if (true) { println(\"inside then\"); }"

        val results = analyze(code, LanguageVersion.V1_1)

        assertTrue(results.all { it is SemanticResult.Success })
    }

    @Test
    fun `an if with else and a boolean variable condition succeeds`() {
        val code =
            """
            let flag: boolean = true;
            if (flag) {
                println("inside");
            } else {
                println("else");
            }
            """.trimIndent()

        val results = analyze(code, LanguageVersion.V1_1)

        assertEquals(2, results.size)
        assertTrue(results.all { it is SemanticResult.Success })
    }

    // el error apunta a la condicion, no al if que la contiene
    @Test
    fun `a number condition fails at the condition and stops the analysis`() {
        val code =
            """
            let num: number = 5;
            if (num) { println("x"); }
            println("should not reach here");
            """.trimIndent()

        val results = analyze(code, LanguageVersion.V1_1)

        assertEquals(2, results.size)
        val failure = assertIs<SemanticResult.Failure>(results.last())
        assertEquals("'if' condition must be a boolean expression, found 'number'.", failure.message)
        assertEquals(Position(2, 5), failure.position)
    }

    @Test
    fun `a string condition fails`() {
        val code = "if (\"hello\") { println(\"x\"); }"

        val results = analyze(code, LanguageVersion.V1_1)

        val failure = assertIs<SemanticResult.Failure>(results.single())
        assertEquals("'if' condition must be a boolean expression, found 'string'.", failure.message)
    }

    @Test
    fun `an undeclared variable in the condition propagates the failure`() {
        val code = "if (missing) { println(\"x\"); }"

        val results = analyze(code, LanguageVersion.V1_1)

        val failure = assertIs<SemanticResult.Failure>(results.single())
        assertEquals("Variable 'missing' not declared.", failure.message)
    }

    // el error de un statement dentro del bloque conserva su posicion, no la del if
    @Test
    fun `an error inside the block keeps its own position`() {
        val code =
            """
            if (true) {
                let y: number = "err";
            }
            """.trimIndent()

        val results = analyze(code, LanguageVersion.V1_1)

        val failure = assertIs<SemanticResult.Failure>(results.single())
        assertEquals(Position(2, 5), failure.position)
    }
}
