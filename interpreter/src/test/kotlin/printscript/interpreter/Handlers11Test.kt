package printscript.interpreter

import printscript.ast.Assignment
import printscript.ast.Block
import printscript.ast.BooleanLiteral
import printscript.ast.Expression
import printscript.ast.Identifier
import printscript.ast.IfStatement
import printscript.ast.NumberLiteral
import printscript.ast.PrintCall
import printscript.ast.ReadEnv
import printscript.ast.ReadInput
import printscript.ast.Statement
import printscript.ast.StringLiteral
import printscript.ast.VariableDeclaration
import printscript.common.Position
import printscript.interpreter.env.MapEnvProvider
import printscript.interpreter.input.QueueInput
import printscript.interpreter.output.BucketOutput
import printscript.interpreter.plugin.InterpreterContext
import printscript.interpreter.plugin.expression.BooleanLiteralEvaluator
import printscript.interpreter.plugin.expression.NumberLiteralEvaluator
import printscript.interpreter.plugin.expression.ReadEnvEvaluator
import printscript.interpreter.plugin.expression.ReadInputEvaluator
import printscript.interpreter.plugin.expression.StringLiteralEvaluator
import printscript.interpreter.plugin.statement.AssignmentInterpreter
import printscript.interpreter.plugin.statement.BlockInterpreter
import printscript.interpreter.plugin.statement.IfStatementInterpreter
import printscript.interpreter.plugin.statement.VariableDeclarationInterpreter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// posicion fija: estos tests verifican el error, no su ubicacion
private val AT = Position(1, 1)

// para los handlers que no usan el contexto o que fallan antes de usarlo
private fun unusedContext() = InterpreterContext(Environment(), StubInterpreter { error("no deberia evaluar nada") })

// cualquier expresion que el handler pida evaluar devuelve value
private fun contextEvaluatingTo(value: Value) = InterpreterContext(Environment(), StubInterpreter { value })

// aisla al handler bajo prueba del resto de los evaluadores
private class StubInterpreter(
    private val evaluator: (Expression) -> Value,
) : InterpreterInterface {
    override fun interpret(statements: Iterator<Statement>) = Unit

    override fun evaluate(expression: Expression): Value = evaluator(expression)
}

class Handlers11Test {
    private fun createTestInterpreter(
        output: BucketOutput = BucketOutput(),
        input: QueueInput = QueueInput(),
        env: MapEnvProvider = MapEnvProvider(),
    ): Interpreter {
        val statements = InterpreterFactory.default11StatementInterpreters(output)
        val expressions =
            listOf(
                BooleanLiteralEvaluator(),
                NumberLiteralEvaluator(),
                StringLiteralEvaluator(),
                ReadInputEvaluator(input),
                ReadEnvEvaluator(env),
            )
        return Interpreter(statements, expressions)
    }

    @Test
    fun `BooleanLiteralEvaluator evaluates boolean literals`() {
        val evaluator = BooleanLiteralEvaluator()
        val ctx = unusedContext()

        assertEquals(BooleanValue(true), evaluator.handle(BooleanLiteral(true), ctx))
        assertEquals(BooleanValue(false), evaluator.handle(BooleanLiteral(false), ctx))
        assertTrue(evaluator.applies(BooleanLiteral(true)))
        assertFalse(evaluator.applies(StringLiteral("hi")))

        assertFailsWith<UnknownExpressionError> {
            evaluator.handle(StringLiteral("hi"), ctx)
        }
    }

    @Test
    fun `ReadInputEvaluator reads from input provider`() {
        val evaluator = ReadInputEvaluator(QueueInput("user_input"))
        val ctx = contextEvaluatingTo(StringValue("prompt: "))

        val result = evaluator.handle(ReadInput(StringLiteral("prompt: ")), ctx)

        assertEquals(StringValue("user_input"), result)
        assertTrue(evaluator.applies(ReadInput(StringLiteral("p"))))
        assertFalse(evaluator.applies(StringLiteral("p")))
        assertFailsWith<UnknownExpressionError> {
            evaluator.handle(StringLiteral("p"), ctx)
        }
    }

