package printscript.formatter.rule

import printscript.common.Position
import printscript.common.Token
import printscript.common.TokenType
import printscript.formatter.FormatState
import printscript.formatter.Gap
import kotlin.test.Test
import kotlin.test.assertEquals

class RuleGroupTest {
    private fun gap(): Gap {
        val previous = Token(TokenType.IDENTIFIER, Position(1, 1), Position(1, 2), "x")
        val current = Token(TokenType.ASSIGN, Position(1, 5), Position(1, 6), "=")
        return Gap(previous, current, FormatState())
    }

    @Test
    fun `applies every rule of the group in order`() {
        val gap = gap()
        val group = RuleGroup(listOf(FormatterRule { it.spaces = 0 }, FormatterRule { it.spaces += 2 }))

        group.apply(gap)

        assertEquals(2, gap.spaces)
    }

    @Test
    fun `skips its rules when the condition does not hold`() {
        val gap = gap()
        val group = RuleGroup(listOf(FormatterRule { it.spaces = 0 })) { it.newlines > 0 }

        group.apply(gap)

        assertEquals(3, gap.spaces)
    }

    @Test
    fun `a group can hold other groups`() {
        val gap = gap()
        val inner = RuleGroup(listOf(FormatterRule { it.spaces = 1 }))
        val outer = RuleGroup(listOf(inner, FormatterRule { it.newlines = 1 }))

        outer.apply(gap)

        assertEquals(1, gap.spaces)
        assertEquals(1, gap.newlines)
    }
}
