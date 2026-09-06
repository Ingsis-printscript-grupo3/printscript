package printscript.ast

import printscript.common.Position
import printscript.common.TokenType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class AstTest {
    @Test
    fun `expression nodes expose their position, default or explicit`() {
        val number = NumberLiteral(1.0)
        val string = StringLiteral("hi", Position(2, 3))
        val identifier = Identifier("x")
        val binary = BinaryExpression(number, TokenType.PLUS, string, Position(4, 5))

        assertEquals(Position(0, 0), number.position)
        assertEquals(Position(2, 3), string.position)
        assertEquals(Position(0, 0), identifier.position)
        assertEquals(Position(4, 5), binary.position)

        val nodes: List<PositionedNode> = listOf(number, string, identifier, binary)
        nodes.forEach { assertEquals(it.position, it.position) }
    }

    @Test
    fun `expression nodes carry the expected values`() {
        assertEquals(1.0, NumberLiteral(1.0).value)
        assertEquals("hi", StringLiteral("hi").value)
        assertEquals("x", Identifier("x").name)

        val binary = BinaryExpression(NumberLiteral(1.0), TokenType.MINUS, NumberLiteral(2.0))
        assertEquals(TokenType.MINUS, binary.operator)
    }

    @Test
    fun `expression data classes support equals, hashCode, copy and toString`() {
        val a = NumberLiteral(1.0, Position(1, 1))
        val b = NumberLiteral(1.0, Position(1, 1))
        val c = a.copy(value = 2.0)

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, c)
        assert(a.toString().contains("NumberLiteral"))
    }

    @Test
    fun `statement nodes expose their position, default or explicit`() {
        val declaration = VariableDeclaration("x", "number", NumberLiteral(1.0))
        val declarationWithoutValue = VariableDeclaration("x", "number", null, Position(1, 2))
        val assignment = Assignment("x", NumberLiteral(1.0), Position(3, 4))
        val printCall = PrintCall(NumberLiteral(1.0))

        assertEquals(Position(0, 0), declaration.position)
        assertEquals(Position(1, 2), declarationWithoutValue.position)
        assertEquals(Position(3, 4), assignment.position)
        assertEquals(Position(0, 0), printCall.position)

        val nodes: List<PositionedNode> = listOf(declaration, declarationWithoutValue, assignment, printCall)
        nodes.forEach { assertEquals(it.position, it.position) }
    }

    @Test
    fun `statement nodes carry the expected values`() {
        val declaration = VariableDeclaration("x", "number", null)
        assertEquals("x", declaration.name)
        assertEquals("number", declaration.type)
        assertEquals(null, declaration.value)

        val assignment = Assignment("y", NumberLiteral(2.0))
        assertEquals("y", assignment.name)
        assertEquals(NumberLiteral(2.0), assignment.value)

        val printCall = PrintCall(NumberLiteral(3.0))
        assertEquals(NumberLiteral(3.0), printCall.value)
    }

    @Test
    fun `statement data classes support equals, hashCode, copy and toString`() {
        val a = Assignment("x", NumberLiteral(1.0), Position(1, 1))
        val b = Assignment("x", NumberLiteral(1.0), Position(1, 1))
        val c = a.copy(name = "y")

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, c)
        assert(a.toString().contains("Assignment"))
    }
}
