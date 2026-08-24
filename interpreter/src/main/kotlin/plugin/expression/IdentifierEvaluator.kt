package printscript.interpreter.plugin.expression
import printscript.ast.Identifier
import printscript.interpreter.Environment
import printscript.interpreter.InterpreterInterface
import printscript.interpreter.Value
import printscript.interpreter.plugin.ExpressionEvaluator

class IdentifierEvaluator : ExpressionEvaluator<Identifier> {
    override fun evaluate(
        expression: Identifier,
        env: Environment,
        interpreter: InterpreterInterface,
    ): Value {
        return env.lookup(expression.name)
    }
}
