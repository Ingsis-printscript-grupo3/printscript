package printscript.linter.rule

import printscript.ast.Assignment
import printscript.ast.Identifier
import printscript.common.Position
import printscript.linter.LinterRules
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LinterRuleRegistryTest {
    @Test
    fun `the default config registers every rule`() {
        val rules = LinterRuleRegistry.rulesFor(LinterRules())

        assertEquals(3, rules.size)
        assertTrue(rules.any { it is IdentifierFormatRule })
        assertTrue(rules.any { it is PrintCallArgumentRule })
        assertTrue(rules.any { it is ReadInputArgumentRule })
    }

    @Test
    fun `turning off the println rule leaves it out`() {
        val rules = LinterRuleRegistry.rulesFor(LinterRules(printCallArgumentsMustBeLiteralOrIdentifier = false))

        assertTrue(rules.none { it is PrintCallArgumentRule })
        assertTrue(rules.any { it is ReadInputArgumentRule })
    }

    @Test
    fun `turning off the readInput rule leaves it out`() {
        val rules = LinterRuleRegistry.rulesFor(LinterRules(readInputArgumentsMustBeLiteralOrIdentifier = false))

        assertTrue(rules.none { it is ReadInputArgumentRule })
        assertTrue(rules.any { it is PrintCallArgumentRule })
    }

    @Test
    fun `the identifier format rule has no flag, it is always registered`() {
        val rules =
            LinterRuleRegistry.rulesFor(
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
        val rules = LinterRuleRegistry.rulesFor(LinterRules(identifierFormat = "snake case"))
        val rule = rules.filterIsInstance<IdentifierFormatRule>().single()

        val warnings = rule.check(Assignment("myVar", Identifier("x"), Position(1, 1)))

        assertEquals(1, warnings.size)
        assertEquals("Identifier 'myVar' does not match format snake case", warnings[0].message)
    }
}
