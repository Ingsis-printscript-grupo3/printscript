package printscript.linter.rule

import printscript.ast.BinaryExpression
import printscript.ast.PositionedNode
import printscript.common.Position
import printscript.linter.LinterFactory
import printscript.linter.Warning
import printscript.linter.warningsOf
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LinterRuleRegistryTest {
    private val println = "mandatory-variable-or-literal-in-println"
    private val readInput = "mandatory-variable-or-literal-in-readInput"

    private val printlnWithExpression = "println(1 + 2);"
    private val readInputWithExpression = """let dato: string = readInput("a" + "b");"""

    private fun rulesFor(vararg config: Pair<String, String>) = LinterRuleRegistry().rulesFor(config.toMap())

    private fun lint(
        source: String,
        vararg config: Pair<String, String>,
    ): List<Warning> = warningsOf(LinterFactory.create(config = config.toMap()), source)

    // stderr capturado, para ver los avisos de claves desconocidas
    private fun stderrOf(block: () -> Unit): String {
        val buffer = ByteArrayOutputStream()
        val original = System.err
        System.setErr(PrintStream(buffer))
        try {
            block()
        } finally {
            System.setErr(original)
        }
        return buffer.toString()
    }

    @Test
    fun `the default config turns every rule on`() {
        val rules = LinterRuleRegistry().run { rulesFor(defaultConfig()) }

        assertEquals(3, rules.size)
        assertTrue(rules.any { it is IdentifierFormatRule })
        assertTrue(rules.any { it is PrintCallArgumentRule })
        assertTrue(rules.any { it is ReadInputArgumentRule })
    }

    @Test
    fun `a config that names no rule builds nothing`() {
        assertTrue(rulesFor().isEmpty())
    }

    @Test
    fun `a rule only runs when the config names its key`() {
        val rules = rulesFor(println to "true")

        assertEquals(listOf(PrintCallArgumentRule::class), rules.map { it::class })
    }

    @Test
    fun `turning off the println rule keeps the readInput rule active`() {
        assertTrue(lint(printlnWithExpression, println to "false", readInput to "true").isEmpty())
        assertEquals(1, lint(readInputWithExpression, println to "false", readInput to "true").size)
    }

    @Test
    fun `turning off the readInput rule keeps the println rule active`() {
        assertTrue(lint(readInputWithExpression, println to "true", readInput to "false").isEmpty())
        assertEquals(1, lint(printlnWithExpression, println to "true", readInput to "false").size)
    }

    @Test
    fun `booleans ignore their casing`() {
        assertEquals(1, rulesFor(println to "TRUE").size)
        assertTrue(rulesFor(println to "False").isEmpty())
    }

    // asi llegan un null de JSON y una clave de YAML sin valor
    @Test
    fun `an empty or null value falls back to the default`() {
        assertEquals(1, lint(printlnWithExpression, println to "null").size)
        assertEquals(1, lint(printlnWithExpression, println to "").size)
        assertEquals(1, lint("let mi_variable: number = 1;", "identifier_format" to "").size)
    }

    @Test
    fun `the identifier rule honours the configured format`() {
        val warnings = lint("let miVariable: number = 1;", "identifier_format" to "snake case")

        assertEquals(1, warnings.size)
        assertEquals("Identifier 'miVariable' does not match format snake case", warnings[0].message)
    }

    @Test
    fun `rejects a value that is not a boolean`() {
        val error = assertFailsWith<IllegalArgumentException> { rulesFor(println to "si") }

        assertEquals("Expected boolean for '$println'", error.message)
    }

    @Test
    fun `rejects an identifier format that no rule knows`() {
        assertFailsWith<IllegalArgumentException> { rulesFor("identifier_format" to "kebab case") }
    }

    @Test
    fun `an unknown key is ignored and reported on stderr`() {
        lateinit var rules: List<LinterRule>

        val stderr = stderrOf { rules = rulesFor("no-existe" to "true") }

        assertTrue(rules.isEmpty())
        assertEquals("linter: ignoring unknown key 'no-existe'", stderr.trim())
    }

    @Test
    fun `the rules keep the order of the registry, not the one of the config`() {
        val rules = rulesFor(readInput to "true", println to "true", "identifier_format" to "camel case")

        assertEquals(
            listOf(IdentifierFormatRule::class, PrintCallArgumentRule::class, ReadInputArgumentRule::class),
            rules.map { it::class },
        )
    }

    @Test
    fun `register adds a rule on top of the default ones and its key stops being unknown`() {
        val registry = LinterRuleRegistry().register(LinterRuleFactory.toggle("no-op") { NoOpRule() })
        lateinit var rules: List<LinterRule>

        val stderr = stderrOf { rules = registry.rulesFor(registry.defaultConfig()) }

        assertEquals(4, rules.size)
        assertTrue(rules.any { it is NoOpRule })
        assertTrue(stderr.isEmpty())
    }

    @Test
    fun `register does not mutate the registry it was called on`() {
        val original = LinterRuleRegistry()

        original.register(LinterRuleFactory.toggle("no-op") { NoOpRule() })

        assertEquals(3, original.rulesFor(original.defaultConfig()).size)
    }

    @Test
    fun `two factories cannot share a key`() {
        assertFailsWith<IllegalArgumentException> {
            LinterRuleRegistry().register(LinterRuleFactory.toggle(println) { NoOpRule() })
        }
    }

    @Test
    fun `a rule registered by hand runs on nested expressions with its own config value`() {
        val registry =
            LinterRuleRegistry().register(
                LinterRuleFactory("binary-expression-message", default = "operacion") { BinaryExpressionRule(it) },
            )
        val linter = LinterFactory.create(registry, mapOf("binary-expression-message" to "suma encontrada"))

        val warnings = warningsOf(linter, "let x: number = 1 + 2;")

        assertEquals(1, warnings.size)
        assertEquals("suma encontrada", warnings[0].message)
        assertEquals(Position(1, 17), warnings[0].position)
    }

    // sin config, la factory toma los defaults del registry que recibe y no los del de fabrica
    @Test
    fun `a linter created with a custom registry uses that registry's defaults`() {
        val registry =
            LinterRuleRegistry().register(
                LinterRuleFactory("binary-expression-message", default = "operacion") { BinaryExpressionRule(it) },
            )

        val warnings = warningsOf(LinterFactory.create(registry), "let x: number = 1 + 2;")

        assertEquals(listOf("operacion"), warnings.map { it.message })
    }

    private class NoOpRule : LinterRule {
        override fun appliesTo(node: PositionedNode): Boolean = false

        override fun check(node: PositionedNode): List<Warning> = emptyList()
    }

    private class BinaryExpressionRule(private val message: String) : LinterRule {
        override fun appliesTo(node: PositionedNode): Boolean = node is BinaryExpression

        override fun check(node: PositionedNode): List<Warning> = listOf(Warning(message, node.position))
    }
}
