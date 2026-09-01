package printscript.semantic

import printscript.ast.Expression
import printscript.ast.Statement

// these exceptions mark a programming bug, not an error in the user's script:
// they only happen when a Statement/Expression subtype is added without registering
// its handler. SemanticResult.Failure is still the channel for real semantic errors
// in the script (undeclared variable, incompatible types, etc).
sealed class SemanticInternalError(message: String) : RuntimeException(message)

class UnknownStatementError(val statement: Statement) :
    SemanticInternalError("Unknown statement type: ${statement::class.simpleName}")

class UnknownExpressionError(val expression: Expression) :
    SemanticInternalError("Unknown expression type: ${expression::class.simpleName}")
