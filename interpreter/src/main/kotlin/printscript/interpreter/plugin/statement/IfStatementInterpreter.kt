package printscript.interpreter.plugin.statement

import printscript.ast.IfStatement
import printscript.ast.Statement
import printscript.ast.registry.Handler
import printscript.interpreter.BooleanValue
import printscript.interpreter.ConditionTypeError
import printscript.interpreter.UnknownStatementError
import printscript.interpreter.interpret
import printscript.interpreter.plugin.InterpreterContext
import printscript.interpreter.typeName

class IfStatementInterpreter : Handler<Statement, InterpreterContext, Unit> {
    override fun applies(node: Statement) = node is IfStatement

    override fun handle(
        node: Statement,
        ctx: InterpreterContext,
    ) {
        if (node !is IfStatement) throw UnknownStatementError(node)

        val conditionValue = ctx.interpreter.evaluate(node.condition)
        if (conditionValue !is BooleanValue) {
            throw ConditionTypeError(conditionValue.typeName())
        }

        val elseBranch = node.elseBranch
        if (conditionValue.value) {
            ctx.interpreter.interpret(node.thenBranch)
        } else if (elseBranch != null) {
            ctx.interpreter.interpret(elseBranch)
        }
    }
}
