package printscript.semantic

import printscript.ast.Assignment
import printscript.ast.Block
import printscript.ast.BooleanLiteral
import printscript.ast.Identifier
import printscript.ast.IfStatement
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
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class StatementValidatorTest {
    private fun validator(
        symbolTable: SymbolTable = SymbolTable(),
        version: LanguageVersion = LanguageVersion.V1_1,
    ) = StatementValidator(symbolTable, ExpressionResolver(symbolTable, version), version)

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
    fun `declaring a const variable with initializer succeeds in 1_1`() {
        val result = validator().validate(VariableDeclaration("a", "number", NumberLiteral(1.0), isConst = true))

        assertEquals(SemanticResult.Success(Unit), result)
    }

    @Test
    fun `declaring a const variable without initializer fails`() {
        val result = validator().validate(VariableDeclaration("a", "number", null, isConst = true))

        assertIs<SemanticResult.Failure>(result)
        assertEquals("Semantic Error: Constant 'a' must be initialized.", result.message)
    }

    @Test
    fun `declaring a const variable under version 1_0 fails`() {
        val result =
            validator(version = LanguageVersion.V1_0).validate(
                VariableDeclaration("a", "number", NumberLiteral(1.0), isConst = true),
            )

        assertIs<SemanticResult.Failure>(result)
        assertEquals("Semantic Error: 'const' declarations are not supported in PrintScript 1.0.", result.message)
    }

    @Test
    fun `declaring a variable with unsupported type fails`() {
        val result =
            validator(version = LanguageVersion.V1_0).validate(
                VariableDeclaration("a", "boolean", null),
            )

        assertIs<SemanticResult.Failure>(result)
        assertEquals("Semantic Error: Type 'boolean' is not supported in PrintScript 1.0.", result.message)
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
    fun `reassigning a const variable fails`() {
        val symbolTable = SymbolTable()
        val validator = validator(symbolTable)
        validator.validate(VariableDeclaration("a", "number", NumberLiteral(1.0), isConst = true))

        val result = validator.validate(Assignment("a", NumberLiteral(2.0)))

        assertIs<SemanticResult.Failure>(result)
        assertEquals("Semantic Error: Cannot reassign constant 'a'.", result.message)
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
            StatementValidator(
                symbolTable,
                ExpressionResolver(symbolTable, LanguageVersion.V1_1),
                LanguageVersion.V1_1,
                emptyRegistry,
            )
        val node = PrintCall(NumberLiteral(1.0), Position(9, 1))

        val result = validator.validate(node)

        assertIs<SemanticResult.Failure>(result)
        assertEquals(node.position, result.position)
    }

    @Test
    fun `validating if statement with boolean literal condition succeeds`() {
        val ifStmt =
            IfStatement(
                BooleanLiteral(true),
                Block(listOf(PrintCall(StringLiteral("inside then")))),
                null,
            )

        val result = validator().validate(ifStmt)

        assertEquals(SemanticResult.Success(Unit), result)
    }

    @Test
    fun `validating if statement with boolean identifier condition succeeds`() {
        val symbolTable = SymbolTable()
        val validator = validator(symbolTable)
        validator.validate(VariableDeclaration("flag", "boolean", BooleanLiteral(true)))

        val ifStmt =
            IfStatement(
                Identifier("flag"),
                Block(listOf(PrintCall(StringLiteral("inside then")))),
                null,
            )

        val result = validator.validate(ifStmt)

        assertEquals(SemanticResult.Success(Unit), result)
    }

    @Test
    fun `validating if statement with non boolean condition fails for number`() {
        val symbolTable = SymbolTable()
        val validator = validator(symbolTable)
        validator.validate(VariableDeclaration("a", "number", NumberLiteral(21.0)))

        val ifStmt =
            IfStatement(
                Identifier("a"),
                Block(listOf(PrintCall(StringLiteral("fail")))),
                null,
                Position(2, 1),
            )

        val result = validator.validate(ifStmt)

        assertIs<SemanticResult.Failure>(result)
        assertTrue(result.message.contains("must be a boolean expression"))
        assertTrue(result.message.contains("number"))
    }

    @Test
    fun `validating if statement with non boolean condition fails for string literal`() {
        val ifStmt =
            IfStatement(
                StringLiteral("hello"),
                Block(emptyList()),
                null,
            )

        val result = validator().validate(ifStmt)

        assertIs<SemanticResult.Failure>(result)
        assertTrue(result.message.contains("must be a boolean expression"))
        assertTrue(result.message.contains("string"))
    }

    @Test
    fun `validating if statement with undeclared identifier in condition propagates failure`() {
        val ifStmt =
            IfStatement(
                Identifier("missing"),
                Block(emptyList()),
                null,
            )

        val result = validator().validate(ifStmt)

        assertIs<SemanticResult.Failure>(result)
        assertTrue(result.message.contains("not declared"))
    }

    @Test
    fun `validating if statement under version 1_0 fails`() {
        val ifStmt =
            IfStatement(
                BooleanLiteral(true),
                Block(emptyList()),
                null,
                Position(1, 1),
            )

        val result = validator(version = LanguageVersion.V1_0).validate(ifStmt)

        assertIs<SemanticResult.Failure>(result)
        assertEquals(Position(1, 1), result.position)
        assertTrue(result.message.contains("'if' statements are not supported in PrintScript 1.0"))
    }

    @Test
    fun `variables declared inside thenBranch are not visible outside the if statement`() {
        val symbolTable = SymbolTable()
        val validator = validator(symbolTable)

        val ifStmt =
            IfStatement(
                BooleanLiteral(true),
                Block(listOf(VariableDeclaration("inner", "number", NumberLiteral(10.0)))),
                null,
            )

        val ifResult = validator.validate(ifStmt)
        assertEquals(SemanticResult.Success(Unit), ifResult)

        val outsideAssign = validator.validate(Assignment("inner", NumberLiteral(20.0)))
        assertIs<SemanticResult.Failure>(outsideAssign)
        assertTrue(outsideAssign.message.contains("not declared"))
    }

    @Test
    fun `variables declared inside elseBranch are not visible outside the if statement`() {
        val symbolTable = SymbolTable()
        val validator = validator(symbolTable)

        val ifStmt =
            IfStatement(
                BooleanLiteral(false),
                Block(emptyList()),
                Block(listOf(VariableDeclaration("elseVar", "string", StringLiteral("hi")))),
            )

        val ifResult = validator.validate(ifStmt)
        assertEquals(SemanticResult.Success(Unit), ifResult)

        val outsideAssign = validator.validate(Assignment("elseVar", StringLiteral("bye")))
        assertIs<SemanticResult.Failure>(outsideAssign)
        assertTrue(outsideAssign.message.contains("not declared"))
    }

    @Test
    fun `variables declared inside thenBranch are not visible in elseBranch`() {
        val symbolTable = SymbolTable()
        val validator = validator(symbolTable)

        val ifStmt =
            IfStatement(
                BooleanLiteral(true),
                Block(listOf(VariableDeclaration("x", "number", NumberLiteral(1.0)))),
                Block(listOf(PrintCall(Identifier("x")))),
            )

        val result = validator.validate(ifStmt)

        assertIs<SemanticResult.Failure>(result)
        assertTrue(result.message.contains("Variable 'x' not declared"))
    }

    @Test
    fun `thenBranch and elseBranch have independent scopes and can define same name with different types`() {
        val symbolTable = SymbolTable()
        val validator = validator(symbolTable)

        val ifStmt =
            IfStatement(
                BooleanLiteral(true),
                Block(listOf(VariableDeclaration("x", "number", NumberLiteral(1.0)))),
                Block(listOf(VariableDeclaration("x", "string", StringLiteral("msg")))),
            )

        val result = validator.validate(ifStmt)

        assertEquals(SemanticResult.Success(Unit), result)
    }

    @Test
    fun `thenBranch can access and shadow variables from outer scope without altering outer scope`() {
        val symbolTable = SymbolTable()
        val validator = validator(symbolTable)

        validator.validate(VariableDeclaration("x", "number", NumberLiteral(10.0)))

        val ifStmt =
            IfStatement(
                BooleanLiteral(true),
                Block(
                    listOf(
                        VariableDeclaration("x", "string", StringLiteral("shadowed")),
                        PrintCall(Identifier("x")),
                    ),
                ),
                null,
            )

        val ifResult = validator.validate(ifStmt)
        assertEquals(SemanticResult.Success(Unit), ifResult)

        // Outer scope retains original type (number)
        val validAssign = validator.validate(Assignment("x", NumberLiteral(20.0)))
        assertEquals(SemanticResult.Success(Unit), validAssign)

        val invalidAssign = validator.validate(Assignment("x", StringLiteral("fail")))
        assertIs<SemanticResult.Failure>(invalidAssign)
    }

    @Test
    fun `failing statement inside a block cleans up the scope via finally`() {
        val symbolTable = SymbolTable()
        val validator = validator(symbolTable)

        val block =
            Block(
                listOf(
                    VariableDeclaration("temp", "number", NumberLiteral(1.0)),
                    Assignment("temp", StringLiteral("incompatible")),
                ),
            )

        val result = validator.validate(block)

        assertIs<SemanticResult.Failure>(result)

        // Scope was properly exited: temp variable no longer exists
        val lookup = symbolTable.lookupVariable("temp")
        assertIs<SemanticResult.Failure>(lookup)

        // We are at root scope: attempting to exit root scope throws exception
        assertFailsWith<IllegalStateException> {
            symbolTable.exitScope()
        }
    }

    @Test
    fun `nested if statements succeed and cleanly unwind all scopes`() {
        val symbolTable = SymbolTable()
        val validator = validator(symbolTable)

        val nestedIf =
            IfStatement(
                BooleanLiteral(true),
                Block(
                    listOf(
                        VariableDeclaration("outerVar", "number", NumberLiteral(1.0)),
                        IfStatement(
                            BooleanLiteral(true),
                            Block(listOf(VariableDeclaration("innerVar", "string", StringLiteral("deep")))),
                            null,
                        ),
                    ),
                ),
                null,
            )

        val result = validator.validate(nestedIf)
        assertEquals(SemanticResult.Success(Unit), result)

        assertIs<SemanticResult.Failure>(validator.validate(Assignment("innerVar", StringLiteral("oops"))))
        assertIs<SemanticResult.Failure>(validator.validate(Assignment("outerVar", NumberLiteral(2.0))))
        assertFailsWith<IllegalStateException> { symbolTable.exitScope() }
    }

    @Test
    fun `standalone block enters and exits scope`() {
        val symbolTable = SymbolTable()
        val validator = validator(symbolTable)

        val block = Block(listOf(VariableDeclaration("b", "number", NumberLiteral(5.0))))
        val result = validator.validate(block)

        assertEquals(SemanticResult.Success(Unit), result)
        assertIs<SemanticResult.Failure>(validator.validate(Assignment("b", NumberLiteral(10.0))))
    }
}
