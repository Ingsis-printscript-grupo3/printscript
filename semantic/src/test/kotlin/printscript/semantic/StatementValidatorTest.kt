package printscript.semantic

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
import printscript.ast.Statement
import printscript.ast.StringLiteral
import printscript.ast.VariableDeclaration
import printscript.ast.registry.Registry
import printscript.common.LanguageVersion
import printscript.common.Position
import printscript.common.TokenType
import printscript.semantic.handler.statement.BlockHandler
import printscript.semantic.handler.statement.IfStatementHandler
import printscript.semantic.symbol.SymbolTable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

// posicion de mentira: estos tests miran el resultado, no donde ocurrio
private val AT = Position(1, 1)

class StatementValidatorTest {
    private fun validator(
        symbolTable: SymbolTable = SymbolTable(),
        version: LanguageVersion = LanguageVersion.V1_1,
    ): StatementValidator {
        val rules = SemanticRules.from(version)
        return StatementValidator(symbolTable, ExpressionResolver(symbolTable, rules), rules)
    }

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
        assertEquals("Constant 'a' must be initialized.", result.message)
    }

    @Test
    fun `declaring a variable with unsupported type fails`() {
        val result =
            validator(version = LanguageVersion.V1_0).validate(
                VariableDeclaration("a", "boolean", null),
            )

        assertIs<SemanticResult.Failure>(result)
        assertEquals("Type 'boolean' is not supported in PrintScript 1.0.", result.message)
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
        assertEquals("Cannot reassign constant 'a'.", result.message)
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
        val result = validator().validate(PrintCall(Identifier("missing")))

        assertIs<SemanticResult.Failure>(result)
    }

    @Test
    fun `a statement with no handler registered fails explicitly with the node position`() {
        val symbolTable = SymbolTable()
        val emptyRegistry = Registry<Statement, StatementValidator, SemanticResult<Unit>>()
        val validator =
            StatementValidator(
                symbolTable,
                ExpressionResolver(symbolTable, SemanticRules.from(LanguageVersion.V1_1)),
                SemanticRules.from(LanguageVersion.V1_1),
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
        val lookup = symbolTable.lookup("temp", at = AT)
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

    @Test
    fun `declaring a variable initialized with readInput infers declared type`() {
        val types = listOf("number", "string", "boolean")
        for (type in types) {
            val v = validator()
            val stmt = VariableDeclaration("x", type, ReadInput(StringLiteral("prompt:")))
            val result = v.validate(stmt)
            assertEquals(SemanticResult.Success(Unit), result)
        }
    }

    @Test
    fun `declaring a const variable initialized with readInput succeeds in 1_1`() {
        val stmt = VariableDeclaration("c", "number", ReadInput(StringLiteral("num:")), isConst = true)
        val result = validator().validate(stmt)
        assertEquals(SemanticResult.Success(Unit), result)
    }

    @Test
    fun `declaring a variable initialized with readEnv infers declared type`() {
        val types = listOf("number", "string", "boolean")
        for (type in types) {
            val v = validator()
            val stmt = VariableDeclaration("envVar", type, ReadEnv(StringLiteral("SOME_ENV")))
            val result = v.validate(stmt)
            assertEquals(SemanticResult.Success(Unit), result)
        }
    }

    @Test
    fun `assigning readInput to a declared variable infers variable type`() {
        val types = listOf("number", "string", "boolean")
        for (type in types) {
            val table = SymbolTable()
            table.define("x", type, at = AT)
            val v = validator(table)
            val assign = Assignment("x", ReadInput(StringLiteral("prompt:")))
            val result = v.validate(assign)
            assertEquals(SemanticResult.Success(Unit), result)
        }
    }

    @Test
    fun `assigning readEnv to a declared variable infers variable type`() {
        val table = SymbolTable()
        table.define("port", "number", at = AT)
        val v = validator(table)
        val assign = Assignment("port", ReadEnv(StringLiteral("PORT")))
        val result = v.validate(assign)
        assertEquals(SemanticResult.Success(Unit), result)
    }

    @Test
    fun `printing a readInput expression defaults to string and succeeds`() {
        val print = PrintCall(ReadInput(StringLiteral("Enter name:")))
        val result = validator().validate(print)
        assertEquals(SemanticResult.Success(Unit), result)
    }

    @Test
    fun `printing a readEnv expression defaults to string and succeeds`() {
        val print = PrintCall(ReadEnv(StringLiteral("USER")))
        val result = validator().validate(print)
        assertEquals(SemanticResult.Success(Unit), result)
    }

    @Test
    fun `declaring variable with readInput having non-string argument fails and propagates failure`() {
        val stmt = VariableDeclaration("x", "string", ReadInput(NumberLiteral(123.0)))
        val result = validator().validate(stmt)
        assertIs<SemanticResult.Failure>(result)
        assertEquals("'readInput' argument must be a string, found 'number'.", result.message)
    }

    @Test
    fun `assigning readInput with non-string argument fails and propagates failure`() {
        val table = SymbolTable()
        table.define("x", "string", at = AT)
        val v = validator(table)
        val assign = Assignment("x", ReadInput(BooleanLiteral(false)))
        val result = v.validate(assign)
        assertIs<SemanticResult.Failure>(result)
        assertEquals("'readInput' argument must be a string, found 'boolean'.", result.message)
    }

    @Test
    fun `printing readInput with non-string argument fails and propagates failure`() {
        val print = PrintCall(ReadInput(NumberLiteral(99.0)))
        val result = validator().validate(print)
        assertIs<SemanticResult.Failure>(result)
        assertEquals("'readInput' argument must be a string, found 'number'.", result.message)
    }

    @Test
    fun `binary addition with readInput defaults readInput to string and produces string`() {
        val binExpr = BinaryExpression(ReadInput(StringLiteral("First name: ")), TokenType.PLUS, StringLiteral("Doe"))
        val decl = VariableDeclaration("fullName", "string", binExpr)
        val result = validator().validate(decl)
        assertEquals(SemanticResult.Success(Unit), result)
    }

    @Test
    fun `declaring number variable with readInput in binary addition fails due to type mismatch`() {
        val binExpr = BinaryExpression(ReadInput(StringLiteral("A: ")), TokenType.PLUS, NumberLiteral(5.0))
        val decl = VariableDeclaration("n", "number", binExpr)
        val result = validator().validate(decl)
        assertIs<SemanticResult.Failure>(result)
        assertEquals("Incompatible types.", result.message)
    }

    @Test
    fun `default10Handlers does not include if statement or block handlers`() {
        val handlers10 = StatementValidator.default10Handlers()
        assertFalse(handlers10.any { it is IfStatementHandler })
        assertFalse(handlers10.any { it is BlockHandler })
        assertEquals(3, handlers10.size)
    }

    @Test
    fun `default11Handlers includes all 10 handlers plus if statement and block handlers`() {
        val handlers11 = StatementValidator.default11Handlers()
        assertTrue(handlers11.any { it is IfStatementHandler })
        assertTrue(handlers11.any { it is BlockHandler })
        assertEquals(5, handlers11.size)
    }

    @Test
    fun `validating if statement in 1_0 fails as unknown statement type`() {
        val ifStmt =
            IfStatement(
                BooleanLiteral(true),
                Block(emptyList()),
                null,
                Position(3, 1),
            )
        val result = validator(version = LanguageVersion.V1_0).validate(ifStmt)
        assertIs<SemanticResult.Failure>(result)
        assertEquals("Unknown statement type.", result.message)
        assertEquals(Position(3, 1), result.position)
    }

    @Test
    fun `validating block in 1_0 fails as unknown statement type`() {
        val block = Block(emptyList(), Position(5, 1))
        val result = validator(version = LanguageVersion.V1_0).validate(block)
        assertIs<SemanticResult.Failure>(result)
        assertEquals("Unknown statement type.", result.message)
        assertEquals(Position(5, 1), result.position)
    }

    @Test
    fun `defaultHandlers dispatches based on version`() {
        val handlers10 = StatementValidator.defaultHandlers(LanguageVersion.V1_0)
        val handlers11 = StatementValidator.defaultHandlers(LanguageVersion.V1_1)
        assertEquals(3, handlers10.size)
        assertEquals(5, handlers11.size)
    }

    @Test
    fun `validating const declaration in 1_0 fails as unsupported`() {
        val constDecl = VariableDeclaration("x", "number", NumberLiteral(10.0), Position(4, 1), isConst = true)
        val result = validator(version = LanguageVersion.V1_0).validate(constDecl)
        assertIs<SemanticResult.Failure>(result)
        assertEquals("'const' declarations are not supported in PrintScript 1.0.", result.message)
        assertEquals(Position(4, 1), result.position)
    }
}
