package printscript.linter.rule

import printscript.ast.Assignment
import printscript.ast.BinaryExpression
import printscript.ast.Identifier
import printscript.ast.NumberLiteral
import printscript.ast.PrintCall
import printscript.ast.ReadInput
import printscript.ast.Statement
import printscript.ast.StringLiteral
import printscript.ast.VariableDeclaration
import printscript.common.Position
import printscript.common.TokenType
import printscript.linter.Linter
import printscript.linter.LinterRules
import printscript.linter.Warning
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LinterRuleRegistryTest {
    // println(1 + 2): lo marca la regla de println y ninguna otra
    private fun printlnWithExpression() =
        PrintCall(
            BinaryExpression(
                NumberLiteral(1.0, Position(1, 9)),
                TokenType.PLUS,
                NumberLiteral(2.0, Position(1, 13)),
                Position(1, 9),
            ),
            Position(1, 1),
        )

    // readInput("a" + "b"): lo marca la regla de readInput y ninguna otra
    private fun readInputWithExpression() =
        VariableDeclaration(
            "x",
            "string",
            ReadInput(
                BinaryExpression(
                    StringLiteral("a", Position(1, 10)),
                    TokenType.PLUS,
                    StringLiteral("b", Position(1, 14)),
                    Position(1, 10),
                ),
                Position(1, 10),
            ),
            Position(1, 1),
        )

    private fun warningsFor(
        config: LinterRules,
        statement: Statement,
    ): List<Warning> = LinterRuleRegistry().ruleFor(config).check(statement)

    @Test
    fun `the default config registers the identifier rule and the argument group`() {
        val rules = LinterRuleRegistry().rulesFor(LinterRules())

        assertEquals(2, rules.size)
        assertTrue(rules.any { it is IdentifierFormatRule })
        assertTrue(rules.any { it is CompositeRule })
    }

    @Test
    fun `the println and readInput rules come grouped in one composite`() {
        val group = LinterRuleRegistry().rulesFor(LinterRules()).filterIsInstance<CompositeRule>().single()

        assertEquals(1, group.check(printlnWithExpression()).size)
    }

    @Test
    fun `turning off the println rule leaves it out`() {
        val config = LinterRules(printCallArgumentsMustBeLiteralOrIdentifier = false)

        assertTrue(warningsFor(config, printlnWithExpression()).isEmpty())
    }

    @Test
    fun `turning off the readInput rule leaves it out`() {
        val config = LinterRules(readInputArgumentsMustBeLiteralOrIdentifier = false)

        assertTrue(warningsFor(config, readInputWithExpression()).isEmpty())
    }

    @Test
    fun `turning off the println rule still keeps the readInput rule active`() {
        val config = LinterRules(printCallArgumentsMustBeLiteralOrIdentifier = false)

        assertEquals(1, warningsFor(config, readInputWithExpression()).size)
    }

    @Test
    fun `turning off the readInput rule still keeps the println rule active`() {
        val config = LinterRules(readInputArgumentsMustBeLiteralOrIdentifier = false)

        assertEquals(1, warningsFor(config, printlnWithExpression()).size)
    }

    @Test
    fun `a config that names no rule at all builds nothing`() {
        val rules =
            LinterRuleRegistry().rulesFor(
                LinterRules(
                    identifierFormat = null,
                    printCallArgumentsMustBeLiteralOrIdentifier = null,
                    readInputArgumentsMustBeLiteralOrIdentifier = null,
                ),
            )

        assertTrue(rules.isEmpty())
    }

    @Test
    fun `the identifier rule is left out when the config does not name it`() {
        val rules = LinterRuleRegistry().rulesFor(LinterRules(identifierFormat = null))

        assertTrue(rules.none { it is IdentifierFormatRule })
    }

    @Test
    fun `the registered identifier format rule honours the configured format`() {
        val rules = LinterRuleRegistry().rulesFor(LinterRules(identifierFormat = "snake case"))
        val rule = rules.filterIsInstance<IdentifierFormatRule>().single()

        val warnings = rule.check(Assignment("myVar", Identifier("x"), Position(1, 1)))

        assertEquals(1, warnings.size)
        assertEquals("Identifier 'myVar' does not match format snake case", warnings[0].message)
    }

    @Test
    fun `register adds a rule on top of the default ones`() {
        val extra = LinterRuleFactory { listOf(NoOpRule()) }

        val rules = LinterRuleRegistry().register(extra).rulesFor(LinterRules())

        assertEquals(3, rules.size)
        assertTrue(rules.any { it is NoOpRule })
    }

    @Test
    fun `a factory can contribute multiple rules`() {
        val extra = LinterRuleFactory { listOf(NoOpRule(), AlwaysWarnsRule()) }

        val rules = LinterRuleRegistry().register(extra).rulesFor(LinterRules())

        assertEquals(4, rules.size)
        assertTrue(rules.any { it is NoOpRule })
        assertTrue(rules.any { it is AlwaysWarnsRule })
    }

    @Test
    fun `register does not mutate the registry it was called on`() {
        val original = LinterRuleRegistry()

        original.register(LinterRuleFactory { listOf(NoOpRule()) })

        assertEquals(2, original.rulesFor(LinterRules()).size)
    }

    @Test
    fun `ruleFor hands back a single rule that runs all of them`() {
        val rule = LinterRuleRegistry().ruleFor(LinterRules())

        assertTrue(rule is CompositeRule)
        assertEquals(1, rule.check(printlnWithExpression()).size)
    }

    @Test
    fun `a factory that returns an empty list contributes no rule`() {
        val rules = LinterRuleRegistry(listOf(LinterRuleFactory { emptyList() })).rulesFor(LinterRules())

        assertTrue(rules.isEmpty())
    }

    @Test
    fun `a rule registered by hand actually runs and emits its warning`() {
        val registry = LinterRuleRegistry().register(LinterRuleFactory { listOf(AlwaysWarnsRule()) })
        val statements = listOf<Statement>(Assignment("miVariable", Identifier("x"), Position(7, 2)))

        val warnings = mutableListOf<Warning>()
        Linter(registry.ruleFor(LinterRules())).analyze(statements.iterator(), warnings::add)

        assertEquals(1, warnings.size)
        assertEquals("siempre avisa", warnings[0].message)
        assertEquals(Position(7, 2), warnings[0].position)
    }

    private class NoOpRule : LinterRule {
        override fun check(statement: Statement): List<Warning> = emptyList()
    }

    private class AlwaysWarnsRule : LinterRule {
        override fun check(statement: Statement): List<Warning> = listOf(Warning("siempre avisa", statement.position))
    }
}
