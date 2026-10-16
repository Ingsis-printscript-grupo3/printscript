package printscript.linter.rule

import printscript.ast.PositionedNode
import printscript.linter.Warning
import kotlin.reflect.KClass
import kotlin.reflect.safeCast

/**
 * Regla base para reglas de linter que analizan un unico tipo de nodo del AST.
 * Filtra por tipo de nodo y garantiza que check retorne lista vacia sin fallar ante otros nodos.
 */
abstract class NodeRule<T : PositionedNode>(
    private val type: KClass<T>,
) : LinterRule {
    final override fun appliesTo(node: PositionedNode): Boolean = type.isInstance(node)

    final override fun check(node: PositionedNode): List<Warning> {
        val typedNode = type.safeCast(node) ?: return emptyList()
        return checkNode(typedNode)
    }

    protected abstract fun checkNode(node: T): List<Warning>
}
