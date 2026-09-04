package printscript.linter

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LinterRulesLoaderTest {
    private fun tempConfig(
        extension: String,
        content: String,
    ): File =
        File.createTempFile("linter-rules", ".$extension").apply {
            writeText(content)
            deleteOnExit()
        }

    @Test
    fun `loads default rules when json is empty`() {
        val json = "{}"
        val rules = LinterRulesLoader.fromJson(json)
        assertEquals("camel case", rules.identifierFormat)
        assertEquals(true, rules.printCallArgumentsMustBeLiteralOrIdentifier)
    }

    @Test
    fun `loads json rules`() {
        val json =
            """
            {
                "identifierFormat": "snake case",
                "printCallArgumentsMustBeLiteralOrIdentifier": false
            }
            """.trimIndent()
        val rules = LinterRulesLoader.fromJson(json)
        assertEquals("snake case", rules.identifierFormat)
        assertEquals(false, rules.printCallArgumentsMustBeLiteralOrIdentifier)
    }

    @Test
    fun `loads yaml rules`() {
        val yaml =
            """
            identifierFormat: "snake case"
            printCallArgumentsMustBeLiteralOrIdentifier: false
            """.trimIndent()
        val rules = LinterRulesLoader.fromYaml(yaml)
        assertEquals("snake case", rules.identifierFormat)
        assertEquals(false, rules.printCallArgumentsMustBeLiteralOrIdentifier)
    }

    @Test
    fun `reads the rules from a json file`() {
        val file = tempConfig("json", """{"identifierFormat": "snake case"}""")

        val rules = LinterRulesLoader.fromFile(file.path)

        assertEquals(LinterRules(identifierFormat = SNAKE_CASE), rules)
    }

    @Test
    fun `reads the rules from a yaml file`() {
        val file = tempConfig("yaml", "printCallArgumentsMustBeLiteralOrIdentifier: false")

        val rules = LinterRulesLoader.fromFile(file.path)

        assertEquals(LinterRules(printCallArgumentsMustBeLiteralOrIdentifier = false), rules)
    }

    @Test
    fun `reads the rules from a yml file`() {
        val file = tempConfig("yml", "identifierFormat: \"snake case\"")

        val rules = LinterRulesLoader.fromFile(file.path)

        assertEquals(LinterRules(identifierFormat = SNAKE_CASE), rules)
    }

    @Test
    fun `ignores the casing of the extension`() {
        val file = tempConfig("JSON", """{"identifierFormat": "snake case"}""")

        val rules = LinterRulesLoader.fromFile(file.path)

        assertEquals(LinterRules(identifierFormat = SNAKE_CASE), rules)
    }

    @Test
    fun `a json file and a yaml file with the same rules produce equal results`() {
        val json = tempConfig("json", """{"identifierFormat": "snake case"}""")
        val yaml = tempConfig("yaml", "identifierFormat: \"snake case\"")

        assertEquals(LinterRulesLoader.fromFile(json.path), LinterRulesLoader.fromFile(yaml.path))
    }

    @Test
    fun `rejects a config file with an extension that is not json or yaml`() {
        val file = tempConfig("txt", "identifierFormat: \"snake case\"")

        assertFailsWith<IllegalArgumentException> { LinterRulesLoader.fromFile(file.path) }
    }

    @Test
    fun `rejects a config file that does not exist`() {
        assertFailsWith<IllegalArgumentException> { LinterRulesLoader.fromFile("no-existe.json") }
    }

    @Test
    fun `rejects a config with an identifier format that no rule knows`() {
        val file = tempConfig("json", """{"identifierFormat": "camelCase"}""")

        assertFailsWith<IllegalArgumentException> { LinterRulesLoader.fromFile(file.path) }
    }
}
