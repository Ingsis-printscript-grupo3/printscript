package printscript.linter

import printscript.common.Position
import kotlin.test.Test
import kotlin.test.assertEquals

class ReadInputIntegrationTest {
    private val readInputOn = mapOf("mandatory-variable-or-literal-in-readInput" to "true")

    private fun lint(
        source: String,
        config: Map<String, String>,
    ): List<Warning> = warningsOf(LinterFactory.create(config = config), source)

    @Test
    fun `finds readInput inside an assignment`() {
        val source =
            """
            let name: string = "x";
            name = readInput("a" + "b");
            """.trimIndent()

        val warnings = lint(source, readInputOn)

        assertEquals(1, warnings.size)
        assertEquals(
            "readInput can only be called with an identifier or a literal, not an expression",
            warnings[0].message,
        )
        assertEquals(Position(2, 8), warnings[0].position)
    }

    @Test
    fun `finds readInput inside a println`() {
        val source = """println(readInput("a" + "b"));"""
        val config =
            mapOf(
                "mandatory-variable-or-literal-in-println" to "false",
                "mandatory-variable-or-literal-in-readInput" to "true",
            )

        val warnings = lint(source, config)

        assertEquals(1, warnings.size)
        assertEquals(
            "readInput can only be called with an identifier or a literal, not an expression",
            warnings[0].message,
        )
    }

    @Test
    fun `finds readInput inside an if condition`() {
        val source =
            """
            if (readInput("a" + "b")) {
                println("ok");
            }
            """.trimIndent()

        val warnings = lint(source, readInputOn)

        assertEquals(1, warnings.size)
        assertEquals(
            "readInput can only be called with an identifier or a literal, not an expression",
            warnings[0].message,
        )
        assertEquals(Position(1, 5), warnings[0].position)
    }

    @Test
    fun `finds readInput nested inside a binary expression`() {
        val source = """let total: string = "prefix: " + readInput("a" + "b");"""
        val warnings = lint(source, readInputOn)

        assertEquals(1, warnings.size)
        assertEquals(
            "readInput can only be called with an identifier or a literal, not an expression",
            warnings[0].message,
        )
    }

    @Test
    fun `finds a readInput nested inside the argument of another readInput`() {
        val source = """let name: string = readInput(readInput("a" + "b"));"""
        val warnings = lint(source, readInputOn)

        assertEquals(2, warnings.size)
        assertEquals(
            listOf(
                "readInput can only be called with an identifier or a literal, not an expression",
                "readInput can only be called with an identifier or a literal, not an expression",
            ),
            warnings.map { it.message },
        )
    }

    @Test
    fun `readInput nested inside another readInput with literal argument only warns once for the outer call`() {
        val source = """let name: string = readInput(readInput("a"));"""
        val warnings = lint(source, readInputOn)

        assertEquals(1, warnings.size)
        assertEquals(
            "readInput can only be called with an identifier or a literal, not an expression",
            warnings[0].message,
        )
    }

    @Test
    fun `readInput with literal or identifier argument produces no warnings`() {
        val source =
            """
            let prompt: string = "ingrese su nombre:";
            let name: string = readInput(prompt);
            let age: string = readInput("ingrese su edad:");
            """.trimIndent()

        val warnings = lint(source, readInputOn)

        assertEquals(0, warnings.size)
    }
}