    @Test
    fun `ReadInputEvaluator throws MissingInputError at the readInput position when provider has no data`() {
        val evaluator = ReadInputEvaluator(QueueInput())
        val ctx = contextEvaluatingTo(StringValue("name:"))
        val position = Position(3, 7)

        val error =
            assertFailsWith<MissingInputError> {
                evaluator.handle(ReadInput(StringLiteral("name:"), position), ctx)
            }

        assertEquals(position, error.start)
    }

    @Test
    fun `ReadInputEvaluator accepts an empty string as a valid input`() {
        val evaluator = ReadInputEvaluator(QueueInput(""))
        val ctx = contextEvaluatingTo(StringValue("name:"))

        val result = evaluator.handle(ReadInput(StringLiteral("name:")), ctx)

        assertEquals(StringValue(""), result)
    }

    @Test
    fun `ReadEnvEvaluator reads existing environment variables`() {
        val evaluator = ReadEnvEvaluator(MapEnvProvider("MY_VAR" to "my_value"))
        val ctx = contextEvaluatingTo(StringValue("MY_VAR"))

        val result = evaluator.handle(ReadEnv(StringLiteral("MY_VAR")), ctx)

        assertEquals(StringValue("my_value"), result)
        assertTrue(evaluator.applies(ReadEnv(StringLiteral("x"))))
        assertFalse(evaluator.applies(StringLiteral("x")))
        assertFailsWith<UnknownExpressionError> {
            evaluator.handle(StringLiteral("x"), ctx)
        }
    }

    @Test
    fun `ReadEnvEvaluator throws EnvVariableNotFoundError if variable does not exist`() {
        val evaluator = ReadEnvEvaluator(MapEnvProvider())
        val ctx = contextEvaluatingTo(StringValue("UNKNOWN_VAR"))

        val error =
            assertFailsWith<EnvVariableNotFoundError> {
                evaluator.handle(ReadEnv(StringLiteral("UNKNOWN_VAR")), ctx)
            }

        assertEquals("UNKNOWN_VAR", error.name)
    }

    @Test
    fun `IfStatementInterpreter executes thenBranch when condition is true`() {
        val output = BucketOutput()
        val interpreter = createTestInterpreter(output = output)
        val stmt =
            IfStatement(
                condition = BooleanLiteral(true),
                thenBranch = Block(listOf(PrintCall(StringLiteral("inside then")))),
                elseBranch = Block(listOf(PrintCall(StringLiteral("inside else")))),
            )

        interpreter.interpret(stmt)

        assertEquals(listOf("inside then"), output.lines())
    }

    @Test
    fun `IfStatementInterpreter executes elseBranch when condition is false`() {
        val output = BucketOutput()
        val interpreter = createTestInterpreter(output = output)
        val stmt =
            IfStatement(
                condition = BooleanLiteral(false),
                thenBranch = Block(listOf(PrintCall(StringLiteral("inside then")))),
                elseBranch = Block(listOf(PrintCall(StringLiteral("inside else")))),
            )

        interpreter.interpret(stmt)

        assertEquals(listOf("inside else"), output.lines())
    }

    @Test
    fun `IfStatementInterpreter does nothing when condition is false and elseBranch is null`() {
        val output = BucketOutput()
        val interpreter = createTestInterpreter(output = output)
        val stmt =
            IfStatement(
                condition = BooleanLiteral(false),
                thenBranch = Block(listOf(PrintCall(StringLiteral("inside then")))),
                elseBranch = null,
            )

        interpreter.interpret(stmt)

        assertTrue(output.lines().isEmpty())
    }

    @Test
    fun `IfStatementInterpreter throws ConditionTypeError if condition is not boolean`() {
        val interpreter =
            Interpreter(
                statementInterpreters = listOf(IfStatementInterpreter()),
                expressionEvaluators = listOf(NumberLiteralEvaluator()),
            )
        val stmt =
            IfStatement(
                condition = NumberLiteral(1.0),
                thenBranch = Block(emptyList()),
                elseBranch = null,
            )

        val error =
            assertFailsWith<ConditionTypeError> {
                interpreter.interpret(stmt)
            }

        assertEquals("number", error.actualType)
    }

