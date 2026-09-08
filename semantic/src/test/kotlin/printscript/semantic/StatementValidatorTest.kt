package printscript.semantic

import printscript.ast.Assignment
import printscript.ast.NumberLiteral
import printscript.ast.PrintCall
import printscript.ast.Statement
import printscript.ast.StringLiteral
import printscript.ast.VariableDeclaration
import printscript.ast.registry.Registry
import printscript.common.LanguageVersion
import printscript.common.Position
import printscript.semantic.symbol.SymbolTable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class StatementValidatorTest {
    private fun validator(symbolTable: SymbolTable = SymbolTable()) =
        StatementValidator(symbolTable, ExpressionResolver(symbolTable, LanguageVersion.V1_1))

    @Test
    fun `declaring a variable without an initializer succeeds`() {
        val result = validator().validate(VariableDeclaration("a", "number", null))

        assertEquals(SemanticResult.Success(Unit), result)
    }

    @Test
    fun `declaring a variable with a matching initializer succeeds`() {
        val result = validator().validate(VariableDeclaration("a", "number", NumberLiteral(1.0)))

        assertEquals(SemanticResult.Success(Unit), result)
    }

    @Test
    fun `declaring a variable with a mismatched initializer fails`() {
        val result = validator().validate(VariableDeclaration("a", "number", StringLiteral("x")))

        assertIs<SemanticResult.Failure>(result)
    }

    @Test
    fun `declaring the same variable twice fails`() {
        val symbolTable = SymbolTable()
        val validator = validator(symbolTable)
        validator.validate(VariableDeclaration("a", "number", null))

        val result = validator.validate(VariableDeclaration("a", "number", null))

        assertIs<SemanticResult.Failure>(result)
    }

    @Test
    fun `assigning a compatible value to a declared variable succeeds`() {
        val symbolTable = SymbolTable()
        val validator = validator(symbolTable)
        validator.validate(VariableDeclaration("a", "number", null))

        val result = validator.validate(Assignment("a", NumberLiteral(2.0)))

        assertEquals(SemanticResult.Success(Unit), result)
    }

    @Test
    fun `assigning to an undeclared variable fails`() {
        val result = validator().validate(Assignment("a", NumberLiteral(2.0)))

        assertIs<SemanticResult.Failure>(result)
    }

    @Test
    fun `assigning an incompatible type fails`() {
        val symbolTable = SymbolTable()
        val validator = validator(symbolTable)
        validator.validate(VariableDeclaration("a", "number", null))

        val result = validator.validate(Assignment("a", StringLiteral("x")))

        assertIs<SemanticResult.Failure>(result)
    }

    @Test
    fun `printing a well typed expression succeeds`() {
        val result = validator().validate(PrintCall(NumberLiteral(1.0)))

        assertEquals(SemanticResult.Success(Unit), result)
    }

    @Test
    fun `printing an expression that fails to resolve propagates the failure`() {
        val result = validator().validate(PrintCall(printscript.ast.Identifier("missing")))

        assertIs<SemanticResult.Failure>(result)
    }

    @Test
    fun `a statement with no handler registered fails explicitly with the node position`() {
        val symbolTable = SymbolTable()
        val emptyRegistry = Registry<Statement, StatementValidator, SemanticResult<Unit>>()
        val validator =
            StatementValidator(symbolTable, ExpressionResolver(symbolTable, LanguageVersion.V1_1), emptyRegistry)
        val node = PrintCall(NumberLiteral(1.0), Position(9, 1))

        val result = validator.validate(node)

        assertIs<SemanticResult.Failure>(result)
        assertEquals(node.position, result.position)
    }
}
