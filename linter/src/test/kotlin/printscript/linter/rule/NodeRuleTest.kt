package printscript.linter.rule

import printscript.ast.NumberLiteral
import printscript.ast.StringLiteral
import printscript.common.Position
import printscript.linter.Warning
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NodeRuleTest {
    private val rule =
        object : NodeRule<NumberLiteral>(NumberLiteral::class) {
            override fun checkNode(node: NumberLiteral): List<Warning> = listOf(Warning("number", node.position))
        }

    private fun pos() = Position(1, 1)

    @Test
    fun `appliesTo returns true for matching node and false for non matching node`() {
        assertTrue(rule.appliesTo(NumberLiteral(42.0, pos())))
        assertFalse(rule.appliesTo(StringLiteral("hello", pos())))
    }

    @Test
    fun `check invokes checkNode for matching node`() {
        val warnings = rule.check(NumberLiteral(42.0, pos()))
        assertEquals(1, warnings.size)
        assertEquals("number", warnings[0].message)
    }

    @Test
    fun `check returns empty list for non matching node`() {
        val warnings = rule.check(StringLiteral("hello", pos()))
        assertTrue(warnings.isEmpty())
    }
}
