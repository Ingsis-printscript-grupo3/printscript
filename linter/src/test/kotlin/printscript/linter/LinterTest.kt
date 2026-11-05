package printscript.linter

import printscript.common.Position
import printscript.linter.rule.LinterRuleRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LinterTest {
    private fun analyze(
        source: String,
        config: Map<String, String> = LinterRuleRegistry().defaultConfig(),
    ): List<Warning> = warningsOf(LinterFactory.create(config = config), source)

    private val camelCase = mapOf("identifier_format" to "camel case")

    private val snakeCase = mapOf("identifier_format" to "snake case")

    @Test
    fun `camel case variable declaration is valid`() {
        val warnings = analyze("let myVar: number;", camelCase)
        assertEquals(0, warnings.size)
    }

    @Test
    fun `snake case variable declaration warns when camel case is expected`() {
        val warnings = analyze("let my_var: number;", camelCase)
        assertEquals(1, warnings.size)
        assertEquals("Identifier 'my_var' does not match format camel case", warnings[0].message)
    }

    @Test
    fun `snake case variable declaration is valid when snake case is expected`() {
        val warnings = analyze("let my_var: number;", snakeCase)
        assertEquals(0, warnings.size)
    }

    @Test
    fun `camel case variable declaration warns when snake case is expected`() {
        val warnings = analyze("let myVar: number;", snakeCase)
        assertEquals(1, warnings.size)
        assertEquals("Identifier 'myVar' does not match format snake case", warnings[0].message)
    }

    @Test
    fun `println with literal is valid`() {
        val warnings = analyze("println(5);")
        assertEquals(0, warnings.size)
    }

    @Test
    fun `println with identifier is valid`() {
        val warnings = analyze("println(x);")
        assertEquals(0, warnings.size)
    }

    @Test
    fun `println with binary expression warns when rule is on`() {
        val warnings = analyze("println(1 + 2);")
        assertEquals(1, warnings.size)
        assertEquals(
            "println can only be called with an identifier or a literal, not an expression",
            warnings[0].message,
        )
    }

    @Test
    fun `println with binary expression is valid when rule is off`() {
        val warnings = analyze("println(1 + 2);", mapOf("mandatory-variable-or-literal-in-println" to "false"))
        assertEquals(0, warnings.size)
    }

    @Test
    fun `the warning points at the position of the statement that caused it`() {
        val warnings = analyze("\n\n      println(1 + 2);")

        assertEquals(Position(3, 7), warnings[0].position)
    }

    // el warning del identificador va al nombre, no al let que lo declara
    @Test
    fun `the identifier warning points at the name and not at the let`() {
        val warnings = analyze("\n\nlet my_var: number;")

        assertEquals(Position(3, 5), warnings[0].position)
    }

    // sin config el linter chequea todo, que es lo que usa el CLI cuando no le pasan --config
    @Test
    fun `no config turns every rule on`() {
        // el argumento del readInput tiene que ser invalido, si no la tercera regla
        // no dispara y el test pasaria aunque estuviera apagada
        val source =
            """
            let my_var: number;
            println(1 + 2);
            let dato: string = readInput("a" + "b");
            """.trimIndent()

        assertEquals(3, analyze(source).size)
    }

    // una config que no nombra ninguna regla no chequea nada
    @Test
    fun `a config that names no rule emits no warnings`() {
        val source =
            """
            let my_var: number;
            println(1 + 2);
            """.trimIndent()
        val config = emptyMap<String, String>()

        assertEquals(0, analyze(source, config).size)
    }

    @Test
    fun `keeps going after the first warning and returns every one of them`() {
        val source =
            """
            let my_var: number;
            let other_var: number;
            println(1 + 2);
            """.trimIndent()

        val warnings = analyze(source)

        assertEquals(3, warnings.size)
        assertEquals(listOf(Position(1, 5), Position(2, 5), Position(3, 1)), warnings.map { it.position })
    }

    @Test
    fun `snake case assignment warns when camel case is expected`() {
        val warnings = analyze("mi_variable = 3;", camelCase)

        assertEquals(1, warnings.size)
        assertEquals("Identifier 'mi_variable' does not match format camel case", warnings[0].message)
    }

    @Test
    fun `camel case assignment warns when snake case is expected`() {
        val warnings = analyze("miVariable = 3;", snakeCase)

        assertEquals(1, warnings.size)
        assertEquals("Identifier 'miVariable' does not match format snake case", warnings[0].message)
    }

    @Test
    fun `a well formatted assignment is valid`() {
        assertEquals(0, analyze("miVariable = 3;", camelCase).size)
    }

    @Test
    fun `the assignment warning points at the real row and column`() {
        val warnings = analyze("\n\n\n\n  mi_variable = 3;")

        assertEquals(Position(5, 3), warnings[0].position)
    }

    @Test
    fun `a const declaration with a bad name warns just like a let`() {
        val warnings = analyze("\nconst mi_constante: number = 3;", camelCase)

        assertEquals(1, warnings.size)
        assertEquals("Identifier 'mi_constante' does not match format camel case", warnings[0].message)
        assertEquals(Position(2, 7), warnings[0].position)
    }

    @Test
    fun `a well formatted const declaration is valid`() {
        assertEquals(0, analyze("const miConstante: number = 3;", camelCase).size)
    }

    @Test
    fun `identifiers with digits are valid in camel case`() {
        val source =
            """
            let dato1: number;
            valor2 = 1;
            """.trimIndent()

        assertEquals(0, analyze(source, camelCase).size)
    }

    @Test
    fun `identifiers with digits are valid in snake case`() {
        val source =
            """
            let dato1: number;
            mi_valor2 = 1;
            """.trimIndent()

        assertEquals(0, analyze(source, snakeCase).size)
    }

    @Test
    fun `a badly formatted declaration inside an if block warns`() {
        val source =
            """
            if (x) {
                let mi_variable: number = 1;
            }
            """.trimIndent()

        val warnings = analyze(source, camelCase)

        assertEquals(1, warnings.size)
        assertEquals("Identifier 'mi_variable' does not match format camel case", warnings[0].message)
        assertEquals(Position(2, 9), warnings[0].position)
    }

    @Test
    fun `a badly formatted const inside an if block warns`() {
        val source =
            """
            if (x) {
                const mi_constante: number = 1;
            }
            """.trimIndent()

        assertEquals(1, analyze(source, camelCase).size)
    }

    @Test
    fun `a badly formatted assignment inside an if block warns`() {
        val source =
            """
            if (x) {
                mi_variable = 2;
            }
            """.trimIndent()

        val warnings = analyze(source, camelCase)

        assertEquals(1, warnings.size)
        assertEquals(Position(2, 5), warnings[0].position)
    }

    @Test
    fun `a badly formatted assignment inside an else block warns`() {
        val source =
            """
            if (x) {
            } else {
                mi_variable = 2;
            }
            """.trimIndent()

        val warnings = analyze(source, camelCase)

        assertEquals(1, warnings.size)
        assertEquals(Position(3, 5), warnings[0].position)
    }

    @Test
    fun `a readInput with an expression inside an if block warns`() {
        val source =
            """
            if (x) {
                let dato: string = readInput("a" + "b");
            }
            """.trimIndent()

        val warnings = analyze(source)

        assertEquals(1, warnings.size)
        assertEquals(
            "readInput can only be called with an identifier or a literal, not an expression",
            warnings[0].message,
        )
        assertEquals(Position(2, 24), warnings[0].position)
    }

    @Test
    fun `warns on a declaration nested two if blocks deep`() {
        val source =
            """
            if (x) {
                if (x) {
                    let mi_variable: number = 1;
                }
            }
            """.trimIndent()

        val warnings = analyze(source, camelCase)

        assertEquals(1, warnings.size)
        assertEquals(Position(3, 13), warnings[0].position)
    }

    @Test
    fun `a well formatted if block produces no warnings`() {
        val source =
            """
            if (x) {
                let miVariable: number = 1;
                miVariable = 2;
            } else {
                println(miVariable);
            }
            """.trimIndent()

        assertEquals(0, analyze(source).size)
    }

    // el recorrido visita la condicion del if una sola vez: el readInput no debe contarse dos veces
    @Test
    fun `a readInput in the if condition warns exactly once`() {
        val source =
            """
            if (readInput("a" + "b")) {
            }
            """.trimIndent()

        assertEquals(1, analyze(source).size)
    }

    @Test
    fun `warnings in if statement are reported in pre-order traversal sequence`() {
        val source =
            """
            if (readInput("a" + "b")) {
                bad_then = 1;
            } else {
                bad_else = 2;
            }
            """.trimIndent()

        val warnings = analyze(source)

        assertEquals(3, warnings.size)
        assertEquals(listOf(Position(1, 5), Position(2, 5), Position(4, 5)), warnings.map { it.position })
    }

    // con IdentifierFormat como enum un formato invalido ya no compila: el unico String que
    // puede nombrar un formato inexistente entra por la config, y ahi es donde se valida
    @Test
    fun `rejects an identifier format that no rule knows`() {
        assertFailsWith<IllegalArgumentException> { IdentifierFormat.fromConfigValue("camelCase") }
    }
}
