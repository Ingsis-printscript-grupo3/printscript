package printscript.semantic

import printscript.ast.Assignment
import printscript.ast.Block
import printscript.ast.NumberLiteral
import printscript.ast.PrintCall
import printscript.ast.Statement
import printscript.ast.StringLiteral
import printscript.ast.VariableDeclaration
import printscript.ast.registry.Registry
import printscript.common.LanguageVersion
import printscript.common.Position
import printscript.semantic.handler.statement.BlockHandler
import printscript.semantic.handler.statement.IfStatementHandler
import printscript.semantic.symbol.SymbolTable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

// posicion fija: estos tests verifican el resultado, no su ubicacion
private val AT = Position(1, 1)

// casos que el codigo fuente no alcanza: bloques sueltos, registros vacios y el estado de la tabla de simbolos
class StatementValidatorTest {
    private fun validator(
        symbolTable: SymbolTable = SymbolTable(),
        version: LanguageVersion = LanguageVersion.V1_1,
    ): StatementValidator = StatementValidator(symbolTable, ExpressionResolver(symbolTable, version), version)

    @Test
    fun `a statement with no handler registered fails explicitly with the node position`() {
        val symbolTable = SymbolTable()
        val emptyRegistry = Registry<Statement, StatementValidator, SemanticResult<Unit>>()
        val validator =
            StatementValidator(
                symbolTable,
                ExpressionResolver(symbolTable, LanguageVersion.V1_1),
                LanguageVersion.V1_1,
                emptyRegistry,
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

    @Test
    fun `validating a block in 1_0 fails as unknown statement type`() {
        val block = Block(emptyList(), Position(5, 1))

        val result = validator(version = LanguageVersion.V1_0).validate(block)

        val failure = assertIs<SemanticResult.Failure>(result)
        assertEquals("Unknown statement type.", failure.message)
        assertEquals(Position(5, 1), failure.position)
    }

    @Test
    fun `default10Handlers does not include if statement or block handlers`() {
        val handlers10 = StatementValidator.default10Handlers()

        assertFalse(handlers10.any { it is IfStatementHandler })
        assertFalse(handlers10.any { it is BlockHandler })
        assertEquals(3, handlers10.size)
    }

    @Test
    fun `default11Handlers adds the if statement and block handlers`() {
        val handlers11 = StatementValidator.default11Handlers()

        assertTrue(handlers11.any { it is IfStatementHandler })
        assertTrue(handlers11.any { it is BlockHandler })
        assertEquals(5, handlers11.size)
    }

    @Test
    fun `defaultHandlers dispatches based on version`() {
        val handlers10 = StatementValidator.defaultHandlers(LanguageVersion.V1_0)
        val handlers11 = StatementValidator.defaultHandlers(LanguageVersion.V1_1)

        assertEquals(3, handlers10.size)
        assertEquals(5, handlers11.size)
    }
}
