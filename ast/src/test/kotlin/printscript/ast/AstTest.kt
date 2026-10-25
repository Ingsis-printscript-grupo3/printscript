package printscript.ast

import printscript.common.Position
import printscript.common.TokenType
import kotlin.test.Test
import kotlin.test.assertEquals

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
    fun `variable declaration defaults to non-const and can be marked const`() {
        val letDeclaration = VariableDeclaration("x", "number", NumberLiteral(1.0))
        val constDeclaration = VariableDeclaration("y", "number", NumberLiteral(2.0), isConst = true)

        assertEquals(false, letDeclaration.isConst)
        assertEquals(true, constDeclaration.isConst)
    }

    @Test
    fun `boolean literal exposes its value and position`() {
        val literal = BooleanLiteral(true, Position(1, 2))

        assertEquals(true, literal.value)
        assertEquals(Position(1, 2), literal.position)
        assertEquals(Position(0, 0), BooleanLiteral(false).position)
    }

    @Test
    fun `readInput and readEnv wrap their argument expression`() {
        val readInput = ReadInput(StringLiteral("prompt"), Position(1, 1))
        val readEnv = ReadEnv(StringLiteral("HOME"))

        assertEquals(StringLiteral("prompt"), readInput.argument)
        assertEquals(Position(1, 1), readInput.position)
        assertEquals(StringLiteral("HOME"), readEnv.argument)
        assertEquals(Position(0, 0), readEnv.position)
    }

    @Test
    fun `block holds an ordered list of statements`() {
        val block = Block(listOf(PrintCall(NumberLiteral(1.0)), PrintCall(NumberLiteral(2.0))), Position(3, 1))

        assertEquals(2, block.statements.size)
        assertEquals(Position(3, 1), block.position)
    }

    @Test
    fun `if statement supports an optional else branch`() {
        val thenBranch = Block(listOf(PrintCall(NumberLiteral(1.0))))
        val elseBranch = Block(listOf(PrintCall(NumberLiteral(2.0))))
        val withElse = IfStatement(BooleanLiteral(true), thenBranch, elseBranch, Position(5, 1))
        val withoutElse = IfStatement(BooleanLiteral(false), thenBranch, null)

        assertEquals(elseBranch, withElse.elseBranch)
        assertEquals(Position(5, 1), withElse.position)
        assertEquals(null, withoutElse.elseBranch)
        assertEquals(Position(0, 0), withoutElse.position)
    }
}
