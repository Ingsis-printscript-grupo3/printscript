package printscript.linter

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
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

    // una config vacia no nombra ninguna regla, asi que no se crea ninguna.
    // el CLI sin --config usa LinterRules(), que si las prende todas
    @Test
    fun `an empty json config names no rule`() {
        val rules = LinterRulesLoader.fromJson("{}")

        assertNull(rules.identifierFormat)
        assertNull(rules.printCallArgumentsMustBeLiteralOrIdentifier)
        assertNull(rules.readInputArgumentsMustBeLiteralOrIdentifier)
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
        assertEquals(IdentifierFormat.SNAKE_CASE, rules.identifierFormat)
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
        assertEquals(IdentifierFormat.SNAKE_CASE, rules.identifierFormat)
        assertEquals(false, rules.printCallArgumentsMustBeLiteralOrIdentifier)
    }

    @Test
    fun `reads the rules from a json file`() {
        val file = tempConfig("json", """{"identifier_format": "snake case"}""")

        val rules = LinterRulesLoader.fromFile(file.path)

        assertEquals(LinterRules(IdentifierFormat.SNAKE_CASE, null, null), rules)
    }

    @Test
    fun `reads the rules from a yaml file`() {
        val file = tempConfig("yaml", "mandatory-variable-or-literal-in-println: false")

        val rules = LinterRulesLoader.fromFile(file.path)

        assertEquals(LinterRules(null, false, null), rules)
    }

    @Test
    fun `reads the rules from a yml file`() {
        val file = tempConfig("yml", "identifier_format: \"snake case\"")

        val rules = LinterRulesLoader.fromFile(file.path)

        assertEquals(LinterRules(IdentifierFormat.SNAKE_CASE, null, null), rules)
    }

    @Test
    fun `ignores the casing of the extension`() {
        val file = tempConfig("JSON", """{"identifier_format": "snake case"}""")

        val rules = LinterRulesLoader.fromFile(file.path)

        assertEquals(LinterRules(IdentifierFormat.SNAKE_CASE, null, null), rules)
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

        assertEquals(LinterRules(null, null, null), rules)
        assertTrue(buffer.toString().contains("no-existe"))
    }

    @Test
    fun `reads a JSON config from a stream, like the TCK hands it over`() {
        val stream = """{"identifier_format": "snake case"}""".byteInputStream()

        assertEquals(IdentifierFormat.SNAKE_CASE, LinterRulesLoader.fromStream(stream).identifierFormat)
    }

    @Test
    fun `reads a YAML config from a stream, like the TCK hands it over`() {
        val stream = "identifier_format: snake case".byteInputStream()

        assertEquals(IdentifierFormat.SNAKE_CASE, LinterRulesLoader.fromStream(stream).identifierFormat)
    }

    @Test
    fun `handles escaped quotes and yaml comments`() {
        val yaml = "identifier_format: 'snake case' # comment"
        val rulesYaml = LinterRulesLoader.fromYaml(yaml)
        assertEquals(IdentifierFormat.SNAKE_CASE, rulesYaml.identifierFormat)

        val json = """{"identifier_format": "snake case", "unknown": "hello \"world\""}"""
        val rulesJson = LinterRulesLoader.fromJson(json)
        assertEquals(IdentifierFormat.SNAKE_CASE, rulesJson.identifierFormat)
    }

    @Test
    fun `falls back to default when a value is null in json or empty in yaml`() {
        val json = """{"mandatory-variable-or-literal-in-println": null}"""
        val rulesJson = LinterRulesLoader.fromJson(json)
        assertEquals(true, rulesJson.printCallArgumentsMustBeLiteralOrIdentifier)

        val yaml = "mandatory-variable-or-literal-in-println: # empty"
        val rulesYaml = LinterRulesLoader.fromYaml(yaml)
        assertEquals(true, rulesYaml.printCallArgumentsMustBeLiteralOrIdentifier)
    }

    @Test
    fun `handles UTF-8 BOM at beginning of stream`() {
        val stream = "\uFEFF{\"identifier_format\": \"snake case\"}".byteInputStream()
        assertEquals(IdentifierFormat.SNAKE_CASE, LinterRulesLoader.fromStream(stream).identifierFormat)
    }
}