    @Test
    fun `IfStatementInterpreter rejects foreign nodes`() {
        val interpreter = IfStatementInterpreter()
        val ctx = unusedContext()

        assertTrue(interpreter.applies(IfStatement(BooleanLiteral(true), Block(emptyList()), null)))
        assertFalse(interpreter.applies(Block(emptyList())))
        assertFailsWith<UnknownStatementError> {
            interpreter.handle(Block(emptyList()), ctx)
        }
    }

    @Test
    fun `BlockInterpreter rejects foreign nodes`() {
        val blockHandler = BlockInterpreter()
        val ctx = unusedContext()

        assertTrue(blockHandler.applies(Block(emptyList())))
        assertFalse(blockHandler.applies(PrintCall(StringLiteral("hi"))))
        assertFailsWith<UnknownStatementError> {
            blockHandler.handle(PrintCall(StringLiteral("hi")), ctx)
        }
    }

    @Test
    fun `VariableDeclaration and Assignment with const and conversion`() {
        val env = Environment()
        // readInput y readEnv devuelven texto para verificar la conversion al tipo declarado
        val ctx =
            InterpreterContext(
                env,
                StubInterpreter { expression ->
                    when (expression) {
                        is NumberLiteral -> NumberValue(expression.value)
                        is ReadInput -> StringValue("99")
                        is ReadEnv -> StringValue("true")
                        is Identifier -> env.lookup(expression.name, at = AT)
                        else -> error("el test no usa $expression")
                    }
                },
            )
        val varDecl = VariableDeclarationInterpreter()
        val assign = AssignmentInterpreter()

        varDecl.handle(VariableDeclaration("inputNum", "number", ReadInput(StringLiteral("p"))), ctx)
        varDecl.handle(VariableDeclaration("envBool", "boolean", ReadEnv(StringLiteral("B"))), ctx)
        varDecl.handle(VariableDeclaration("c", "number", NumberLiteral(10.0), isConst = true), ctx)
        varDecl.handle(VariableDeclaration("mutableNum", "number", NumberLiteral(1.0)), ctx)
        assign.handle(Assignment("mutableNum", ReadInput(StringLiteral("p"))), ctx)

        assertEquals(NumberValue(99.0), env.lookup("inputNum", at = AT))
        assertEquals(BooleanValue(true), env.lookup("envBool", at = AT))
        assertEquals(NumberValue(10.0), env.lookup("c", at = AT))
        assertEquals(NumberValue(99.0), env.lookup("mutableNum", at = AT))
        assertFailsWith<CannotAssignToConstError> {
            assign.handle(Assignment("c", NumberLiteral(20.0)), ctx)
        }
    }

    @Test
    fun `statement plugins reject foreign nodes`() {
        val varDecl = VariableDeclarationInterpreter()
        val assign = AssignmentInterpreter()
        val ctx = unusedContext()
        val letNode = VariableDeclaration("x", "number", NumberLiteral(1.0), isConst = false)
        val constNode = VariableDeclaration("y", "number", NumberLiteral(2.0), isConst = true)
        val assignNode = Assignment("x", NumberLiteral(3.0))

        assertTrue(varDecl.applies(letNode))
        assertTrue(varDecl.applies(constNode))
        assertFalse(varDecl.applies(assignNode))
        assertTrue(assign.applies(assignNode))
        assertFalse(assign.applies(letNode))
        assertFalse(assign.applies(constNode))
        assertFailsWith<UnknownStatementError> { varDecl.handle(assignNode, ctx) }
        assertFailsWith<UnknownStatementError> { assign.handle(letNode, ctx) }
        assertFailsWith<UnknownStatementError> { assign.handle(constNode, ctx) }
    }
}
