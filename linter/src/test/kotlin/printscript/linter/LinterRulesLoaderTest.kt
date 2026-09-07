package printscript.linter

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

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
                "identifier_format": "snake case",
                "mandatory-variable-or-literal-in-println": false
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
            identifier_format: "snake case"
            mandatory-variable-or-literal-in-println: false
            """.trimIndent()
        val rules = LinterRulesLoader.fromYaml(yaml)
        assertEquals("snake case", rules.identifierFormat)
        assertEquals(false, rules.printCallArgumentsMustBeLiteralOrIdentifier)
    }

    @Test
    fun `reads the rules from a json file`() {
        val file = tempConfig("json", """{"identifier_format": "snake case"}""")

        val rules = LinterRulesLoader.fromFile(file.path)

        assertEquals(LinterRules(identifierFormat = SNAKE_CASE), rules)
    }

    @Test
    fun `reads the rules from a yaml file`() {
        val file = tempConfig("yaml", "mandatory-variable-or-literal-in-println: false")

        val rules = LinterRulesLoader.fromFile(file.path)

        assertEquals(LinterRules(printCallArgumentsMustBeLiteralOrIdentifier = false), rules)
    }

    @Test
    fun `reads the rules from a yml file`() {
        val file = tempConfig("yml", "identifier_format: \"snake case\"")

        val rules = LinterRulesLoader.fromFile(file.path)

        assertEquals(LinterRules(identifierFormat = SNAKE_CASE), rules)
    }

    @Test
    fun `ignores the casing of the extension`() {
        val file = tempConfig("JSON", """{"identifier_format": "snake case"}""")

        val rules = LinterRulesLoader.fromFile(file.path)

        assertEquals(LinterRules(identifierFormat = SNAKE_CASE), rules)
    }

    @Test
    fun `a json file and a yaml file with the same rules produce equal results`() {
        val json = tempConfig("json", """{"identifier_format": "snake case"}""")
        val yaml = tempConfig("yaml", "identifier_format: \"snake case\"")

        assertEquals(LinterRulesLoader.fromFile(json.path), LinterRulesLoader.fromFile(yaml.path))
    }

    @Test
    fun `rejects a config file with an extension that is not json or yaml`() {
        val file = tempConfig("txt", "identifier_format: \"snake case\"")

        assertFailsWith<IllegalArgumentException> { LinterRulesLoader.fromFile(file.path) }
    }

    @Test
    fun `rejects a config file that does not exist`() {
        assertFailsWith<IllegalArgumentException> { LinterRulesLoader.fromFile("no-existe.json") }
    }

    @Test
    fun `rejects a config with an identifier format that no rule knows`() {
        val file = tempConfig("json", """{"identifier_format": "camelCase"}""")

        assertFailsWith<IllegalArgumentException> { LinterRulesLoader.fromFile(file.path) }
    }

    @Test
    fun `accepts the readInput key the TCK sends, even though the rule lives elsewhere`() {
        val rules = LinterRulesLoader.fromJson("""{"mandatory-variable-or-literal-in-readInput": false}""")

        assertEquals(false, rules.readInputArgumentsMustBeLiteralOrIdentifier)
    }

    @Test
    fun `an unknown key does not break the load and is reported on stderr`() {
        val buffer = ByteArrayOutputStream()
        val original = System.err
        System.setErr(PrintStream(buffer))
        val rules =
            try {
                LinterRulesLoader.fromJson("""{"no-existe": true}""")
            } finally {
                System.setErr(original)
            }

        assertEquals(LinterRules(), rules)
        assertTrue(buffer.toString().contains("no-existe"))
    }

    @Test
    fun `reads a JSON config from a stream, like the TCK hands it over`() {
        val stream = """{"identifier_format": "snake case"}""".byteInputStream()

        assertEquals(SNAKE_CASE, LinterRulesLoader.fromStream(stream).identifierFormat)
    }

    @Test
    fun `reads a YAML config from a stream, like the TCK hands it over`() {
        val stream = "identifier_format: snake case".byteInputStream()

        assertEquals(SNAKE_CASE, LinterRulesLoader.fromStream(stream).identifierFormat)
    }
}
