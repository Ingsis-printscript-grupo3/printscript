package printscript.linter

import printscript.ast.Assignment
import printscript.ast.BinaryExpression
import printscript.ast.Block
import printscript.ast.BooleanLiteral
import printscript.ast.Identifier
import printscript.ast.IfStatement
import printscript.ast.NumberLiteral
import printscript.ast.PrintCall
import printscript.ast.ReadEnv
import printscript.ast.ReadInput
import printscript.ast.StringLiteral
import printscript.ast.VariableDeclaration
import printscript.common.Position
import printscript.common.TokenType
import kotlin.test.Test
import kotlin.test.assertEquals

class NodeChildrenTest {
    private fun pos() = Position(1, 1)

    @Test
    fun `childrenOf extracts value from VariableDeclaration with initializer`() {
        val expr = NumberLiteral(42.0, pos())
        val decl = VariableDeclaration("x", "number", expr, pos())

        assertEquals(listOf(expr), childrenOf(decl))
    }

    @Test
    fun `childrenOf returns empty list for VariableDeclaration without initializer`() {
        val decl = VariableDeclaration("x", "number", null, pos())

        assertEquals(emptyList(), childrenOf(decl))
    }

    @Test
    fun `childrenOf extracts value from Assignment`() {
        val expr = NumberLiteral(10.0, pos())
        val assignment = Assignment("x", expr, pos())

        assertEquals(listOf(expr), childrenOf(assignment))
    }

    @Test
    fun `childrenOf extracts value from PrintCall`() {
        val expr = StringLiteral("hola", pos())
        val printCall = PrintCall(expr, pos())

        assertEquals(listOf(expr), childrenOf(printCall))
    }

    @Test
    fun `childrenOf extracts condition and both branches from IfStatement with else`() {
        val condition = BooleanLiteral(true, pos())
        val thenBlock = Block(listOf(PrintCall(NumberLiteral(1.0, pos()), pos())), pos())
        val elseBlock = Block(listOf(PrintCall(NumberLiteral(2.0, pos()), pos())), pos())
        val ifStmt = IfStatement(condition, thenBlock, elseBlock, pos())

        assertEquals(listOf(condition, thenBlock, elseBlock), childrenOf(ifStmt))
    }

    @Test
    fun `childrenOf extracts condition and then branch from IfStatement without else`() {
        val condition = BooleanLiteral(true, pos())
        val thenBlock = Block(listOf(PrintCall(NumberLiteral(1.0, pos()), pos())), pos())
        val ifStmt = IfStatement(condition, thenBlock, null, pos())

        assertEquals(listOf(condition, thenBlock), childrenOf(ifStmt))
    }

    @Test
    fun `childrenOf extracts statements from Block`() {
        val stmt1 = PrintCall(NumberLiteral(1.0, pos()), pos())
        val stmt2 = PrintCall(NumberLiteral(2.0, pos()), pos())
        val block = Block(listOf(stmt1, stmt2), pos())

        assertEquals(listOf(stmt1, stmt2), childrenOf(block))
    }

    @Test
    fun `childrenOf extracts left and right from BinaryExpression`() {
        val left = NumberLiteral(1.0, pos())
        val right = NumberLiteral(2.0, pos())
        val binary = BinaryExpression(left, TokenType.PLUS, right, pos())

        assertEquals(listOf(left, right), childrenOf(binary))
    }

    @Test
    fun `childrenOf extracts argument from ReadInput`() {
        val arg = StringLiteral("name?", pos())
        val readInput = ReadInput(arg, pos())

        assertEquals(listOf(arg), childrenOf(readInput))
    }

    @Test
    fun `childrenOf extracts argument from ReadEnv`() {
        val arg = StringLiteral("HOME", pos())
        val readEnv = ReadEnv(arg, pos())

        assertEquals(listOf(arg), childrenOf(readEnv))
    }

    @Test
    fun `childrenOf returns empty list for leaf expressions`() {
        assertEquals(emptyList(), childrenOf(NumberLiteral(1.0, pos())))
        assertEquals(emptyList(), childrenOf(StringLiteral("a", pos())))
        assertEquals(emptyList(), childrenOf(BooleanLiteral(false, pos())))
        assertEquals(emptyList(), childrenOf(Identifier("x", pos())))
    }
}
