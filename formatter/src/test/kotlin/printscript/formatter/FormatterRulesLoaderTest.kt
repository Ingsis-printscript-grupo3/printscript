package printscript.formatter

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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

        val expected =
            FormatterRules.of(
                "enforce-spacing-before-colon-in-declaration" to true,
                "enforce-spacing-after-colon-in-declaration" to true,
                "enforce-spacing-around-equals" to true,
                "line-breaks-after-println" to 2,
                "indent-inside-if" to 2,
                "if-brace-below-line" to true,
            )

        assertEquals(expected, FormatterRulesLoader.fromJson(json))
    }

    @Test
    fun `loads every TCK key from a YAML config`() {
        val yaml =
            """
            enforce-spacing-before-colon-in-declaration: true
            enforce-no-spacing-around-equals: true
            line-breaks-after-println: 1
            """.trimIndent()

        val expected =
            FormatterRules.of(
                "enforce-spacing-before-colon-in-declaration" to true,
                "enforce-no-spacing-around-equals" to true,
                "line-breaks-after-println" to 1,
            )

        assertEquals(expected, FormatterRulesLoader.fromYaml(yaml))
    }

    @Test
    fun `falls back to defaults for keys the config does not mention`() {
        val rules = FormatterRulesLoader.fromJson("""{ "line-breaks-after-println": 2 }""")

        assertEquals(FormatterRules.of("line-breaks-after-println" to 2), rules)
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

        assertEquals(2, FormatterRulesLoader.fromStream(stream).values["line-breaks-after-println"])
    }

    @Test
    fun `reads a YAML config from a stream, like the TCK hands it over`() {
        val stream = "line-breaks-after-println: 1".byteInputStream()

        assertEquals(1, FormatterRulesLoader.fromStream(stream).values["line-breaks-after-println"])
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

        assertEquals(FormatterRules.of("line-breaks-after-println" to 2), FormatterRulesLoader.fromFile(file.path))
    }

    @Test
    fun `reads the rules from a yaml file`() {
        val file = tempConfig("yaml", "enforce-spacing-after-colon-in-declaration: true")

        assertEquals(
            FormatterRules.of("enforce-spacing-after-colon-in-declaration" to true),
            FormatterRulesLoader.fromFile(file.path),
        )
    }

    @Test
    fun `reads the rules from a yml file`() {
        val file = tempConfig("yml", "enforce-no-spacing-around-equals: true")

        assertEquals(
            FormatterRules.of("enforce-no-spacing-around-equals" to true),
            FormatterRulesLoader.fromFile(file.path),
        )
    }

    @Test
    fun `ignores the casing of the extension`() {
        val file = tempConfig("JSON", """{"line-breaks-after-println": 1}""")

        assertEquals(FormatterRules.of("line-breaks-after-println" to 1), FormatterRulesLoader.fromFile(file.path))
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
    fun `a boolean set to false leaves the rule off`() {
        assertEquals(FormatterRules(), FormatterRulesLoader.fromJson("""{"if-brace-same-line": false}"""))
    }

    @Test
    fun `rejects a boolean value that is not true or false`() {
        assertFailsWith<IllegalArgumentException> { FormatterRulesLoader.fromYaml("if-brace-same-line: si") }
    }

    @Test
    fun `rejects an integer value that is not a number`() {
        assertFailsWith<IllegalArgumentException> { FormatterRulesLoader.fromYaml("indent-inside-if: muchos") }
    }

    @Test
    fun `rejects asking for spaces and no spaces around equals at once`() {
        assertFailsWith<IllegalArgumentException> {
            FormatterRulesLoader.fromJson(
                """{"enforce-spacing-around-equals": true, "enforce-no-spacing-around-equals": true}""",
            )
        }
    }

    @Test
    fun `a null value in json or an empty one in yaml leaves the rule off`() {
        val json = """{"line-breaks-after-println": null, "enforce-spacing-around-equals": null}"""
        assertEquals(FormatterRules(), FormatterRulesLoader.fromJson(json))

        val yaml = "line-breaks-after-println:\nenforce-spacing-around-equals: # empty"
        assertEquals(FormatterRules(), FormatterRulesLoader.fromYaml(yaml))
    }

    @Test
    fun `handles UTF-8 BOM at beginning of stream`() {
        val stream = "\uFEFF{\"line-breaks-after-println\": 2}".byteInputStream()
        assertEquals(2, FormatterRulesLoader.fromStream(stream).values["line-breaks-after-println"])
    }
}
