package printscript.interpreter

import printscript.ast.Assignment
import printscript.ast.Block
import printscript.ast.BooleanLiteral
import printscript.ast.IfStatement
import printscript.ast.NumberLiteral
import printscript.ast.PrintCall
import printscript.ast.ReadEnv
import printscript.ast.ReadInput
import printscript.ast.Statement
import printscript.ast.StringLiteral
import printscript.ast.VariableDeclaration
import printscript.common.Position
import printscript.interpreter.output.BucketOutput
import printscript.interpreter.plugin.InterpreterContext
import printscript.interpreter.plugin.statement.AssignmentInterpreter
import printscript.interpreter.plugin.statement.BlockInterpreter
import printscript.interpreter.plugin.statement.IfStatementInterpreter
import printscript.interpreter.plugin.statement.PrintCallInterpreter
import printscript.interpreter.plugin.statement.VariableDeclarationInterpreter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// posicion fija: estos tests verifican el error, no su ubicacion
private val AT = Position(1, 1)

class StatementHandlersTest {
    private val cases =
        listOf(
            HandlerCase(
                VariableDeclarationInterpreter(),
                VariableDeclaration("x", "number", NumberLiteral(1.0)),
                Assignment("x", NumberLiteral(1.0)),
            ),
            HandlerCase(
                AssignmentInterpreter(),
                Assignment("x", NumberLiteral(1.0)),
                VariableDeclaration("x", "number", NumberLiteral(1.0), isConst = true),
            ),
            HandlerCase(PrintCallInterpreter(BucketOutput()), PrintCall(StringLiteral("hi")), Block(emptyList())),
            HandlerCase(
                IfStatementInterpreter(),
                IfStatement(BooleanLiteral(true), Block(emptyList()), null),
                Block(emptyList()),
            ),
            HandlerCase(BlockInterpreter(), Block(emptyList()), PrintCall(StringLiteral("hi"))),
        )

    @Test
    fun `IfStatementInterpreter runs the then branch when the condition is true`() {
        val thenBranch = Block(listOf(PrintCall(StringLiteral("then"))))
        val elseBranch = Block(listOf(PrintCall(StringLiteral("else"))))
        val interpreter = StubInterpreter { BooleanValue(true) }
        val ctx = InterpreterContext(Environment(), interpreter)

        IfStatementInterpreter().handle(IfStatement(BooleanLiteral(true), thenBranch, elseBranch), ctx)

        assertEquals(listOf<Statement>(thenBranch), interpreter.interpreted)
    }

    @Test
    fun `IfStatementInterpreter runs the else branch when the condition is false`() {
        val thenBranch = Block(listOf(PrintCall(StringLiteral("then"))))
        val elseBranch = Block(listOf(PrintCall(StringLiteral("else"))))
        val interpreter = StubInterpreter { BooleanValue(false) }
        val ctx = InterpreterContext(Environment(), interpreter)

        IfStatementInterpreter().handle(IfStatement(BooleanLiteral(false), thenBranch, elseBranch), ctx)

        assertEquals(listOf<Statement>(elseBranch), interpreter.interpreted)
    }

    @Test
    fun `IfStatementInterpreter runs nothing when the condition is false and there is no else`() {
        val thenBranch = Block(listOf(PrintCall(StringLiteral("then"))))
        val interpreter = StubInterpreter { BooleanValue(false) }
        val ctx = InterpreterContext(Environment(), interpreter)

        IfStatementInterpreter().handle(IfStatement(BooleanLiteral(false), thenBranch, null), ctx)

        assertEquals(emptyList(), interpreter.interpreted)
    }

    @Test
    fun `IfStatementInterpreter throws ConditionTypeError if the condition is not boolean`() {
        val ctx = contextEvaluatingTo(NumberValue(1.0))

        val error =
            assertFailsWith<ConditionTypeError> {
                IfStatementInterpreter().handle(IfStatement(NumberLiteral(1.0), Block(emptyList()), null), ctx)
            }

        assertEquals("number", error.actualType)
    }

