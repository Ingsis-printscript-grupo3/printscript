package printscript.ast

import printscript.common.Position
import printscript.common.TokenType
import kotlin.test.Test
import kotlin.test.assertEquals

class ChildrenTest {
    private fun pos() = Position(1, 1)

    @Test
    fun `children extracts value from VariableDeclaration with initializer`() {
        val expr = NumberLiteral(42.0, pos())
        val decl = VariableDeclaration("x", "number", expr, pos())

        assertEquals(listOf(expr), decl.children)
    }

    @Test
    fun `children returns empty list for VariableDeclaration without initializer`() {
        val decl = VariableDeclaration("x", "number", null, pos())

        assertEquals(emptyList(), decl.children)
    }

    @Test
    fun `children extracts value from Assignment`() {
        val expr = NumberLiteral(10.0, pos())
        val assignment = Assignment("x", expr, pos())

        assertEquals(listOf(expr), assignment.children)
    }

    @Test
    fun `children extracts value from PrintCall`() {
        val expr = StringLiteral("hola", pos())
        val printCall = PrintCall(expr, pos())

        assertEquals(listOf(expr), printCall.children)
    }

    @Test
    fun `children extracts condition and both branches from IfStatement with else`() {
        val condition = BooleanLiteral(true, pos())
        val thenBlock = Block(listOf(PrintCall(NumberLiteral(1.0, pos()), pos())), pos())
        val elseBlock = Block(listOf(PrintCall(NumberLiteral(2.0, pos()), pos())), pos())
        val ifStmt = IfStatement(condition, thenBlock, elseBlock, pos())

        assertEquals(listOf(condition, thenBlock, elseBlock), ifStmt.children)
    }

    @Test
    fun `children extracts condition and then branch from IfStatement without else`() {
        val condition = BooleanLiteral(true, pos())
        val thenBlock = Block(listOf(PrintCall(NumberLiteral(1.0, pos()), pos())), pos())
        val ifStmt = IfStatement(condition, thenBlock, null, pos())

        assertEquals(listOf(condition, thenBlock), ifStmt.children)
    }

    @Test
    fun `children extracts statements from Block`() {
        val stmt1 = PrintCall(NumberLiteral(1.0, pos()), pos())
        val stmt2 = PrintCall(NumberLiteral(2.0, pos()), pos())
        val block = Block(listOf(stmt1, stmt2), pos())

        assertEquals(listOf(stmt1, stmt2), block.children)
    }

    @Test
    fun `children extracts left and right from BinaryExpression`() {
        val left = NumberLiteral(1.0, pos())
        val right = NumberLiteral(2.0, pos())
        val binary = BinaryExpression(left, TokenType.PLUS, right, pos())

        assertEquals(listOf(left, right), binary.children)
    }

    @Test
    fun `children extracts argument from ReadInput`() {
        val arg = StringLiteral("name?", pos())
        val readInput = ReadInput(arg, pos())

        assertEquals(listOf(arg), readInput.children)
    }

    @Test
    fun `children extracts argument from ReadEnv`() {
        val arg = StringLiteral("HOME", pos())
        val readEnv = ReadEnv(arg, pos())

        assertEquals(listOf(arg), readEnv.children)
    }

    @Test
    fun `children returns empty list for leaf expressions`() {
        assertEquals(emptyList(), NumberLiteral(1.0, pos()).children)
        assertEquals(emptyList(), StringLiteral("a", pos()).children)
        assertEquals(emptyList(), BooleanLiteral(false, pos()).children)
        assertEquals(emptyList(), Identifier("x", pos()).children)
    }
}
