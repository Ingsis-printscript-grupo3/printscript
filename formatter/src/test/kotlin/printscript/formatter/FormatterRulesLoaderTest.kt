package printscript.formatter

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FormatterRulesLoaderTest {
    private fun tempConfig(
        extension: String,
        content: String,
    ): File =
        File.createTempFile("formatter-rules", ".$extension").apply {
            writeText(content)
            deleteOnExit()
        }

    private fun capturingStderr(block: () -> Unit): String {
        val buffer = ByteArrayOutputStream()
        val original = System.err
        System.setErr(PrintStream(buffer))
        try {
            block()
        } finally {
            System.setErr(original)
        }
        return buffer.toString()
    }

    @Test
    fun `loads every TCK key from a JSON config`() {
        val json =
            """
            {
              "enforce-spacing-before-colon-in-declaration": true,
              "enforce-spacing-after-colon-in-declaration": true,
              "enforce-spacing-around-equals": true,
              "line-breaks-after-println": 2,
              "indent-inside-if": 2,
              "if-brace-below-line": true
            }
            """.trimIndent()

        val rules = FormatterRulesLoader.fromJson(json)

        assertTrue(rules.spaceBeforeColon)
        assertTrue(rules.spaceAfterColon)
        assertTrue(rules.spacingAroundEquals)
        assertEquals(2, rules.lineBreaksAfterPrintln)
        assertEquals(2, rules.indentInsideIf)
        assertTrue(rules.ifBraceBelowLine)
    }

    @Test
    fun `loads every TCK key from a YAML config`() {
        val yaml =
            """
            enforce-spacing-before-colon-in-declaration: true
            enforce-no-spacing-around-equals: true
            line-breaks-after-println: 1
            """.trimIndent()

        val rules = FormatterRulesLoader.fromYaml(yaml)

        assertTrue(rules.spaceBeforeColon)
        assertTrue(rules.noSpacingAroundEquals)
        assertEquals(1, rules.lineBreaksAfterPrintln)
    }

    @Test
    fun `falls back to defaults for keys the config does not mention`() {
        val rules = FormatterRulesLoader.fromJson("""{ "line-breaks-after-println": 2 }""")

        assertEquals(FormatterRules(lineBreaksAfterPrintln = 2), rules)
    }

    @Test
    fun `an unknown key does not break the load and is reported on stderr`() {
        var rules: FormatterRules? = null

        val stderr = capturingStderr { rules = FormatterRulesLoader.fromJson("""{ "no-existe": true }""") }

        assertEquals(FormatterRules(), rules)
        assertTrue(stderr.contains("no-existe"))
    }

    @Test
    fun `reads a JSON config from a stream, like the TCK hands it over`() {
        val stream = """{ "line-breaks-after-println": 2 }""".byteInputStream()

        assertEquals(2, FormatterRulesLoader.fromStream(stream).lineBreaksAfterPrintln)
    }

    @Test
    fun `reads a YAML config from a stream, like the TCK hands it over`() {
        val stream = "line-breaks-after-println: 1".byteInputStream()

        assertEquals(1, FormatterRulesLoader.fromStream(stream).lineBreaksAfterPrintln)
    }

    @Test
    fun `JSON and YAML configs representing the same rules produce equal results`() {
        val json = """{"enforce-spacing-before-colon-in-declaration": true, "line-breaks-after-println": 2}"""
        val yaml = "enforce-spacing-before-colon-in-declaration: true\nline-breaks-after-println: 2"

        assertEquals(FormatterRulesLoader.fromJson(json), FormatterRulesLoader.fromYaml(yaml))
    }

    @Test
    fun `reads the rules from a json file`() {
        val file = tempConfig("json", """{"line-breaks-after-println": 2}""")

        assertEquals(FormatterRules(lineBreaksAfterPrintln = 2), FormatterRulesLoader.fromFile(file.path))
    }

    @Test
    fun `reads the rules from a yaml file`() {
        val file = tempConfig("yaml", "enforce-spacing-after-colon-in-declaration: true")

        assertEquals(FormatterRules(spaceAfterColon = true), FormatterRulesLoader.fromFile(file.path))
    }

    @Test
    fun `reads the rules from a yml file`() {
        val file = tempConfig("yml", "enforce-no-spacing-around-equals: true")

        assertEquals(FormatterRules(noSpacingAroundEquals = true), FormatterRulesLoader.fromFile(file.path))
    }

    @Test
    fun `ignores the casing of the extension`() {
        val file = tempConfig("JSON", """{"line-breaks-after-println": 1}""")

        assertEquals(FormatterRules(lineBreaksAfterPrintln = 1), FormatterRulesLoader.fromFile(file.path))
    }

    @Test
    fun `rejects a config file with an extension that is not json or yaml`() {
        val file = tempConfig("txt", "line-breaks-after-println: 1")

        assertFailsWith<IllegalArgumentException> { FormatterRulesLoader.fromFile(file.path) }
    }

    @Test
    fun `rejects a config file that does not exist`() {
        assertFailsWith<IllegalArgumentException> { FormatterRulesLoader.fromFile("no-existe.json") }
    }

    @Test
    fun `rejects a line breaks value the formatter cannot honour`() {
        assertFailsWith<IllegalArgumentException> {
            FormatterRulesLoader.fromJson("""{ "line-breaks-after-println": 5 }""")
        }
    }

    @Test
    fun `a null value in json or an empty one in yaml leaves the rule off`() {
        val json = """{"line-breaks-after-println": null, "enforce-spacing-around-equals": null}"""
        val rulesJson = FormatterRulesLoader.fromJson(json)
        assertNull(rulesJson.lineBreaksAfterPrintln)
        assertFalse(rulesJson.spacingAroundEquals)

        val yaml = "line-breaks-after-println:\nenforce-spacing-around-equals: # empty"
        val rulesYaml = FormatterRulesLoader.fromYaml(yaml)
        assertNull(rulesYaml.lineBreaksAfterPrintln)
        assertFalse(rulesYaml.spacingAroundEquals)
    }

    @Test
    fun `handles UTF-8 BOM at beginning of stream`() {
        val stream = "\uFEFF{\"line-breaks-after-println\": 2}".byteInputStream()
        assertEquals(2, FormatterRulesLoader.fromStream(stream).lineBreaksAfterPrintln)
    }
}
