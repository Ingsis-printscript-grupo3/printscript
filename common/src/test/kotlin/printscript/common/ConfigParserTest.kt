package printscript.common

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ConfigParserTest {
    private fun tempFile(
        extension: String,
        content: String,
    ): File =
        File.createTempFile("config-test", ".$extension").apply {
            writeText(content)
            deleteOnExit()
        }

    @Test
    fun `parses empty json`() {
        val result = ConfigParser.parseJson("{}")
        assertEquals(emptyMap(), result)
    }

    @Test
    fun `parses flat json with strings, numbers, booleans, nulls`() {
        val json =
            """
            {
                "str": "hello world",
                "escaped": "foo \"bar\" \\ baz",
                "num": 42,
                "bool": true,
                "boolFalse": false,
                "nil": null
            }
            """.trimIndent()
        val result = ConfigParser.parseJson(json)
        assertEquals("hello world", result["str"])
        assertEquals("foo \"bar\" \\ baz", result["escaped"])
        assertEquals("42", result["num"])
        assertEquals("true", result["bool"])
        assertEquals("false", result["boolFalse"])
        assertEquals("null", result["nil"])
    }

    @Test
    fun `parses json with nested structures as raw values`() {
        val json =
            """
            {
                "arr": [1, 2, 3],
                "obj": {"nested": "value"}
            }
            """.trimIndent()
        val result = ConfigParser.parseJson(json)
        assertEquals("[1, 2, 3]", result["arr"])
        assertEquals("{\"nested\": \"value\"}", result["obj"])
    }

    @Test
    fun `parses yaml with comments, quotes and empty values`() {
        val yaml =
            """
            # Header comment
            key1: value1 # inline comment
            key2: 'single quoted' # comment
            key3: "double quoted"
            key4:
            key5: # empty with comment
            """.trimIndent()
        val result = ConfigParser.parseYaml(yaml)
        assertEquals("value1", result["key1"])
        assertEquals("single quoted", result["key2"])
        assertEquals("double quoted", result["key3"])
        assertEquals("", result["key4"])
        assertEquals("", result["key5"])
    }

    @Test
    fun `handles UTF-8 BOM in json and yaml`() {
        val json = "\uFEFF{\"key\": true}"
        assertEquals("true", ConfigParser.parseJson(json)["key"])

        val yaml = "\uFEFFkey: true"
        assertEquals("true", ConfigParser.parseYaml(yaml)["key"])
    }

    @Test
    fun `parseStream auto-detects json and yaml`() {
        val jsonStream = """{"key": "val"}""".byteInputStream()
        assertEquals("val", ConfigParser.parseStream(jsonStream)["key"])

        val yamlStream = "key: val".byteInputStream()
        assertEquals("val", ConfigParser.parseStream(yamlStream)["key"])
    }

    @Test
    fun `parseFile parses supported extensions`() {
        val jsonFile = tempFile("json", """{"k": "v"}""")
        assertEquals("v", ConfigParser.parseFile(jsonFile)["k"])

        val yamlFile = tempFile("yaml", "k: v")
        assertEquals("v", ConfigParser.parseFile(yamlFile)["k"])

        val ymlFile = tempFile("yml", "k: v")
        assertEquals("v", ConfigParser.parseFile(ymlFile)["k"])
    }

    @Test
    fun `parseFile rejects unsupported extension or missing file`() {
        val txtFile = tempFile("txt", "k: v")
        assertFailsWith<IllegalArgumentException> { ConfigParser.parseFile(txtFile) }
        assertFailsWith<IllegalArgumentException> { ConfigParser.parseFile("missing-file.json") }
    }

    @Test
    fun `parses yaml with colons in quoted keys and values`() {
        val yaml =
            """
            "http://domain.com": "secure"
            'ftp://domain.com': 'active'
            api_endpoint: https://api.domain.com/v1
            """.trimIndent()
        val result = ConfigParser.parseYaml(yaml)
        assertEquals("secure", result["http://domain.com"])
        assertEquals("active", result["ftp://domain.com"])
        assertEquals("https://api.domain.com/v1", result["api_endpoint"])
    }

    @Test
    fun `parses yaml quoted values preserving hashes and stripping comments`() {
        val yaml =
            """
            key1: "value #1" # comment1
            key2: 'value #2' # comment2
            """.trimIndent()
        val result = ConfigParser.parseYaml(yaml)
        assertEquals("value #1", result["key1"])
        assertEquals("value #2", result["key2"])
    }

    @Test
    fun `parses json with trailing commas, newlines, and nested strings with braces`() {
        val json =
            """
            {
                "spaced"
                :
                "value",
                "nested": {"inner": "brace } inside string"},
                "trailing": true,
            }
            """.trimIndent()
        val result = ConfigParser.parseJson(json)
        assertEquals("value", result["spaced"])
        assertEquals("{\"inner\": \"brace } inside string\"}", result["nested"])
        assertEquals("true", result["trailing"])
    }
}
