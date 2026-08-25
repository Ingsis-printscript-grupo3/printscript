package printscript.formatter
import kotlin.test.Test
import kotlin.test.assertEquals

class FormatterRulesLoaderTest {
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
}
