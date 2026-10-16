package printscript.interpreter

import printscript.ast.Assignment
import printscript.ast.BinaryExpression
import printscript.ast.Block
import printscript.ast.BooleanLiteral
import printscript.ast.Expression
import printscript.ast.Identifier
import printscript.ast.IfStatement
import printscript.ast.NumberLiteral
import printscript.ast.PositionedNode
import printscript.ast.PrintCall
import printscript.ast.ReadEnv
import printscript.ast.ReadInput
import printscript.ast.Statement
import printscript.ast.StringLiteral
import printscript.ast.VariableDeclaration
import printscript.ast.registry.Handler
import printscript.common.Position
import printscript.common.TokenType
import printscript.interpreter.env.MapEnvProvider
import printscript.interpreter.input.QueueInput
import printscript.interpreter.output.BucketOutput
import printscript.interpreter.plugin.InterpreterContext
import printscript.interpreter.plugin.expression.BinaryExpressionEvaluator
import printscript.interpreter.plugin.expression.BooleanLiteralEvaluator
import printscript.interpreter.plugin.expression.IdentifierEvaluator
import printscript.interpreter.plugin.expression.NumberLiteralEvaluator
import printscript.interpreter.plugin.expression.ReadEnvEvaluator
import printscript.interpreter.plugin.expression.ReadInputEvaluator
import printscript.interpreter.plugin.expression.StringLiteralEvaluator
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

// para los handlers que no usan el contexto o que fallan antes de usarlo
private fun unusedContext() = InterpreterContext(Environment(), StubInterpreter { error("no deberia evaluar nada") })

// cualquier expresion que el handler pida evaluar devuelve value
private fun contextEvaluatingTo(value: Value) = InterpreterContext(Environment(), StubInterpreter { value })

// aisla al handler bajo prueba del resto del interprete y registra lo que le pide ejecutar
private class StubInterpreter(
    private val evaluator: (Expression) -> Value,
) : InterpreterInterface {
    val interpreted = mutableListOf<Statement>()

    override fun interpret(statements: Iterator<Statement>) = statements.forEach(interpreted::add)

    override fun evaluate(expression: Expression): Value = evaluator(expression)
}

private class HandlerCase<N : PositionedNode>(
    val handler: Handler<N, InterpreterContext, *>,
    val own: N,
    val foreign: N,
)

class HandlersTest {
    private val statementCases =
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

    private val expressionCases =
        listOf(
            HandlerCase(NumberLiteralEvaluator(), NumberLiteral(1.0), StringLiteral("hi")),
            HandlerCase(StringLiteralEvaluator(), StringLiteral("hi"), NumberLiteral(1.0)),
            HandlerCase(BooleanLiteralEvaluator(), BooleanLiteral(true), StringLiteral("hi")),
            HandlerCase(IdentifierEvaluator(), Identifier("x"), NumberLiteral(1.0)),
            HandlerCase(
                BinaryExpressionEvaluator(),
                BinaryExpression(NumberLiteral(1.0), TokenType.PLUS, NumberLiteral(2.0)),
                NumberLiteral(1.0),
            ),
            HandlerCase(ReadInputEvaluator(QueueInput()), ReadInput(StringLiteral("p")), StringLiteral("p")),
            HandlerCase(ReadEnvEvaluator(MapEnvProvider()), ReadEnv(StringLiteral("x")), StringLiteral("x")),
        )

    @Test
    fun `BooleanLiteralEvaluator evaluates boolean literals`() {
        val evaluator = BooleanLiteralEvaluator()
        val ctx = unusedContext()

        val trueValue = evaluator.handle(BooleanLiteral(true), ctx)
        val falseValue = evaluator.handle(BooleanLiteral(false), ctx)

        assertEquals(BooleanValue(true), trueValue)
        assertEquals(BooleanValue(false), falseValue)
    }

