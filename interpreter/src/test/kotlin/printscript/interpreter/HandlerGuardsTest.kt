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
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// el codigo fuente nunca le da a un handler un nodo ajeno: el Interpreter consulta applies antes de llamar a handle
class HandlerGuardsTest {
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
    fun `each handler applies only to its own node`() {
        for (case in statementCases + expressionCases) {
            assertTrue(case.handler.applies(case.own), case.name)
            assertFalse(case.handler.applies(case.foreign), case.name)
        }
    }

    @Test
    fun `statement handlers reject nodes that are not theirs`() {
        for (case in statementCases) {
            assertFailsWith<UnknownStatementError>(case.name) { case.handler.handle(case.foreign, unusedContext()) }
        }
    }

    @Test
    fun `expression evaluators reject nodes that are not theirs`() {
        for (case in expressionCases) {
            assertFailsWith<UnknownExpressionError>(case.name) { case.handler.handle(case.foreign, unusedContext()) }
        }
    }
}

private class HandlerCase<N : PositionedNode>(
    val handler: Handler<N, InterpreterContext, *>,
    val own: N,
    val foreign: N,
) {
    val name: String = handler::class.simpleName ?: "handler"
}

// el handler falla antes de usar el contexto: si pidiera ejecutar o evaluar algo, el test se rompe
private fun unusedContext() = InterpreterContext(Environment(), UnusedInterpreter)

private object UnusedInterpreter : InterpreterInterface {
    override fun interpret(statements: Iterator<Statement>): Unit = error("no deberia ejecutar nada")

    override fun evaluate(expression: Expression): Value = error("no deberia evaluar nada")
}
