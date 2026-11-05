package printscript.semantic

import printscript.ast.Assignment
import printscript.ast.Block
import printscript.ast.NumberLiteral
import printscript.ast.PrintCall
import printscript.ast.StringLiteral
import printscript.ast.VariableDeclaration
import printscript.common.Position
import printscript.semantic.symbol.SymbolTable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

// posicion fija: estos tests verifican el resultado, no su ubicacion
private val AT = Position(1, 1)

// casos que el codigo fuente no alcanza: bloques sueltos, registros vacios y el estado de la tabla de simbolos
class StatementValidatorTest {
    private fun validator(symbolTable: SymbolTable = SymbolTable()): StatementValidator =
        StatementValidator(symbolTable, ExpressionResolver(symbolTable, EXPRESSION_HANDLERS), STATEMENT_HANDLERS)

    @Test
    fun `a statement with no handler registered fails explicitly with the node position`() {
        val symbolTable = SymbolTable()
        val validator =
            StatementValidator(
                symbolTable,
                ExpressionResolver(symbolTable, EXPRESSION_HANDLERS),
                handlers = emptyList(),
            )
        val node = PrintCall(NumberLiteral(1.0), Position(9, 1))

        val result = validator.validate(node)

        val failure = assertIs<SemanticResult.Failure>(result)
        assertEquals(node.position, failure.position)
    }

    // el parser ya rechaza un const sin valor, pero el semantico tiene su propia regla
    @Test
    fun `declaring a const without an initializer fails`() {
        val declaration = VariableDeclaration("a", "number", null, isConst = true)

        val result = validator().validate(declaration)

        val failure = assertIs<SemanticResult.Failure>(result)
        assertEquals("Constant 'a' must be initialized.", failure.message)
    }

    @Test
    fun `a failing statement inside a block still exits the scope`() {
        val symbolTable = SymbolTable()
        val block =
            Block(
                listOf(
                    VariableDeclaration("temp", "number", NumberLiteral(1.0)),
                    Assignment("temp", StringLiteral("incompatible")),
                ),
            )

        val result = validator(symbolTable).validate(block)

        assertIs<SemanticResult.Failure>(result)
        assertIs<SemanticResult.Failure>(symbolTable.lookup("temp", at = AT))
        assertFailsWith<IllegalStateException> { symbolTable.exitScope() }
    }

    @Test
    fun `a standalone block discards its variables when it ends`() {
        val validator = validator()
        val block = Block(listOf(VariableDeclaration("b", "number", NumberLiteral(5.0))))

        val result = validator.validate(block)

        assertEquals(SemanticResult.Success(Unit), result)
        assertIs<SemanticResult.Failure>(validator.validate(Assignment("b", NumberLiteral(10.0))))
    }
}
