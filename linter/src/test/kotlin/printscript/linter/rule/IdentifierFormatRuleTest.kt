package printscript.linter.rule

import printscript.ast.Assignment
import printscript.ast.Identifier
import printscript.ast.NumberLiteral
import printscript.ast.PrintCall
import printscript.ast.VariableDeclaration
import printscript.common.Position
import printscript.linter.IdentifierFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IdentifierFormatRuleTest {
    private val camelCaseRule = IdentifierFormatRule(IdentifierFormat.CAMEL_CASE)
    private val snakeCaseRule = IdentifierFormatRule(IdentifierFormat.SNAKE_CASE)

    private fun pos() = Position(1, 1)

    @Test
    fun `applies to VariableDeclaration and Assignment`() {
        val decl = VariableDeclaration("myVar", "number", null, pos())
        val assign = Assignment("myVar", NumberLiteral(1.0, pos()), pos())
        val print = PrintCall(Identifier("myVar", pos()), pos())

        assertTrue(camelCaseRule.appliesTo(decl))
        assertTrue(camelCaseRule.appliesTo(assign))
        assertFalse(camelCaseRule.appliesTo(print))
        assertFalse(camelCaseRule.appliesTo(NumberLiteral(1.0, pos())))
    }

    @Test
    fun `camel case variable declaration passes under camel case rule`() {
        val decl = VariableDeclaration("myVar", "number", null, pos())

        assertEquals(0, camelCaseRule.check(decl).size)
    }

    @Test
    fun `snake case variable declaration warns under camel case rule`() {
        val decl = VariableDeclaration("my_var", "number", null, Position(1, 1), namePosition = Position(1, 5))

        val warnings = camelCaseRule.check(decl)

        assertEquals(1, warnings.size)
        assertEquals("Identifier 'my_var' does not match format camel case", warnings[0].message)
        assertEquals(Position(1, 5), warnings[0].position)
    }

    @Test
    fun `snake case assignment warns under camel case rule`() {
        val assign = Assignment("my_var", NumberLiteral(1.0, pos()), Position(2, 3))

        val warnings = camelCaseRule.check(assign)

        assertEquals(1, warnings.size)
        assertEquals("Identifier 'my_var' does not match format camel case", warnings[0].message)
        assertEquals(Position(2, 3), warnings[0].position)
    }

    @Test
    fun `camel case variable declaration warns under snake case rule`() {
        val decl = VariableDeclaration("myVar", "number", null, Position(1, 1), namePosition = Position(1, 5))

        val warnings = snakeCaseRule.check(decl)

        assertEquals(1, warnings.size)
        assertEquals("Identifier 'myVar' does not match format snake case", warnings[0].message)
        assertEquals(Position(1, 5), warnings[0].position)
    }

    @Test
    fun `snake case variable declaration passes under snake case rule`() {
        val decl = VariableDeclaration("my_var", "number", null, pos())

        assertEquals(0, snakeCaseRule.check(decl).size)
    }

    @Test
    fun `const declaration warns if name does not match format`() {
        val constDecl = VariableDeclaration("MI_CONST", "number", null, Position(1, 1), isConst = true)

        val warnings = camelCaseRule.check(constDecl)

        assertEquals(1, warnings.size)
        assertEquals("Identifier 'MI_CONST' does not match format camel case", warnings[0].message)
    }

    @Test
    fun `check returns empty list for node where appliesTo is false`() {
        val print = PrintCall(Identifier("myVar", pos()), pos())

        assertEquals(emptyList(), camelCaseRule.check(print))
    }
}
