package printscript.linter.rule

import printscript.common.Position
import printscript.linter.IdentifierFormat
import printscript.linter.expressionOf
import printscript.linter.statementOf
import printscript.linter.warningsOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IdentifierFormatRuleTest {
    private val camelCaseRule = IdentifierFormatRule(IdentifierFormat.CAMEL_CASE)
    private val snakeCaseRule = IdentifierFormatRule(IdentifierFormat.SNAKE_CASE)

    @Test
    fun `applies to VariableDeclaration and Assignment`() {
        assertTrue(camelCaseRule.appliesTo(statementOf("let myVar: number;")))
        assertTrue(camelCaseRule.appliesTo(statementOf("myVar = 1;")))
        assertFalse(camelCaseRule.appliesTo(statementOf("println(myVar);")))
        assertFalse(camelCaseRule.appliesTo(expressionOf("1")))
    }

    @Test
    fun `camel case variable declaration passes under camel case rule`() {
        assertEquals(0, warningsOf(camelCaseRule, "let myVar: number;").size)
    }

    @Test
    fun `snake case variable declaration warns under camel case rule`() {
        val warnings = warningsOf(camelCaseRule, "let my_var: number;")

        assertEquals(1, warnings.size)
        assertEquals("Identifier 'my_var' does not match format camel case", warnings[0].message)
        assertEquals(Position(1, 5), warnings[0].position)
    }

    @Test
    fun `snake case assignment warns under camel case rule`() {
        val warnings = warningsOf(camelCaseRule, "\n  my_var = 1;")

        assertEquals(1, warnings.size)
        assertEquals("Identifier 'my_var' does not match format camel case", warnings[0].message)
        assertEquals(Position(2, 3), warnings[0].position)
    }

    @Test
    fun `camel case variable declaration warns under snake case rule`() {
        val warnings = warningsOf(snakeCaseRule, "let myVar: number;")

        assertEquals(1, warnings.size)
        assertEquals("Identifier 'myVar' does not match format snake case", warnings[0].message)
        assertEquals(Position(1, 5), warnings[0].position)
    }

    @Test
    fun `snake case variable declaration passes under snake case rule`() {
        assertEquals(0, warningsOf(snakeCaseRule, "let my_var: number;").size)
    }

    @Test
    fun `const declaration warns if name does not match format`() {
        val warnings = warningsOf(camelCaseRule, "const MI_CONST: number = 1;")

        assertEquals(1, warnings.size)
        assertEquals("Identifier 'MI_CONST' does not match format camel case", warnings[0].message)
    }

    @Test
    fun `check returns empty list for node where appliesTo is false`() {
        assertEquals(emptyList(), camelCaseRule.check(statementOf("println(myVar);")))
    }
}
