package printscript.formatter.rule

import printscript.common.LanguageVersion
import printscript.common.TokenType
import printscript.formatter.FormatterFactory
import printscript.formatter.FormatterRules
import printscript.formatter.FormatterRulesLoader
import printscript.formatter.Gap
import printscript.lexer.CharStream
import printscript.lexer.LexerFactory
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.io.StringReader
import java.io.StringWriter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
        FormatterRules.of(
            "enforce-spacing-before-colon-in-declaration" to true,
            "enforce-spacing-after-colon-in-declaration" to true,
            "enforce-spacing-around-equals" to true,
            "line-breaks-after-println" to 1,
            "mandatory-single-space-separation" to true,
            "mandatory-space-surrounding-operations" to true,
            "mandatory-line-break-after-statement" to true,
            "indent-inside-if" to 2,
            "if-brace-same-line" to true,
        )

    private val fromOutside = BoolRuleFactory("space-after-colon-from-outside") { SpaceAfterColonFromOutside }
    private val fromOutsideOn = FormatterRules.of("space-after-colon-from-outside" to true)

    private fun format(
        code: String,
        registry: FormatterRuleRegistry,
        config: FormatterRules = FormatterRules(),
    ): String {
        val writer = StringWriter()
        val tokens = LexerFactory.create(CharStream(StringReader(code)), LanguageVersion.V1_1).tokenize()
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
        val registry = FormatterRuleRegistry().register(fromOutside)

        assertEquals("let x:   number = 1;", format("let x:number = 1;", registry, fromOutsideOn))
    }

    @Test
    fun `a registered rule stays off when the config does not ask for it`() {
        val registry = FormatterRuleRegistry().register(fromOutside)

        assertEquals("let x:number = 1;", format("let x:number = 1;", registry))
    }

    @Test
    fun `a registered rule runs after the default ones and has the last word`() {
        val registry = FormatterRuleRegistry().register(fromOutside)
        val config =
            FormatterRules.of(
                "enforce-spacing-after-colon-in-declaration" to true,
                "space-after-colon-from-outside" to true,
            )

        assertEquals("let x:   number = 1;", format("let x:number = 1;", registry, config))
    }

    @Test
    fun `a rule only touches the gaps it applies to`() {
        val registry = FormatterRuleRegistry(emptyList()).register(fromOutside)

        assertEquals("let  x:   number=1;", format("let  x:number=1;", registry, fromOutsideOn))
    }

    @Test
    fun `the loader reads the key of a registered rule from the config file`() {
        val registry = FormatterRuleRegistry().register(fromOutside)
        val buffer = ByteArrayOutputStream()
        val original = System.err
        System.setErr(PrintStream(buffer))
        val config =
            try {
                FormatterRulesLoader.fromJson("""{ "space-after-colon-from-outside": true }""", registry)
            } finally {
                System.setErr(original)
            }

        assertEquals(fromOutsideOn, config)
        assertEquals("", buffer.toString())
        assertEquals("let x:   number = 1;", format("let x:number = 1;", registry, config))
    }

    @Test
    fun `a boolean rule set to false in code stays off`() {
        val config = FormatterRules.of("if-brace-same-line" to false)

        assertTrue(FormatterRuleRegistry().rulesFor(config).isEmpty())
    }

    @Test
    fun `registering a key that already exists fails`() {
        val duplicated = BoolRuleFactory("if-brace-same-line") { SpaceAfterColonFromOutside }

        assertFailsWith<IllegalArgumentException> { FormatterRuleRegistry().register(duplicated) }
    }
}
