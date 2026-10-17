package printscript.semantic.handler.expression

import printscript.ast.Expression
import printscript.semantic.ExpressionResolver
import printscript.semantic.SemanticResult

// readInput y readEnv se validan igual: el argumento es un string y el tipo lo decide el contexto
internal fun resolveReadArgument(
    function: String,
    argument: Expression,
    ctx: ExpressionResolver,
): SemanticResult<String> =
    when (val argumentType = ctx.resolveType(argument, expectedType = "string")) {
        is SemanticResult.Failure -> argumentType
        is SemanticResult.Success ->
            if (argumentType.value != "string") {
                SemanticResult.Failure(
                    "'$function' argument must be a string, found '${argumentType.value}'.",
                    argument.position,
                )
            } else {
                SemanticResult.Success(ctx.expectedType ?: "string")
            }
    }
