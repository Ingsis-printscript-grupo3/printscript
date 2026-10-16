package printscript.linter

import printscript.ast.BinaryExpression
import printscript.ast.NumberLiteral
import printscript.ast.PrintCall
import printscript.ast.ReadInput
import printscript.ast.Statement
import printscript.ast.StringLiteral
import printscript.ast.VariableDeclaration
import printscript.common.LanguageVersion
import printscript.common.Position
import printscript.common.TokenType
import printscript.linter.rule.LinterRuleRegistry
import java.io.ByteArrayInputStream
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LinterFactoryTest {
    // println(1 + 2): la marca la regla de println
    private fun printlnWithExpression() =
        PrintCall(
            BinaryExpression(
                NumberLiteral(1.0, Position(1, 9)),
                TokenType.PLUS,
                NumberLiteral(2.0, Position(1, 13)),
                Position(1, 9),
            ),
            Position(1, 1),
        )

    // let x: string = readInput("a" + "b"): la marca la regla de readInput
    private fun readInputWithExpression() =
        VariableDeclaration(
            "x",
            "string",
            ReadInput(
                BinaryExpression(
                    StringLiteral("a", Position(1, 10)),
                    TokenType.PLUS,
                    StringLiteral("b", Position(1, 14)),
                    Position(1, 10),
                ),
                Position(1, 10),
            ),
            Position(1, 1),
        )

    private fun snakeCaseDeclaration() =
        VariableDeclaration("mi_variable", "number", NumberLiteral(1.0, Position(1, 20)), Position(1, 1))

    private fun warningsOf(
        linter: LinterInterface,
        vararg statements: Statement,
    ): List<Warning> {
        val warnings = mutableListOf<Warning>()
        linter.analyze(statements.iterator(), warnings::add)
        return warnings
    }

    private val camelCaseJson =
        """
        {
          "identifier_format": "camel case",
          "mandatory-variable-or-literal-in-println": true,
          "mandatory-variable-or-literal-in-readInput": true
        }
        """.trimIndent()

    private val camelCaseYaml =
        """
        identifier_format: camel case
        mandatory-variable-or-literal-in-println: true
        mandatory-variable-or-literal-in-readInput: true
        """.trimIndent()

    @Test
    fun `create without arguments applies every default rule`() {
        val warnings = warningsOf(LinterFactory.create(), snakeCaseDeclaration(), printlnWithExpression())

        assertEquals(2, warnings.size)
    }

    @Test
    fun `create honours the config it is given`() {
        val config =
            LinterRules(
                identifierFormat = IdentifierFormat.SNAKE_CASE,
                printCallArgumentsMustBeLiteralOrIdentifier = false,
            )

        val warnings = warningsOf(LinterFactory.create(config), snakeCaseDeclaration(), printlnWithExpression())

        assertTrue(warnings.isEmpty())
    }

    @Test
    fun `create11 keeps the readInput rule`() {
        val warnings = warningsOf(LinterFactory.create(LanguageVersion.V1_1), readInputWithExpression())

        assertEquals(1, warnings.size)
    }

    // en 1.0 el parser rechaza readInput antes de armar el AST, asi que la regla sobra
    @Test
    fun `create10 drops the readInput rule and keeps the rest`() {
        val linter = LinterFactory.create(LanguageVersion.V1_0)

        assertTrue(warningsOf(linter, readInputWithExpression()).isEmpty())
        assertEquals(1, warningsOf(LinterFactory.create10(), printlnWithExpression()).size)
        assertEquals(1, warningsOf(LinterFactory.create10(), snakeCaseDeclaration()).size)
    }

    @Test
    fun `create11 is the version aware entry point for 1_1`() {
        assertEquals(1, warningsOf(LinterFactory.create11(), printlnWithExpression()).size)
    }

    @Test
    fun `a custom registry replaces the default rules`() {
        val emptyRegistry = LinterRuleRegistry(emptyList())

        val warnings = warningsOf(LinterFactory.create(LinterRules(), emptyRegistry), snakeCaseDeclaration())

        assertTrue(warnings.isEmpty())
        assertTrue(LinterFactory.defaultRegistry().rulesFor(LinterRules()).isNotEmpty())
    }

    // el traverser inyectable es lo que permite que el linter entre a bloques propios
    @Test
    fun `a custom traverser decides which children are visited`() {
        val blind = CompoundStatementTraverser { emptySequence() }

        val warnings =
            warningsOf(
                LinterFactory.create(LinterRules(), LinterFactory.defaultRegistry(), blind),
                snakeCaseDeclaration(),
            )

        assertEquals(1, warnings.size)
        assertTrue(LinterFactory.defaultTraverser() is DefaultCompoundStatementTraverser)
    }

    @Test
    fun `fromJson and fromYaml build the same linter`() {
        val fromJson = warningsOf(LinterFactory.fromJson(camelCaseJson, LanguageVersion.V1_1), snakeCaseDeclaration())
        val fromYaml = warningsOf(LinterFactory.fromYaml(camelCaseYaml, LanguageVersion.V1_1), snakeCaseDeclaration())

        assertEquals(1, fromJson.size)
        assertEquals(fromJson.map { it.message }, fromYaml.map { it.message })
    }

    @Test
    fun `fromStream reads the config the runner passes through`() {
        val stream = ByteArrayInputStream(camelCaseJson.toByteArray())

        val warnings = warningsOf(LinterFactory.fromStream(stream, LanguageVersion.V1_0), snakeCaseDeclaration())

        assertEquals(1, warnings.size)
    }

    @Test
    fun `fromFile reads the config the cli passes through`() {
        val file = File.createTempFile("linter-factory", ".json")
        file.deleteOnExit()
        file.writeText(camelCaseJson)

        val warnings = warningsOf(LinterFactory.fromFile(file.path, LanguageVersion.V1_1), snakeCaseDeclaration())

        assertEquals(1, warnings.size)
    }
}
