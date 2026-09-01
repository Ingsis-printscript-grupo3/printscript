package printscript.semantic

import printscript.ast.Assignment
import printscript.ast.Expression
import printscript.ast.Identifier
import printscript.ast.NumberLiteral
import printscript.ast.PrintCall
import printscript.ast.StringLiteral
import printscript.ast.VariableDeclaration
import printscript.semantic.plugin.statement.AssignmentHandler
import printscript.semantic.plugin.statement.PrintCallHandler
import printscript.semantic.plugin.statement.VariableDeclarationHandler
import printscript.semantic.symbol.SymbolTable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class StatementValidatorTest {
    private fun num(value: Double): Expression = NumberLiteral(value)

    private fun text(value: String): Expression = StringLiteral(value)

    private fun id(name: String): Expression = Identifier(name)

    private fun newValidator(symbolTable: SymbolTable = SymbolTable()): Pair<StatementValidator, SymbolTable> {
        val resolver = ExpressionResolver(symbolTable)
        return StatementValidator(symbolTable, resolver) to symbolTable
    }

    @Test
    fun `declaring a variable with a matching type succeeds`() {
        val (validator, symbolTable) = newValidator()

        val result = validator.validate(VariableDeclaration("x", "number", num(1.0)))

        assertIs<SemanticResult.Success<Unit>>(result)
        assertEquals(SemanticResult.Success("number"), symbolTable.lookup("x"))
    }

    @Test
    fun `declaring a variable without an initializer just registers its type`() {
        val (validator, symbolTable) = newValidator()

        val result = validator.validate(VariableDeclaration("x", "number", null))

        assertIs<SemanticResult.Success<Unit>>(result)
        assertEquals(SemanticResult.Success("number"), symbolTable.lookup("x"))
    }

    @Test
    fun `declaring a variable with a mismatched type fails`() {
        val (validator, _) = newValidator()

        val result = validator.validate(VariableDeclaration("x", "number", text("hi")))

        assertIs<SemanticResult.Failure>(result)
    }

    @Test
    fun `redeclaring an already declared variable fails`() {
        val symbolTable = SymbolTable()
        symbolTable.define("x", "number")
        val (validator, _) = newValidator(symbolTable)

        val result = validator.validate(VariableDeclaration("x", "number", num(1.0)))

        assertIs<SemanticResult.Failure>(result)
    }

    @Test
    fun `declaring a variable whose initializer fails to resolve propagates the failure`() {
        val (validator, _) = newValidator()

        val result = validator.validate(VariableDeclaration("x", "number", id("missing")))

        assertIs<SemanticResult.Failure>(result)
    }

    @Test
    fun `assigning a matching type to a declared variable succeeds`() {
        val symbolTable = SymbolTable()
        symbolTable.define("x", "number")
        val (validator, _) = newValidator(symbolTable)

        val result = validator.validate(Assignment("x", num(2.0)))

        assertIs<SemanticResult.Success<Unit>>(result)
    }

    @Test
    fun `assigning to an undeclared variable fails`() {
        val (validator, _) = newValidator()

        val result = validator.validate(Assignment("x", num(2.0)))

        assertIs<SemanticResult.Failure>(result)
    }

    @Test
    fun `assigning a mismatched type fails`() {
        val symbolTable = SymbolTable()
        symbolTable.define("x", "number")
        val (validator, _) = newValidator(symbolTable)

        val result = validator.validate(Assignment("x", text("hi")))

        assertIs<SemanticResult.Failure>(result)
    }

    @Test
    fun `printing a well-typed expression succeeds`() {
        val (validator, _) = newValidator()

        val result = validator.validate(PrintCall(text("hi")))

        assertIs<SemanticResult.Success<Unit>>(result)
    }

    @Test
    fun `printing an expression that fails to resolve propagates the failure`() {
        val (validator, _) = newValidator()

        val result = validator.validate(PrintCall(id("missing")))

        assertIs<SemanticResult.Failure>(result)
    }

    @Test
    fun `a statement with no handler registered fails`() {
        val symbolTable = SymbolTable()
        val validator = StatementValidator(symbolTable, ExpressionResolver(symbolTable), emptyList())

        assertFailsWith<UnknownStatementError> {
            validator.validate(PrintCall(text("hi")))
        }
    }

    // each handler has a guard that throws if it gets a node that isn't its own;
    // this never happens in the normal flow, since the validator asks matches() first
    @Test
    fun `statement handlers reject nodes that are not theirs`() {
        val print = PrintCall(text("hi"))
        val assignment = Assignment("x", num(1.0))
        val symbolTable = SymbolTable()
        val resolver = ExpressionResolver(symbolTable)

        val cases =
            listOf(
                VariableDeclarationHandler() to print,
                AssignmentHandler() to print,
                PrintCallHandler() to assignment,
            )

        for ((handler, foreignNode) in cases) {
            assertFailsWith<UnknownStatementError> {
                handler.validate(foreignNode, symbolTable, resolver)
            }
        }
    }
}
