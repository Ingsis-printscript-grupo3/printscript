package printscript.semantic

import printscript.common.LanguageVersion
import printscript.common.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ReadInputTest {
    @Test
    fun `readInput in a declaration takes the declared type`() {
        val code =
            """
            let n: number = readInput("prompt:");
            let s: string = readInput("prompt:");
            let b: boolean = readInput("prompt:");
            """.trimIndent()

        val results = analyze(code, LanguageVersion.V1_1)

        assertEquals(3, results.size)
        assertTrue(results.all { it is SemanticResult.Success })
    }

    @Test
    fun `a const can be initialized with readInput`() {
        val code = "const c: number = readInput(\"num:\");"

        val results = analyze(code, LanguageVersion.V1_1)

        assertTrue(results.all { it is SemanticResult.Success })
    }

    @Test
    fun `readInput in an assignment takes the type of the variable`() {
        val code =
            """
            let n: number;
            let s: string;
            let b: boolean;
            n = readInput("prompt:");
            s = readInput("prompt:");
            b = readInput("prompt:");
            """.trimIndent()

        val results = analyze(code, LanguageVersion.V1_1)

        assertEquals(6, results.size)
        assertTrue(results.all { it is SemanticResult.Success })
    }

    @Test
    fun `printing readInput defaults to string`() {
        val code = "println(readInput(\"Enter name:\"));"

        val results = analyze(code, LanguageVersion.V1_1)

        assertTrue(results.all { it is SemanticResult.Success })
    }

    // el error apunta al argumento y corta el analisis
    @Test
    fun `a non string argument in a declaration fails at the argument`() {
        val code =
            """
            let x: string = readInput(123);
            println("unreachable");
            """.trimIndent()

        val results = analyze(code, LanguageVersion.V1_1)

        val failure = assertIs<SemanticResult.Failure>(results.single())
        assertEquals("'readInput' argument must be a string, found 'number'.", failure.message)
        assertEquals(Position(1, 27), failure.position)
    }

    @Test
    fun `a non string argument in an assignment fails`() {
        val code =
            """
            let x: string;
            x = readInput(false);
            """.trimIndent()

        val results = analyze(code, LanguageVersion.V1_1)

        val failure = assertIs<SemanticResult.Failure>(results.last())
        assertEquals("'readInput' argument must be a string, found 'boolean'.", failure.message)
    }

    @Test
    fun `a non string argument in a println fails`() {
        val code = "println(readInput(99));"

        val results = analyze(code, LanguageVersion.V1_1)

        val failure = assertIs<SemanticResult.Failure>(results.single())
        assertEquals("'readInput' argument must be a string, found 'number'.", failure.message)
    }

    @Test
    fun `readInput added to a string is a string`() {
        val code = "let fullName: string = readInput(\"First name: \") + \"Doe\";"

        val results = analyze(code, LanguageVersion.V1_1)

        assertTrue(results.all { it is SemanticResult.Success })
    }

    // dentro de una suma readInput no toma el tipo de la variable: es string, igual que en el interprete
    @Test
    fun `readInput added to a number cannot be stored in a number variable`() {
        val code = "let n: number = readInput(\"A: \") + 5;"

        val results = analyze(code, LanguageVersion.V1_1)

        val failure = assertIs<SemanticResult.Failure>(results.single())
        assertEquals("Incompatible types.", failure.message)
    }
}
