package printscript.semantic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class OperationsTest {
    @Test
    fun `adding two numbers is a number`() {
        val code = "let n: number = 1 + 2;"

        val results = analyze(code)

        assertTrue(results.all { it is SemanticResult.Success })
    }

    @Test
    fun `adding a string on either side is a string`() {
        val code =
            """
            let a: string = 1 + "x";
            let b: string = "x" + 1;
            """.trimIndent()

        val results = analyze(code)

        assertEquals(2, results.size)
        assertTrue(results.all { it is SemanticResult.Success })
    }

    @Test
    fun `a sum with a string cannot be stored in a number`() {
        val code = "let n: number = 1 + \"x\";"

        val results = analyze(code)

        val failure = assertIs<SemanticResult.Failure>(results.single())
        assertEquals("Incompatible types.", failure.message)
    }

    @Test
    fun `subtracting multiplying and dividing numbers is a number`() {
        val code =
            """
            let a: number = 5 - 2;
            let b: number = 5 * 2;
            let c: number = 5 / 2;
            """.trimIndent()

        val results = analyze(code)

        assertEquals(3, results.size)
        assertTrue(results.all { it is SemanticResult.Success })
    }

    @Test
    fun `subtracting a string fails`() {
        val code = "let n: number = \"x\" - 2;"

        val results = analyze(code)

        val failure = assertIs<SemanticResult.Failure>(results.single())
        assertEquals("Incompatible types in operation.", failure.message)
    }

    @Test
    fun `an undeclared operand fails the operation`() {
        val code = "let n: number = missing + 2;"

        val results = analyze(code)

        val failure = assertIs<SemanticResult.Failure>(results.single())
        assertEquals("Variable 'missing' not declared.", failure.message)
    }

    @Test
    fun `a variable has the type it was declared with`() {
        val code =
            """
            let a: string = "x";
            let b: string = a;
            let c: number = a;
            """.trimIndent()

        val results = analyze(code)

        assertEquals(3, results.size)
        val failure = assertIs<SemanticResult.Failure>(results.last())
        assertEquals("Incompatible types.", failure.message)
    }

    @Test
    fun `no arithmetic operation accepts booleans`() {
        val operators = mapOf("+" to "PLUS", "-" to "MINUS", "*" to "MULTIPLY", "/" to "DIVIDE")

        for ((symbol, name) in operators) {
            val results = analyze("println(true $symbol false);")

            val failure = assertIs<SemanticResult.Failure>(results.single())
            assertEquals("Operator '$name' cannot be applied to boolean types.", failure.message)
        }
    }

    @Test
    fun `a boolean cannot be added to a number or a string`() {
        for (code in listOf("println(true + 1);", "println(\"hello\" + true);")) {
            val results = analyze(code)

            val failure = assertIs<SemanticResult.Failure>(results.single())
            assertEquals("Operator 'PLUS' cannot be applied to boolean types.", failure.message)
        }
    }

    @Test
    fun `negating a number or a number variable is a number`() {
        val code =
            """
            let x: number = 1;
            let a: number = -5;
            let b: number = -x;
            """.trimIndent()

        val results = analyze(code)

        assertEquals(3, results.size)
        assertTrue(results.all { it is SemanticResult.Success })
    }

    @Test
    fun `negating a string fails`() {
        val code = "let n: number = -\"hello\";"

        val results = analyze(code)

        val failure = assertIs<SemanticResult.Failure>(results.single())
        assertEquals("Incompatible types in operation.", failure.message)
    }

    @Test
    fun `negating a boolean fails`() {
        val code = "println(-true);"

        val results = analyze(code)

        val failure = assertIs<SemanticResult.Failure>(results.single())
        assertEquals("Operator 'MINUS' cannot be applied to boolean types.", failure.message)
    }
}
