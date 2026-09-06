package printscript.ast.registry

import printscript.ast.PositionedNode

interface Handler<N : PositionedNode, C, R> {
    fun applies(node: N): Boolean

    fun handle(
        node: N,
        ctx: C,
    ): R
}
