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

    @Test
    fun `check returns empty list when checkNode produces no warnings`() {
        val cleanRule =
            object : NodeRule<NumberLiteral>(NumberLiteral::class) {
                override fun checkNode(node: NumberLiteral): List<Warning> = emptyList()
            }

        assertTrue(cleanRule.check(NumberLiteral(42.0, pos())).isEmpty())
    }

    @Test
    fun `check returns multiple warnings when checkNode produces multiple warnings`() {
        val multiWarningRule =
            object : NodeRule<NumberLiteral>(NumberLiteral::class) {
                override fun checkNode(node: NumberLiteral): List<Warning> =
                    listOf(Warning("w1", node.position), Warning("w2", node.position))
            }

        val warnings = multiWarningRule.check(NumberLiteral(42.0, pos()))
        assertEquals(2, warnings.size)
    }

    @Test
    fun `appliesTo and check support interface or supertype generic bounds`() {
        val expressionRule =
            object : NodeRule<printscript.ast.Expression>(printscript.ast.Expression::class) {
                override fun checkNode(node: printscript.ast.Expression): List<Warning> =
                    listOf(Warning("expression", node.position))
            }

        val numLiteral = NumberLiteral(10.0, pos())
        val strLiteral = StringLiteral("test", pos())
        val decl = printscript.ast.VariableDeclaration("x", "number", null, pos())

        assertTrue(expressionRule.appliesTo(numLiteral))
        assertTrue(expressionRule.appliesTo(strLiteral))
        assertFalse(expressionRule.appliesTo(decl))

        assertEquals(1, expressionRule.check(numLiteral).size)
        assertEquals(0, expressionRule.check(decl).size)
    }
}
