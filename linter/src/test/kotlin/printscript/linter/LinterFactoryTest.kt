package printscript.linter

import printscript.ast.PositionedNode
import printscript.ast.VariableDeclaration
import printscript.common.LanguageVersion
import printscript.linter.rule.BooleanRuleFactory
import printscript.linter.rule.LinterRule
import printscript.linter.rule.LinterRuleRegistry
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LinterFactoryTest {
    // una infraccion de cada regla por defecto
    private val oneOfEach =
        """
        let mi_variable: number = 1;
        println(1 + 2);
        let dato: string = readInput("a" + "b");
        """.trimIndent()

    private val snakeCaseDeclaration = "let mi_variable: number = 1;"

    private val camelCaseJson = """{"identifier_format": "camel case"}"""

    private fun tempConfig(
        extension: String,
        content: String,
    ): File =
        File.createTempFile("linter-config", ".$extension").apply {
            writeText(content)
            deleteOnExit()
        }

    @Test
    fun `create without arguments applies every default rule`() {
        val linter = LinterFactory.create()

        val warnings = warningsOf(linter, oneOfEach)

        assertEquals(3, warnings.size)
    }

    @Test
    fun `create honours the config it is given`() {
        val linter = LinterFactory.create(config = mapOf("identifier_format" to "snake case"))

        val warnings = warningsOf(linter, oneOfEach)

        assertTrue(warnings.isEmpty())
    }

    @Test
    fun `an empty config builds a linter that checks nothing`() {
        val linter = LinterFactory.create(config = emptyMap())

        assertTrue(warningsOf(linter, oneOfEach).isEmpty())
    }

    @Test
    fun `a custom registry replaces the default rules`() {
        val linter = LinterFactory.create(registry = LinterRuleRegistry(emptyList()))

        assertTrue(warningsOf(linter, oneOfEach).isEmpty())
    }

    @Test
    fun `fromStream reads a json config, like the TCK hands it over`() {
        val linter = LinterFactory.fromStream(camelCaseJson.byteInputStream())

        val warnings = warningsOf(linter, snakeCaseDeclaration, LanguageVersion.V1_0)

        assertEquals(1, warnings.size)
    }

    @Test
    fun `fromStream reads a yaml config`() {
        val linter = LinterFactory.fromStream("identifier_format: camel case".byteInputStream())

        assertEquals(1, warningsOf(linter, snakeCaseDeclaration).size)
    }

    @Test
    fun `fromStream skips a UTF-8 BOM`() {
        val linter = LinterFactory.fromStream("﻿$camelCaseJson".byteInputStream())

        assertEquals(1, warningsOf(linter, snakeCaseDeclaration).size)
    }

    @Test
    fun `fromFile reads json, yaml and yml files, like the cli passes them`() {
        val files =
            listOf(
                tempConfig("json", camelCaseJson),
                tempConfig("yaml", "identifier_format: camel case"),
                tempConfig("yml", "identifier_format: \"camel case\""),
                tempConfig("JSON", camelCaseJson),
            )

        files.forEach { file ->
            assertEquals(1, warningsOf(LinterFactory.fromFile(file.path), snakeCaseDeclaration).size, file.name)
        }
    }

    @Test
    fun `fromFile rejects an extension that is not json or yaml`() {
        val file = tempConfig("txt", "identifier_format: camel case")

        assertFailsWith<IllegalArgumentException> { LinterFactory.fromFile(file.path) }
    }

    @Test
    fun `fromFile rejects a file that does not exist`() {
        assertFailsWith<IllegalArgumentException> { LinterFactory.fromFile("no-existe.json") }
    }

    @Test
    fun `fromFile rejects an identifier format that no rule knows`() {
        val file = tempConfig("json", """{"identifier_format": "camelCase"}""")

        assertFailsWith<IllegalArgumentException> { LinterFactory.fromFile(file.path) }
    }

    @Test
    fun `fromStream with an empty registry disables all rules`() {
        val emptyRegistry = LinterRuleRegistry(emptyList())
        val linter = LinterFactory.fromStream(camelCaseJson.byteInputStream(), emptyRegistry)

        val warnings = warningsOf(linter, snakeCaseDeclaration)

        assertTrue(warnings.isEmpty())
    }

    @Test
    fun `fromFile with an empty registry disables all rules`() {
        val emptyRegistry = LinterRuleRegistry(emptyList())
        val file = tempConfig("json", camelCaseJson)
        val linter = LinterFactory.fromFile(file.path, emptyRegistry)

        val warnings = warningsOf(linter, snakeCaseDeclaration)

        assertTrue(warnings.isEmpty())
    }

    @Test
    fun `fromStream with custom rule factory parses custom config key and applies rule`() {
        val customFactory = BooleanRuleFactory("custom-flag", CustomRule())
        val registry = LinterRuleRegistry(listOf(customFactory))
        val json = """{"custom-flag": true}"""
        val linter = LinterFactory.fromStream(json.byteInputStream(), registry)

        val warnings = warningsOf(linter, snakeCaseDeclaration)

        assertEquals(1, warnings.size)
        assertEquals("custom warning", warnings[0].message)
    }

    @Test
    fun `fromFile with File object and custom rule factory parses custom config key and applies rule`() {
        val customFactory = BooleanRuleFactory("custom-flag", CustomRule())
        val registry = LinterRuleRegistry(listOf(customFactory))
        val file = tempConfig("json", """{"custom-flag": true}""")
        val linter = LinterFactory.fromFile(file, registry)

        val warnings = warningsOf(linter, snakeCaseDeclaration)

        assertEquals(1, warnings.size)
        assertEquals("custom warning", warnings[0].message)
    }

    private class CustomRule : LinterRule {
        override fun appliesTo(node: PositionedNode): Boolean = node is VariableDeclaration

        override fun check(node: PositionedNode): List<Warning> = listOf(Warning("custom warning", node.position))
    }
}
