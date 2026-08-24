package printscript.interpreter

import printscript.ast.Expression
import printscript.ast.Statement

interface InterpreterInterface {
    fun interpret(statements: Iterator<Statement>)

    fun evaluate(expression: Expression): Value
}
