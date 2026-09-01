package printscript.semantic

import printscript.ast.Expression

// back-reference that ExpressionHandlers receive so they can recurse (BinaryExpression
// resolves both of its sides). same role as InterpreterInterface in the interpreter module.
interface ExpressionResolverInterface {
    fun resolveType(expression: Expression): SemanticResult<String>
}
