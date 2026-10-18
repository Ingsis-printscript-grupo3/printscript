package printscript.semantic

import printscript.common.LanguageVersion
import printscript.common.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class VariablesTest {
    @Test
    fun `declaring a variable without an initializer succeeds`() {
        val code = "let a: number;"

        val results = analyze(code)

        assertTrue(results.all { it is SemanticResult.Success })
    }

    @Test
    fun `declaring a variable with a matching initializer succeeds`() {
        val code = "let a: number = 1;"

        val results = analyze(code)

        assertTrue(results.all { it is SemanticResult.Success })
    }

    @Test
    fun `declaring a variable with a mismatched initializer fails`() {
        val code = "let a: number = \"x\";"

        val results = analyze(code)

        val failure = assertIs<SemanticResult.Failure>(results.single())
        assertEquals("Incompatible types.", failure.message)
    }

    @Test
    fun `a boolean variable can be declared and assigned in 1_1`() {
        val code =
            """
            let b: boolean = true;
            b = false;
            """.trimIndent()

        val results = analyze(code, LanguageVersion.V1_1)

        assertEquals(2, results.size)
        assertTrue(results.all { it is SemanticResult.Success })
    }

    @Test
    fun `a boolean variable cannot be initialized with a number in 1_1`() {
        val code = "let b: boolean = 123;"

        val results = analyze(code, LanguageVersion.V1_1)

        val failure = assertIs<SemanticResult.Failure>(results.single())
        assertEquals("Incompatible types.", failure.message)
    }

    @Test
    fun `declaring the same variable twice fails`() {
        val code =
            """
            let a: number;
            let a: number;
            """.trimIndent()

        val results = analyze(code)

        val failure = assertIs<SemanticResult.Failure>(results.last())
        assertEquals("Variable 'a' already exists.", failure.message)
    }

    // el error nace en la tabla de simbolos y apunta al nombre repetido, no al let
    @Test
    fun `an error born in the symbol table carries the position of the name`() {
        val code =
            """
            let a: number = 1;
            let a: number = 2;
            """.trimIndent()

        val results = analyze(code)

        val failure = assertIs<SemanticResult.Failure>(results.last())
        assertEquals(Position(2, 5), failure.position)
    }

    @Test
    fun `assigning a compatible value to a declared variable succeeds`() {
        val code =
            """
            let a: number;
            a = 2;
            """.trimIndent()

        val results = analyze(code)

        assertEquals(2, results.size)
        assertTrue(results.all { it is SemanticResult.Success })
    }

    @Test
    fun `assigning to an undeclared variable fails`() {
        val code = "a = 2;"

        val results = analyze(code)

        val failure = assertIs<SemanticResult.Failure>(results.single())
        assertEquals("Variable 'a' not declared.", failure.message)
    }

    @Test
    fun `assigning an incompatible type fails`() {
        val code =
            """
            let a: number;
            a = "x";
            """.trimIndent()

        val results = analyze(code)

        val failure = assertIs<SemanticResult.Failure>(results.last())
        assertEquals("Incompatible types in assignment.", failure.message)
    }
}
