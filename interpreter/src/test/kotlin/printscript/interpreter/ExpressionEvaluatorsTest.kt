package printscript.interpreter

import printscript.ast.BinaryExpression
import printscript.ast.BooleanLiteral
import printscript.ast.Identifier
import printscript.ast.NumberLiteral
import printscript.ast.ReadEnv
import printscript.ast.ReadInput
import printscript.ast.StringLiteral
import printscript.common.Position
import printscript.common.TokenType
import printscript.interpreter.env.MapEnvProvider
import printscript.interpreter.input.QueueInput
import printscript.interpreter.plugin.expression.BinaryExpressionEvaluator
import printscript.interpreter.plugin.expression.BooleanLiteralEvaluator
import printscript.interpreter.plugin.expression.IdentifierEvaluator
import printscript.interpreter.plugin.expression.NumberLiteralEvaluator
import printscript.interpreter.plugin.expression.ReadEnvEvaluator
import printscript.interpreter.plugin.expression.ReadInputEvaluator
import printscript.interpreter.plugin.expression.StringLiteralEvaluator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExpressionEvaluatorsTest {
    private val cases =
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
    fun `evaluators apply only to their own node`() {
        for (case in cases) {
            assertTrue(case.handler.applies(case.own), case.name)
            assertFalse(case.handler.applies(case.foreign), case.name)
        }
    }

    // en ejecucion normal no pasa: el Interpreter consulta applies antes de llamar a handle
    @Test
    fun `evaluators reject nodes that are not theirs`() {
        for (case in cases) {
            assertFailsWith<UnknownExpressionError>(case.name) {
                case.handler.handle(case.foreign, unusedContext())
            }
        }
    }
}
