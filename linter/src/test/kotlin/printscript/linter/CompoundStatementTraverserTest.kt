package printscript.linter

import printscript.ast.Block
import printscript.ast.BooleanLiteral
import printscript.ast.IfStatement
import printscript.ast.NumberLiteral
import printscript.ast.PrintCall
import printscript.ast.Statement
import printscript.ast.VariableDeclaration
import printscript.common.Position
import printscript.linter.rule.LinterRule
import kotlin.test.Test
import kotlin.test.assertEquals

class CompoundStatementTraverserTest {
    private fun pos() = Position(1, 1)

    @Test
    fun `default traverser extracts then branch from IfStatement without else`() {
        val traverser = DefaultCompoundStatementTraverser()
        val thenBlock = Block(listOf(PrintCall(NumberLiteral(1.0, pos()), pos())), pos())
        val ifStmt = IfStatement(BooleanLiteral(true, pos()), thenBlock, null, pos())

        val children = traverser.childrenOf(ifStmt).toList()

        assertEquals(listOf(thenBlock), children)
    }

    @Test
    fun `default traverser extracts both branches from IfStatement with else`() {
        val traverser = DefaultCompoundStatementTraverser()
        val thenBlock = Block(listOf(PrintCall(NumberLiteral(1.0, pos()), pos())), pos())
        val elseBlock = Block(listOf(PrintCall(NumberLiteral(2.0, pos()), pos())), pos())
        val ifStmt = IfStatement(BooleanLiteral(true, pos()), thenBlock, elseBlock, pos())

        val children = traverser.childrenOf(ifStmt).toList()

        assertEquals(listOf(thenBlock, elseBlock), children)
    }

    @Test
    fun `default traverser extracts statements from Block`() {
        val traverser = DefaultCompoundStatementTraverser()
        val stmt1 = PrintCall(NumberLiteral(1.0, pos()), pos())
        val stmt2 = PrintCall(NumberLiteral(2.0, pos()), pos())
        val block = Block(listOf(stmt1, stmt2), pos())

        val children = traverser.childrenOf(block).toList()

        assertEquals(listOf(stmt1, stmt2), children)
    }

    @Test
    fun `default traverser returns empty sequence for simple statement`() {
        val traverser = DefaultCompoundStatementTraverser()
        val stmt = VariableDeclaration("x", "number", NumberLiteral(1.0, pos()), pos())

        val children = traverser.childrenOf(stmt).toList()

        assertEquals(emptyList(), children)
    }

    @Test
    fun `default traverser uses custom handler when provided`() {
        val customBlock = Block(emptyList(), pos())
        val child = PrintCall(NumberLiteral(42.0, pos()), pos())
        val traverser =
            DefaultCompoundStatementTraverser(
                customHandlers =
                    listOf { statement ->
                        if (statement === customBlock) sequenceOf(child) else null
                    },
            )

        val children = traverser.childrenOf(customBlock).toList()

        assertEquals(listOf(child), children)
    }

    @Test
    fun `linter unwraps compound statements through custom traverser`() {
        val root = Block(emptyList(), pos())
        val child = PrintCall(NumberLiteral(42.0, pos()), pos())

        val customTraverser =
            CompoundStatementTraverser { statement ->
                if (statement === root) sequenceOf(child) else emptySequence()
            }

        val visited = mutableListOf<Statement>()
        val dummyRule =
            object : LinterRule {
                override fun check(statement: Statement): List<Warning> {
                    visited.add(statement)
                    return emptyList()
                }
            }

        val linter = Linter(dummyRule, customTraverser)
        linter.analyze(listOf<Statement>(root).iterator()) { }

        assertEquals(listOf(root, child), visited)
    }
}
