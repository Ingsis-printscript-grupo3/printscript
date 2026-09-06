package printscript.ast.registry

import printscript.ast.PositionedNode
import printscript.common.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private data class FakeNode(val label: String, override val position: Position) : PositionedNode

private class FakeNodeHandler : Handler<FakeNode, Unit, String> {
    override fun applies(node: FakeNode) = true

    override fun handle(
        node: FakeNode,
        ctx: Unit,
    ) = "handled:${node.label}"
}

class RegistryTest {
    @Test
    fun `resolves the node using a registered handler`() {
        val registry = Registry<FakeNode, Unit, String>(listOf(FakeNodeHandler()))

        val result = registry.resolve(FakeNode("a", Position(1, 1)), Unit)

        assertEquals("handled:a", result)
    }

    @Test
    fun `resolveOrNull returns null when no handler applies`() {
        val registry = Registry<FakeNode, Unit, String>()

        assertEquals(null, registry.resolveOrNull(FakeNode("a", Position(1, 1)), Unit))
    }

    @Test
    fun `resolve throws NoHandlerFoundException with the node position when nothing applies`() {
        val registry = Registry<FakeNode, Unit, String>()
        val node = FakeNode("orphan", Position(3, 7))

        val error =
            assertFailsWith<NoHandlerFoundException> {
                registry.resolve(node, Unit)
            }

        assert(error.message!!.contains("FakeNode"))
        assert(error.message!!.contains(node.position.toString()))
    }

    @Test
    fun `register extends a registry without mutating the original`() {
        val empty = Registry<FakeNode, Unit, String>()
        val extended = empty.register(FakeNodeHandler())
        val node = FakeNode("b", Position(2, 2))

        assertEquals(null, empty.resolveOrNull(node, Unit))
        assertEquals("handled:b", extended.resolveOrNull(node, Unit))
    }
}
