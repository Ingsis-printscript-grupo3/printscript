package printscript.formatter.rule

import printscript.common.TokenType
import printscript.formatter.FormatterFactory
import printscript.formatter.FormatterRules
import printscript.formatter.Gap
import printscript.lexer.CharStream
import printscript.lexer.Lexer
import java.io.StringReader
import java.io.StringWriter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// una regla que no vive en el modulo: deja tres espacios despues de ':'
private object SpaceAfterColonFromOutside : FormatterRule {
    override fun appliesTo(gap: Gap) = gap.previous.type == TokenType.COLON

    override fun apply(gap: Gap) {
        gap.spaces = 3
    }
}

class FormatterRuleRegistryTest {
    private val everything =
        FormatterRules(
            spaceBeforeColon = true,
            spaceAfterColon = true,
            spacingAroundEquals = true,
            lineBreaksAfterPrintln = 1,
            singleSpaceSeparation = true,
            spaceSurroundingOperations = true,
            lineBreakAfterStatement = true,
            indentInsideIf = 2,
            ifBraceSameLine = true,
        )

    private fun format(
        code: String,
        registry: FormatterRuleRegistry,
        config: FormatterRules = FormatterRules(),
    ): String {
        val writer = StringWriter()
        val tokens = Lexer(CharStream(StringReader(code))).tokenize()
        FormatterFactory.create(config, registry).format(tokens, writer)
        return writer.toString()
    }

    @Test
    fun `without config no rule is created`() {
        assertTrue(FormatterRuleRegistry().rulesFor(FormatterRules()).isEmpty())
    }

    @Test
    fun `creates one rule per option turned on`() {
        assertEquals(9, FormatterRuleRegistry().rulesFor(everything).size)
    }

    @Test
    fun `line breaks run before spaces and indentation runs last`() {
        val rules = FormatterRuleRegistry().rulesFor(everything)

        assertTrue(rules.first() is LineBreaksAfterPrintln)
        assertTrue(rules.last() is IndentInsideIf)
    }

    @Test
    fun `a rule registered from outside runs without touching the formatter`() {
        val registry = FormatterRuleRegistry().register { listOf(SpaceAfterColonFromOutside) }

        assertEquals("let x:   number = 1;", format("let x:number = 1;", registry))
    }

    @Test
    fun `a registered rule runs after the default ones and has the last word`() {
        val registry = FormatterRuleRegistry().register { listOf(SpaceAfterColonFromOutside) }
        val config = FormatterRules(spaceAfterColon = true)

        assertEquals("let x:   number = 1;", format("let x:number = 1;", registry, config))
    }

    @Test
    fun `a rule only touches the gaps it applies to`() {
        val registry = FormatterRuleRegistry(emptyList()).register { listOf(SpaceAfterColonFromOutside) }

        assertEquals("let  x:   number=1;", format("let  x:number=1;", registry))
    }
}