    @Test
    fun `BlockInterpreter discards the variables declared inside it`() {
        val env = Environment()
        val declareY: (Statement) -> Unit = { env.declare("y", NumberValue(1.0), at = AT) }
        val ctx = InterpreterContext(env, StubInterpreter(declareY) { NumberValue(1.0) })

        BlockInterpreter().handle(Block(listOf(PrintCall(StringLiteral("inside")))), ctx)

        assertFailsWith<UndeclaredVariableError> { env.lookup("y", at = AT) }
    }

    @Test
    fun `BlockInterpreter exits the scope even if a statement fails`() {
        val env = Environment()
        val declareYAndFail: (Statement) -> Unit = {
            env.declare("y", NumberValue(1.0), at = AT)
            throw UndeclaredVariableError("z", AT)
        }
        val ctx = InterpreterContext(env, StubInterpreter(declareYAndFail) { NumberValue(1.0) })

        assertFailsWith<UndeclaredVariableError> {
            BlockInterpreter().handle(Block(listOf(PrintCall(StringLiteral("inside")))), ctx)
        }

        assertFailsWith<UndeclaredVariableError> { env.lookup("y", at = AT) }
    }

    @Test
    fun `VariableDeclarationInterpreter converts readInput text to the declared number`() {
        val ctx = contextEvaluatingTo(StringValue("99"))

        VariableDeclarationInterpreter().handle(VariableDeclaration("n", "number", ReadInput(StringLiteral("p"))), ctx)

        assertEquals(NumberValue(99.0), ctx.env.lookup("n", at = AT))
    }

    @Test
    fun `VariableDeclarationInterpreter converts readEnv text to the declared boolean`() {
        val ctx = contextEvaluatingTo(StringValue("true"))

        VariableDeclarationInterpreter().handle(VariableDeclaration("b", "boolean", ReadEnv(StringLiteral("B"))), ctx)

        assertEquals(BooleanValue(true), ctx.env.lookup("b", at = AT))
    }

    @Test
    fun `VariableDeclarationInterpreter declares a const`() {
        val ctx = contextEvaluatingTo(NumberValue(10.0))
        val declaration = VariableDeclaration("c", "number", NumberLiteral(10.0), isConst = true)

        VariableDeclarationInterpreter().handle(declaration, ctx)

        assertEquals(NumberValue(10.0), ctx.env.lookup("c", at = AT))
    }

    @Test
    fun `AssignmentInterpreter cannot reassign a const`() {
        val ctx = contextEvaluatingTo(NumberValue(20.0))
        ctx.env.declare("c", NumberValue(10.0), type = "number", isConst = true, at = AT)

        assertFailsWith<CannotAssignToConstError> {
            AssignmentInterpreter().handle(Assignment("c", NumberLiteral(20.0)), ctx)
        }
    }

    @Test
    fun `AssignmentInterpreter converts readInput text to the declared type`() {
        val ctx = contextEvaluatingTo(StringValue("99"))
        ctx.env.declare("n", NumberValue(1.0), type = "number", at = AT)

        AssignmentInterpreter().handle(Assignment("n", ReadInput(StringLiteral("p"))), ctx)

        assertEquals(NumberValue(99.0), ctx.env.lookup("n", at = AT))
    }

    @Test
    fun `statement handlers apply only to their own node`() {
        for (case in cases) {
            assertTrue(case.handler.applies(case.own), case.name)
            assertFalse(case.handler.applies(case.foreign), case.name)
        }
    }

    // en ejecucion normal no pasa: el Interpreter consulta applies antes de llamar a handle
    @Test
    fun `statement handlers reject nodes that are not theirs`() {
        for (case in cases) {
            assertFailsWith<UnknownStatementError>(case.name) {
                case.handler.handle(case.foreign, unusedContext())
            }
        }
    }
}
