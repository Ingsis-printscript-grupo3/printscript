package printscript.interpreter

import printscript.ast.Expression
import printscript.ast.Statement

sealed class InterpreterError(message: String) : RuntimeException(message)

class VariableAlreadyDeclaredError(val name: String) :
    InterpreterError("La variable '$name' ya fue declarada")

class UndeclaredVariableError(val name: String) :
    InterpreterError("La variable '$name' no fue declarada")

class UninitializedVariableError(val name: String) :
    InterpreterError("La variable '$name' fue declarada pero todavia no tiene un valor asignado")

class TypeMismatchError(val leftType: String, val rightType: String) :
    InterpreterError("No se puede operar entre $leftType y $rightType")

class UnknownStatementError(val statement: Statement) :
    InterpreterError("No se reconoce el tipo de statement")

class UnknownExpressionError(val expression: Expression) :
    InterpreterError("No se reconoce el tipo de expresion")
