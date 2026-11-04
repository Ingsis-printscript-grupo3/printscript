package printscript.semantic

import printscript.ast.NumberLiteral
import printscript.common.Position
import printscript.semantic.symbol.SymbolTable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

// el unico caso que el codigo fuente no alcanza: un resolver sin handlers
class ExpressionResolverTest {
    @Test
    fun `an expression with no handler registered fails explicitly with the node position`() {
        val resolver = ExpressionResolver(SymbolTable(), handlers = emptyList())
        val node = NumberLiteral(1.0, Position(4, 2))

        val result = resolver.resolveType(node)

        val failure = assertIs<SemanticResult.Failure>(result)
        assertEquals("Unknown expression type.", failure.message)
        assertEquals(node.position, failure.position)
    }
}
