package printscript.linter.rule

import printscript.ast.Assignment
import printscript.ast.Identifier
import printscript.ast.Statement
import printscript.common.Position
import printscript.linter.Linter
import printscript.linter.LinterRules
import printscript.linter.Warning
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LinterRuleRegistryTest {
    @Test
    fun `the default config registers every rule`() {
        val rules = LinterRuleRegistry().rulesFor(LinterRules())

        assertEquals(3, rules.size)
        assertTrue(rules.any { it is IdentifierFormatRule })
        assertTrue(rules.any { it is PrintCallArgumentRule })
        assertTrue(rules.any { it is ReadInputArgumentRule })
    }

    @Test
    fun `turning off the println rule leaves it out`() {
        val rules = LinterRuleRegistry().rulesFor(LinterRules(printCallArgumentsMustBeLiteralOrIdentifier = false))

        assertTrue(rules.none { it is PrintCallArgumentRule })
        assertTrue(rules.any { it is ReadInputArgumentRule })
    }

    @Test
    fun `turning off the readInput rule leaves it out`() {
        val rules = LinterRuleRegistry().rulesFor(LinterRules(readInputArgumentsMustBeLiteralOrIdentifier = false))

        assertTrue(rules.none { it is ReadInputArgumentRule })
        assertTrue(rules.any { it is PrintCallArgumentRule })
    }

    @Test
    fun `the identifier format rule has no flag, it is always registered`() {
        val rules =
            LinterRuleRegistry().rulesFor(
                LinterRules(
                    printCallArgumentsMustBeLiteralOrIdentifier = false,
                    readInputArgumentsMustBeLiteralOrIdentifier = false,
                ),
            )

        assertEquals(1, rules.size)
        assertTrue(rules.single() is IdentifierFormatRule)
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
        val extra = LinterRuleFactory { NoOpRule() }

        val rules = LinterRuleRegistry().register(extra).rulesFor(LinterRules())

        assertEquals(4, rules.size)
        assertTrue(rules.any { it is NoOpRule })
    }

    @Test
    fun `register does not mutate the registry it was called on`() {
        val original = LinterRuleRegistry()

        original.register(LinterRuleFactory { NoOpRule() })

        assertEquals(3, original.rulesFor(LinterRules()).size)
    }

    @Test
    fun `a rule registered by hand actually runs and emits its warning`() {
        val registry = LinterRuleRegistry().register(LinterRuleFactory { AlwaysWarnsRule() })
        val statements = listOf<Statement>(Assignment("miVariable", Identifier("x"), Position(7, 2)))

        val warnings = mutableListOf<Warning>()
        Linter(registry.rulesFor(LinterRules())).analyze(statements.iterator(), warnings::add)

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
