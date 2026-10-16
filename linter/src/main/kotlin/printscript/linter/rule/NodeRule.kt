package printscript.linter.rule

import printscript.ast.PositionedNode
import printscript.linter.Warning
import kotlin.reflect.KClass
import kotlin.reflect.safeCast

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
