package printscript.interpreter

import printscript.ast.Expression
import printscript.ast.Statement

sealed class InterpreterError(message: String) : RuntimeException(message)

class VariableAlreadyDeclaredError(val name: String) :
    InterpreterError("Variable '$name' is already declared")

class UndeclaredVariableError(val name: String) :
    InterpreterError("Variable '$name' is not declared")

class UninitializedVariableError(val name: String) :
    InterpreterError("Variable '$name' is declared but not initialized")

class TypeMismatchError(val leftType: String, val rightType: String) :
    InterpreterError("Cannot operate between $leftType and $rightType")

class UnknownStatementError(val statement: Statement) :
    InterpreterError("Unknown statement type")

class UnknownExpressionError(val expression: Expression) :
    InterpreterError("Unknown expression type")

class CannotAssignToConstError(val name: String) :
    InterpreterError("Cannot reassign constant '$name'")

class ValueConversionError(val value: Value, val targetType: String) :
    InterpreterError("Cannot convert value '${value.textOf()}' to type '$targetType'")

class EnvVariableNotFoundError(val name: String) :
    InterpreterError("Environment variable '$name' is not defined")

class ConditionTypeError(val actualType: String) :
    InterpreterError("Expected boolean condition, got $actualType")
