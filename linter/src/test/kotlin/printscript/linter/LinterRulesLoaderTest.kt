package printscript.linter

import kotlin.test.Test
import kotlin.test.assertEquals

class LinterRulesLoaderTest {
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
}
