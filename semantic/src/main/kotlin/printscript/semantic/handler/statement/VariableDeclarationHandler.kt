package printscript.semantic.handler.statement

import printscript.ast.Statement
import printscript.ast.VariableDeclaration
import printscript.ast.registry.Handler
import printscript.semantic.SemanticResult
import printscript.semantic.StatementValidator

class VariableDeclarationHandler : Handler<Statement, StatementValidator, SemanticResult<Unit>> {
    override fun applies(node: Statement) = node is VariableDeclaration

    override fun handle(
        node: Statement,
        ctx: StatementValidator,
    ): SemanticResult<Unit> {
        if (node !is VariableDeclaration) {
            return SemanticResult.Failure(
                "Semantic Error: Unexpected node in VariableDeclarationHandler.",
                node.position,
            )
        }

        if (node.isConst && !ctx.rules.allowsConst) {
            return SemanticResult.Failure(
                "Semantic Error: 'const' declarations are not supported in PrintScript ${ctx.rules.version.label}.",
                node.position,
            )
        }

        if (node.isConst && node.value == null) {
            return SemanticResult.Failure(
                "Semantic Error: Constant '${node.name}' must be initialized.",
                node.position,
            )
        }

        if (node.type !in ctx.rules.supportedTypes) {
            return SemanticResult.Failure(
                "Semantic Error: Type '${node.type}' is not supported in PrintScript ${ctx.rules.version.label}.",
                node.position,
            )
        }

        val typeMismatch =
            node.value?.let { value ->
                val exprResult = ctx.expressionResolver.resolveType(value, expectedType = node.type)
                val exprType = (exprResult as? SemanticResult.Success)?.value
                when {
                    exprResult is SemanticResult.Failure -> exprResult
                    exprType != node.type ->
                        SemanticResult.Failure("Semantic Error: Incompatible types.", node.position)
                    else -> null
                }
            }

        if (typeMismatch != null) return typeMismatch

        val defineResult = ctx.symbolTable.define(node.name, node.type, node.isConst)
        return if (defineResult is SemanticResult.Failure) {
            SemanticResult.Failure(defineResult.message, node.position)
        } else {
            defineResult
        }
    }
}
