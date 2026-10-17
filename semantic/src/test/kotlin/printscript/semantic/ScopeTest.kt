package printscript.semantic

import printscript.common.LanguageVersion
import printscript.common.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ScopeTest {
    // el error apunta al identificador, no al println que lo contiene
    @Test
    fun `a variable declared in the then branch is not visible after the if`() {
        val code =
            """
            if (true) {
                let scopedVar: number = 42;
            }
            println(scopedVar);
            """.trimIndent()

        val results = analyze(code, LanguageVersion.V1_1)

        assertIs<SemanticResult.Success<*>>(results.first())
        val failure = assertIs<SemanticResult.Failure>(results.last())
        assertEquals("Variable 'scopedVar' not declared.", failure.message)
        assertEquals(Position(4, 9), failure.position)
    }

    @Test
    fun `a variable declared in the else branch is not visible after the if`() {
        val code =
            """
            if (false) {
                println("then");
            } else {
                let elseVar: string = "hi";
            }
            elseVar = "bye";
            """.trimIndent()

        val results = analyze(code, LanguageVersion.V1_1)

        val failure = assertIs<SemanticResult.Failure>(results.last())
        assertEquals("Variable 'elseVar' not declared.", failure.message)
    }

    @Test
    fun `a variable declared in the then branch is not visible in the else branch`() {
        val code =
            """
            if (true) {
                let x: number = 1;
            } else {
                println(x);
            }
            """.trimIndent()

        val results = analyze(code, LanguageVersion.V1_1)

        val failure = assertIs<SemanticResult.Failure>(results.single())
        assertEquals("Variable 'x' not declared.", failure.message)
    }

    @Test
    fun `then and else branches can declare the same name with different types`() {
        val code =
            """
            if (true) {
                let x: number = 1;
            } else {
                let x: string = "msg";
            }
            """.trimIndent()

        val results = analyze(code, LanguageVersion.V1_1)

        assertTrue(results.all { it is SemanticResult.Success })
    }

    @Test
    fun `shadowing inside an if does not change the type of the outer variable`() {
        val code =
            """
            let x: number = 10;
            if (true) {
                let x: string = "shadowed";
                println(x);
            }
            x = 20;
            x = "fail";
            """.trimIndent()

        val results = analyze(code, LanguageVersion.V1_1)

        assertEquals(4, results.size)
        assertTrue(results.dropLast(1).all { it is SemanticResult.Success })
        assertIs<SemanticResult.Failure>(results.last())
    }

    @Test
    fun `nested ifs discard the variables of every level when they end`() {
        val code =
            """
            if (true) {
                let outerVar: number = 1;
                if (true) {
                    let innerVar: string = "deep";
                }
            }
            outerVar = 2;
            """.trimIndent()

        val results = analyze(code, LanguageVersion.V1_1)

        assertIs<SemanticResult.Success<*>>(results.first())
        val failure = assertIs<SemanticResult.Failure>(results.last())
        assertEquals("Variable 'outerVar' not declared.", failure.message)
    }
}
