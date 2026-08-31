package printscript.formatter
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FormatterRulesLoaderTest {
    private fun tempConfig(
        extension: String,
        content: String,
    ): File =
        File.createTempFile("formatter-rules", ".$extension").apply {
            writeText(content)
            deleteOnExit()
        }

    @Test
    fun `loads all fields from a complete JSON config`() {
        val json =
            """
            {
              "spaceBeforeColon": true,
              "spaceAfterColon": false,
              "spaceAroundAssignment": true,
              "lineBreaksBeforePrintln": 2
            }
            """.trimIndent()

        val rules = FormatterRulesLoader.fromJson(json)

        assertEquals(FormatterRules(true, false, true, 2), rules)
    }

    @Test
    fun `falls back to defaults for fields missing from a JSON config`() {
        val json = """{ "spaceBeforeColon": true }"""

        val rules = FormatterRulesLoader.fromJson(json)

        assertEquals(FormatterRules(spaceBeforeColon = true), rules)
    }

    @Test
    fun `loads all fields from a complete YAML config`() {
        val yaml =
            """
            spaceBeforeColon: true
            spaceAfterColon: false
            spaceAroundAssignment: true
            lineBreaksBeforePrintln: 2
            """.trimIndent()

        val rules = FormatterRulesLoader.fromYaml(yaml)

        assertEquals(FormatterRules(true, false, true, 2), rules)
    }

    @Test
    fun `falls back to defaults for fields missing from a YAML config`() {
        val yaml = "lineBreaksBeforePrintln: 0"

        val rules = FormatterRulesLoader.fromYaml(yaml)

        assertEquals(FormatterRules(lineBreaksBeforePrintln = 0), rules)
    }

    @Test
    fun `JSON and YAML configs representing the same rules produce equal results`() {
        val json = """{"spaceBeforeColon": true, "lineBreaksBeforePrintln": 2}"""
        val yaml = "spaceBeforeColon: true\nlineBreaksBeforePrintln: 2"

        assertEquals(FormatterRulesLoader.fromJson(json), FormatterRulesLoader.fromYaml(yaml))
    }

    @Test
    fun `reads the rules from a json file`() {
        val file = tempConfig("json", """{"spaceBeforeColon": true, "lineBreaksBeforePrintln": 2}""")

        val rules = FormatterRulesLoader.fromFile(file.path)

        assertEquals(FormatterRules(spaceBeforeColon = true, lineBreaksBeforePrintln = 2), rules)
    }

    @Test
    fun `reads the rules from a yaml file`() {
        val file = tempConfig("yaml", "spaceAfterColon: false")

        val rules = FormatterRulesLoader.fromFile(file.path)

        assertEquals(FormatterRules(spaceAfterColon = false), rules)
    }

    @Test
    fun `reads the rules from a yml file`() {
        val file = tempConfig("yml", "spaceAroundAssignment: false")

        val rules = FormatterRulesLoader.fromFile(file.path)

        assertEquals(FormatterRules(spaceAroundAssignment = false), rules)
    }

    @Test
    fun `ignores the casing of the extension`() {
        val file = tempConfig("JSON", """{"spaceBeforeColon": true}""")

        val rules = FormatterRulesLoader.fromFile(file.path)

        assertEquals(FormatterRules(spaceBeforeColon = true), rules)
    }

    @Test
    fun `a json file and a yaml file with the same rules produce equal results`() {
        val json = tempConfig("json", """{"spaceBeforeColon": true, "lineBreaksBeforePrintln": 2}""")
        val yaml = tempConfig("yaml", "spaceBeforeColon: true\nlineBreaksBeforePrintln: 2")

        assertEquals(FormatterRulesLoader.fromFile(json.path), FormatterRulesLoader.fromFile(yaml.path))
    }

    @Test
    fun `rejects a config file with an extension that is not json or yaml`() {
        val file = tempConfig("txt", "spaceAfterColon: false")

        assertFailsWith<IllegalArgumentException> { FormatterRulesLoader.fromFile(file.path) }
    }

    @Test
    fun `rejects a config file that does not exist`() {
        assertFailsWith<IllegalArgumentException> { FormatterRulesLoader.fromFile("no-existe.json") }
    }
}
