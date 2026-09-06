package printscript.ast.registry

import printscript.ast.PositionedNode

class NoHandlerFoundException(node: PositionedNode) :
    RuntimeException("No handler registered for node ${node::class.simpleName} at ${node.position}")