    @Test
    fun `ReadInputEvaluator returns the next input`() {
        val evaluator = ReadInputEvaluator(QueueInput("user_input"))
        val ctx = contextEvaluatingTo(StringValue("prompt: "))

        val result = evaluator.handle(ReadInput(StringLiteral("prompt: ")), ctx)

        assertEquals(StringValue("user_input"), result)
    }

    @Test
    fun `ReadInputEvaluator accepts an empty string as a valid input`() {
        val evaluator = ReadInputEvaluator(QueueInput(""))
        val ctx = contextEvaluatingTo(StringValue("name:"))

        val result = evaluator.handle(ReadInput(StringLiteral("name:")), ctx)

        assertEquals(StringValue(""), result)
    }

    @Test
    fun `ReadInputEvaluator throws MissingInputError at the readInput position when there is no input`() {
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
    fun `ReadEnvEvaluator returns the value of the variable`() {
        val evaluator = ReadEnvEvaluator(MapEnvProvider("MY_VAR" to "my_value"))
        val ctx = contextEvaluatingTo(StringValue("MY_VAR"))

        val result = evaluator.handle(ReadEnv(StringLiteral("MY_VAR")), ctx)

        assertEquals(StringValue("my_value"), result)
    }

    @Test
    fun `ReadEnvEvaluator throws EnvVariableNotFoundError if the variable does not exist`() {
        val evaluator = ReadEnvEvaluator(MapEnvProvider())
        val ctx = contextEvaluatingTo(StringValue("UNKNOWN_VAR"))

        val error =
            assertFailsWith<EnvVariableNotFoundError> {
                evaluator.handle(ReadEnv(StringLiteral("UNKNOWN_VAR")), ctx)
            }

        assertEquals("UNKNOWN_VAR", error.name)
    }

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
        val ctx = contextEvaluatingTo(NumberValue(10.0))
        val declaration = VariableDeclaration("c", "number", NumberLiteral(10.0), isConst = true)
        VariableDeclarationInterpreter().handle(declaration, ctx)

        assertFailsWith<CannotAssignToConstError> {
            AssignmentInterpreter().handle(Assignment("c", NumberLiteral(20.0)), ctx)
        }
    }

    @Test
    fun `AssignmentInterpreter converts readInput text to the declared type`() {
        // el literal se evalua a su numero y readInput devuelve texto
        val interpreter =
            StubInterpreter { expression ->
                when (expression) {
                    is NumberLiteral -> NumberValue(expression.value)
                    is ReadInput -> StringValue("99")
                    else -> error("el test no usa $expression")
                }
            }
        val ctx = InterpreterContext(Environment(), interpreter)
        VariableDeclarationInterpreter().handle(VariableDeclaration("n", "number", NumberLiteral(1.0)), ctx)

        AssignmentInterpreter().handle(Assignment("n", ReadInput(StringLiteral("p"))), ctx)

        assertEquals(NumberValue(99.0), ctx.env.lookup("n", at = AT))
    }

    @Test
    fun `statement handlers apply only to their own node`() {
        for (case in statementCases) {
            assertTrue(case.handler.applies(case.own))
            assertFalse(case.handler.applies(case.foreign))
        }
    }

    // en ejecucion normal no pasa: el Interpreter consulta applies antes de llamar a handle
    @Test
    fun `statement handlers reject nodes that are not theirs`() {
        for (case in statementCases) {
            assertFailsWith<UnknownStatementError> {
                case.handler.handle(case.foreign, unusedContext())
            }
        }
    }

    @Test
    fun `expression handlers apply only to their own node`() {
        for (case in expressionCases) {
            assertTrue(case.handler.applies(case.own))
            assertFalse(case.handler.applies(case.foreign))
        }
    }

    @Test
    fun `expression handlers reject nodes that are not theirs`() {
        for (case in expressionCases) {
            assertFailsWith<UnknownExpressionError> {
                case.handler.handle(case.foreign, unusedContext())
            }
        }
    }
}
