package printscript.interpreter

import printscript.ast.Expression
import printscript.ast.PositionedNode
import printscript.ast.Statement
import printscript.ast.registry.Handler
import printscript.interpreter.plugin.InterpreterContext

// para los handlers que no usan el contexto o que fallan antes de usarlo
internal fun unusedContext() = InterpreterContext(Environment(), StubInterpreter { error("no deberia evaluar nada") })

// cualquier expresion que el handler pida evaluar devuelve value
internal fun contextEvaluatingTo(value: Value) = InterpreterContext(Environment(), StubInterpreter { value })

// aisla al handler bajo prueba del resto del interprete y registra lo que le pide ejecutar
internal class StubInterpreter(
    private val onInterpret: (Statement) -> Unit = {},
    private val evaluator: (Expression) -> Value,
) : InterpreterInterface {
    val interpreted = mutableListOf<Statement>()

    override fun interpret(statements: Iterator<Statement>) =
        statements.forEach {
            interpreted.add(it)
            onInterpret(it)
        }

    override fun evaluate(expression: Expression): Value = evaluator(expression)
}

internal class HandlerCase<N : PositionedNode>(
    val handler: Handler<N, InterpreterContext, *>,
    val own: N,
    val foreign: N,
) {
    val name: String = handler::class.simpleName ?: "handler"
}
