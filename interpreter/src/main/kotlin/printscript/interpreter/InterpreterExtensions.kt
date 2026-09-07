package printscript.interpreter

import printscript.ast.Statement

fun InterpreterInterface.interpret(statement: Statement) {
    interpret(listOf(statement).iterator())
}
