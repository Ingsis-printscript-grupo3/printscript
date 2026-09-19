package printscript.semantic

import printscript.ast.Assignment
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
import printscript.common.LanguageVersion
import printscript.common.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SemanticAnalyzerTest {
    @Test
    fun `yields a success per well typed statement`() {
        val statements =
            listOf(
                VariableDeclaration("x", "number", NumberLiteral(1.0)),
                PrintCall(Identifier("x")),
            )

        val results = SemanticAnalyzer(LanguageVersion.V1_1).analyze(statements.iterator()).asSequence().toList()

        assertEquals(2, results.size)
        results.forEach { assertIs<SemanticResult.Success<*>>(it) }
    }

    @Test
    fun `stops at the first failure and reports the statement position`() {
        val badStatement = VariableDeclaration("x", "number", StringLiteral("oops"), Position(7, 3))
        val statements = listOf(badStatement, PrintCall(NumberLiteral(1.0)))

        val results = SemanticAnalyzer(LanguageVersion.V1_1).analyze(statements.iterator()).asSequence().toList()

        assertEquals(1, results.size)
        val failure = results.single()
        assertIs<SemanticResult.Failure>(failure)
        assertEquals(Position(7, 3), failure.position)
    }

    @Test
    fun `an empty program yields no results`() {
        val empty = emptyList<printscript.ast.Statement>().iterator()
        val results = SemanticAnalyzer(LanguageVersion.V1_1).analyze(empty).asSequence().toList()

        assertTrue(results.isEmpty())
    }

    @Test
    fun `semantic analyzer accepts const declaration with initializer in 1_1`() {
        val statements =
            listOf(
                VariableDeclaration("x", "number", NumberLiteral(10.0), isConst = true),
                PrintCall(Identifier("x")),
            )

        val results = SemanticAnalyzer(LanguageVersion.V1_1).analyze(statements.iterator()).asSequence().toList()

        assertEquals(2, results.size)
        results.forEach { assertIs<SemanticResult.Success<*>>(it) }
    }

    @Test
    fun `semantic analyzer fails when reassigning a const variable and reports statement position`() {
        val statements =
            listOf(
                VariableDeclaration("x", "number", NumberLiteral(10.0), isConst = true),
                Assignment("x", NumberLiteral(20.0), Position(2, 1)),
            )

        val results = SemanticAnalyzer(LanguageVersion.V1_1).analyze(statements.iterator()).asSequence().toList()

        assertEquals(2, results.size)
        assertIs<SemanticResult.Success<*>>(results[0])
        val failure = results[1]
        assertIs<SemanticResult.Failure>(failure)
        assertEquals(Position(2, 1), failure.position)
        assertTrue(failure.message.contains("Cannot reassign constant 'x'"))
    }

    @Test
    fun `semantic analyzer rejects unsupported type declaration in 1_0`() {
        val statements =
            listOf(
                VariableDeclaration("x", "boolean", null, Position(1, 1)),
            )

        val results = SemanticAnalyzer(LanguageVersion.V1_0).analyze(statements.iterator()).asSequence().toList()

        assertEquals(1, results.size)
        val failure = results.single()
        assertIs<SemanticResult.Failure>(failure)
        assertEquals(Position(1, 1), failure.position)
        assertTrue(failure.message.contains("Type 'boolean' is not supported in PrintScript 1.0"))
    }

    @Test
    fun `semantic analyzer accepts valid if statement in 1_1`() {
        val statements =
            listOf(
                VariableDeclaration("flag", "boolean", BooleanLiteral(true)),
                IfStatement(
                    Identifier("flag"),
                    Block(listOf(PrintCall(StringLiteral("inside")))),
                    Block(listOf(PrintCall(StringLiteral("else")))),
                ),
            )

        val results = SemanticAnalyzer(LanguageVersion.V1_1).analyze(statements.iterator()).asSequence().toList()

        assertEquals(2, results.size)
        results.forEach { assertIs<SemanticResult.Success<*>>(it) }
    }

    @Test
    fun `semantic analyzer stops and reports position on invalid if condition type`() {
        val statements =
            listOf(
                VariableDeclaration("num", "number", NumberLiteral(5.0)),
                IfStatement(
                    Identifier("num", Position(2, 5)),
                    Block(emptyList()),
                    null,
                    Position(2, 1),
                ),
                PrintCall(StringLiteral("should not reach here")),
            )

        val results = SemanticAnalyzer(LanguageVersion.V1_1).analyze(statements.iterator()).asSequence().toList()

        assertEquals(2, results.size)
        assertIs<SemanticResult.Success<*>>(results[0])
        val failure = results[1]
        assertIs<SemanticResult.Failure>(failure)
        // apunta a la condicion, no al if que la contiene
        assertEquals(Position(2, 5), failure.position)
        assertTrue(failure.message.contains("must be a boolean expression"))
    }

    @Test
    fun `variables declared inside if block are not visible to subsequent statements`() {
        val statements =
            listOf(
                IfStatement(
                    BooleanLiteral(true),
                    Block(listOf(VariableDeclaration("scopedVar", "number", NumberLiteral(42.0)))),
                    null,
                ),
                PrintCall(Identifier("scopedVar", Position(5, 9)), Position(5, 1)),
            )

        val results = SemanticAnalyzer(LanguageVersion.V1_1).analyze(statements.iterator()).asSequence().toList()

        assertEquals(2, results.size)
        assertIs<SemanticResult.Success<*>>(results[0])
        val failure = results[1]
        assertIs<SemanticResult.Failure>(failure)
        // apunta al identificador, no al println que lo contiene
        assertEquals(Position(5, 9), failure.position)
        assertTrue(failure.message.contains("Variable 'scopedVar' not declared"))
    }

    @Test
    fun `semantic analyzer validates program with readInput in declaration, assignment and println in 1_1`() {
        val statements =
            listOf(
                VariableDeclaration("name", "string", ReadInput(StringLiteral("Name: "))),
                VariableDeclaration("age", "number", ReadInput(StringLiteral("Age: "))),
                Assignment("name", ReadInput(StringLiteral("New Name: "))),
                PrintCall(ReadInput(StringLiteral("Echo: "))),
            )

        val results = SemanticAnalyzer(LanguageVersion.V1_1).analyze(statements.iterator()).asSequence().toList()

        assertEquals(4, results.size)
        results.forEach { assertIs<SemanticResult.Success<*>>(it) }
    }

    @Test
    fun `semantic analyzer validates program with readEnv in declaration, assignment and println in 1_1`() {
        val statements =
            listOf(
                VariableDeclaration("user", "string", ReadEnv(StringLiteral("USER"))),
                VariableDeclaration("port", "number", ReadEnv(StringLiteral("PORT"))),
                Assignment("user", ReadEnv(StringLiteral("NEW_USER"))),
                PrintCall(ReadEnv(StringLiteral("PATH"))),
            )

        val results = SemanticAnalyzer(LanguageVersion.V1_1).analyze(statements.iterator()).asSequence().toList()

        assertEquals(4, results.size)
        results.forEach { assertIs<SemanticResult.Success<*>>(it) }
    }

    @Test
    fun `semantic analyzer fails and stops when readInput receives invalid argument type`() {
        val statements =
            listOf(
                VariableDeclaration(
                    "x",
                    "string",
                    ReadInput(NumberLiteral(123.0, Position(2, 23))),
                    Position(2, 5),
                ),
                PrintCall(StringLiteral("unreachable")),
            )

        val results = SemanticAnalyzer(LanguageVersion.V1_1).analyze(statements.iterator()).asSequence().toList()

        assertEquals(1, results.size)
        val failure = results.single()
        assertIs<SemanticResult.Failure>(failure)
        assertEquals(Position(2, 23), failure.position)
        assertTrue(failure.message.contains("'readInput' argument must be a string, found 'number'"))
    }

    @Test
    fun `semantic analyzer preserves fine-grained position of error inside a nested block statement`() {
        val statements =
            listOf(
                IfStatement(
                    BooleanLiteral(true),
                    Block(
                        listOf(
                            VariableDeclaration("y", "number", StringLiteral("err"), Position(15, 8)),
                        ),
                    ),
                    null,
                    Position(1, 1),
                ),
            )

        val results = SemanticAnalyzer(LanguageVersion.V1_1).analyze(statements.iterator()).asSequence().toList()

        assertEquals(1, results.size)
        val failure = results.single()
        assertIs<SemanticResult.Failure>(failure)
        assertEquals(Position(15, 8), failure.position)
    }

    // los errores que nacen en la SymbolTable llegaban con Position(0, 0) y el analyzer se la parcheaba
    // con la del statement; ahora la tabla la recibe y el error nace con la posicion del nombre
    @Test
    fun `an error born in the symbol table carries the position of the name`() {
        val statements =
            listOf(
                VariableDeclaration("a", "number", NumberLiteral(1.0), Position(1, 1), namePosition = Position(1, 5)),
                VariableDeclaration("a", "number", NumberLiteral(2.0), Position(2, 1), namePosition = Position(2, 5)),
            )

        val results = SemanticAnalyzer(LanguageVersion.V1_1).analyze(statements.iterator()).asSequence().toList()

        val failure = results.last()
        assertIs<SemanticResult.Failure>(failure)
        assertTrue(failure.message.contains("already exists"))
        assertEquals(Position(2, 5), failure.position)
    }

    @Test
    fun `semantic analyzer in 1_0 rejects if statement with unknown statement type`() {
        val statements =
            listOf(
                IfStatement(
                    NumberLiteral(1.0),
                    Block(emptyList()),
                    null,
                    Position(3, 1),
                ),
            )

        val results = SemanticAnalyzer(LanguageVersion.V1_0).analyze(statements.iterator()).asSequence().toList()

        assertEquals(1, results.size)
        val failure = results.single()
        assertIs<SemanticResult.Failure>(failure)
        assertEquals("Unknown statement type.", failure.message)
        assertEquals(Position(3, 1), failure.position)
    }

    @Test
    fun `semantic analyzer in 1_0 rejects readInput with unknown expression type`() {
        val statements =
            listOf(
                VariableDeclaration(
                    "x",
                    "string",
                    ReadInput(StringLiteral("prompt:"), Position(2, 20)),
                    Position(2, 1),
                ),
            )

        val results = SemanticAnalyzer(LanguageVersion.V1_0).analyze(statements.iterator()).asSequence().toList()

        assertEquals(1, results.size)
        val failure = results.single()
        assertIs<SemanticResult.Failure>(failure)
        assertEquals("Unknown expression type.", failure.message)
        assertEquals(Position(2, 20), failure.position)
    }

    @Test
    fun `semantic analyzer in 1_0 rejects const declaration`() {
        val statements =
            listOf(
                VariableDeclaration(
                    "x",
                    "number",
                    NumberLiteral(10.0, Position(1, 17)),
                    Position(1, 1),
                    isConst = true,
                ),
            )

        val results = SemanticAnalyzer(LanguageVersion.V1_0).analyze(statements.iterator()).asSequence().toList()

        assertEquals(1, results.size)
        val failure = results.single()
        assertIs<SemanticResult.Failure>(failure)
        assertEquals("'const' declarations are not supported in PrintScript 1.0.", failure.message)
        assertEquals(Position(1, 1), failure.position)
    }
}
