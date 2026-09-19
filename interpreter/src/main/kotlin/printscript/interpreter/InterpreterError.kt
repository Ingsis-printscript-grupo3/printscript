package printscript.interpreter

import printscript.ast.Expression
import printscript.ast.Statement
import printscript.common.Position
import printscript.common.ScriptError

// igual que LexicalError, SyntaxError y SemanticError: todo error del interprete sabe donde ocurrio.
// end vale lo mismo que start porque un nodo del AST tiene una posicion, no un rango
sealed class InterpreterError(
    override val message: String,
    override val start: Position,
    override val end: Position = start,
) : RuntimeException(message), ScriptError

class VariableAlreadyDeclaredError(val name: String, position: Position) :
    InterpreterError("Variable '$name' is already declared", position)

class UndeclaredVariableError(val name: String, position: Position) :
    InterpreterError("Variable '$name' is not declared", position)

class UninitializedVariableError(val name: String, position: Position) :
    InterpreterError("Variable '$name' is declared but not initialized", position)

class TypeMismatchError(val leftType: String, val rightType: String, position: Position) :
    InterpreterError("Cannot operate between $leftType and $rightType", position)

// estos dos ya reciben el nodo, asi que la posicion la sacan solos
class UnknownStatementError(val statement: Statement) :
    InterpreterError("Unknown statement type", statement.position)

class UnknownExpressionError(val expression: Expression) :
    InterpreterError("Unknown expression type", expression.position)

class CannotAssignToConstError(val name: String, position: Position) :
    InterpreterError("Cannot reassign constant '$name'", position)

class ValueConversionError(val value: Value, val targetType: String, position: Position) :
    InterpreterError("Cannot convert value '${value.textOf()}' to type '$targetType'", position)

class EnvVariableNotFoundError(val name: String, position: Position) :
    InterpreterError("Environment variable '$name' is not defined", position)

class ConditionTypeError(val actualType: String, position: Position) :
    InterpreterError("Expected boolean condition, got $actualType", position)
