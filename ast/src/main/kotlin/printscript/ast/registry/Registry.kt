package printscript.ast.registry

import printscript.ast.PositionedNode

class Registry<N : PositionedNode, C, R>(
    private val handlers: List<Handler<N, C, R>> = emptyList(),
) {
    fun register(handler: Handler<N, C, R>): Registry<N, C, R> = Registry(handlers + handler)

    fun resolveOrNull(
        node: N,
        ctx: C,
    ): R? = handlers.firstOrNull { it.applies(node) }?.handle(node, ctx)
}
