package printscript.semantic

// lo que recibe cada handler de expresiones: el resolver para sus subexpresiones y el tipo que espera quien pregunta
class ResolverContext(
    val resolver: ExpressionResolver,
    val expectedType: String?,
)
