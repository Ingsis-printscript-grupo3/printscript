package printscript.interpreter.plugin

import printscript.ast.Statement
import printscript.interpreter.Environment
import printscript.interpreter.InterpreterInterface
import printscript.interpreter.UnknownStatementError

// plugin that knows how to execute one kind of statement.
// same pattern as TokenReader in the lexer: first ask if it's theirs, then execute.
//
// the "is this mine" guard (matches + throw UnknownStatementError if not) used to
// live duplicated in every handler. here it lives once: the concrete type goes in
// the signature (T), so the subtype only implements the typed execute and doesn't
// repeat the check.
abstract class StatementInterpreter<T : Statement>(val type: Class<T>) {
    fun matches(statement: Statement): Boolean = type.isInstance(statement)

    fun execute(
        statement: Statement,
        env: Environment,
        interpreter: InterpreterInterface,
    ) {
        if (!matches(statement)) throw UnknownStatementError(statement)
        doExecute(type.cast(statement), env, interpreter)
    }

    protected abstract fun doExecute(
        statement: T,
        env: Environment,
        interpreter: InterpreterInterface,
    )
}
