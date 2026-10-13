package printscript.linter.rule

import printscript.ast.Statement
import printscript.common.LanguageVersion
import printscript.common.Position
import printscript.lexer.CharStream
import printscript.lexer.LexerFactory
import printscript.linter.LinterFactory
import printscript.linter.LinterRules
import printscript.linter.Warning
import printscript.parser.Parser
import printscript.parser.result.ParseResult
import java.io.StringReader
import kotlin.test.Test
import kotlin.test.assertEquals

class ReadInputIntegrationTest {
    private fun lint(
        source: String,
        config: LinterRules = LinterRules(),
    ): List<Warning> {
        val tokens = LexerFactory.create(CharStream(StringReader(source)), LanguageVersion.V1_1).tokenize()
        val statements = mutableListOf<Statement>()
        Parser(tokens, LanguageVersion.V1_1).parse().forEach { result ->
            when (result) {
                is ParseResult.Success -> statements.add(result.statement)
                is ParseResult.Failure -> error("No deberia fallar el parseo: ${result.message} en ${result.start}")
            }
        }
        val warnings = mutableListOf<Warning>()
        LinterFactory.create(config).analyze(statements.iterator(), warnings::add)
        return warnings
    }

    @Test
    fun `finds readInput inside an assignment`() {
        val source =
            """
            let name: string = "x";
            name = readInput("a" + "b");
            """.trimIndent()
        val config = LinterRules(readInputArgumentsMustBeLiteralOrIdentifier = true)

        val warnings = lint(source, config)

        assertEquals(1, warnings.size)
        assertEquals(
            "readInput can only be called with an identifier or a literal, not an expression",
            warnings[0].message,
        )
        assertEquals(Position(2, 8), warnings[0].position)
    }

    @Test
    fun `finds readInput inside a println`() {
        val source = """println(readInput("a" + "b"));"""
        val config =
            LinterRules(
                printCallArgumentsMustBeLiteralOrIdentifier = false,
                readInputArgumentsMustBeLiteralOrIdentifier = true,
            )

        val warnings = lint(source, config)

        assertEquals(1, warnings.size)
        assertEquals(
            "readInput can only be called with an identifier or a literal, not an expression",
            warnings[0].message,
        )
    }

    @Test
    fun `finds readInput inside an if condition`() {
        val source =
            """
            if (readInput("a" + "b")) {
                println("ok");
            }
            """.trimIndent()
        val config = LinterRules(readInputArgumentsMustBeLiteralOrIdentifier = true)

        val warnings = lint(source, config)

        assertEquals(1, warnings.size)
        assertEquals(
            "readInput can only be called with an identifier or a literal, not an expression",
            warnings[0].message,
        )
        assertEquals(Position(1, 5), warnings[0].position)
    }

    @Test
    fun `finds readInput nested inside a binary expression`() {
        val source = """let total: string = "prefix: " + readInput("a" + "b");"""
        val config = LinterRules(readInputArgumentsMustBeLiteralOrIdentifier = true)

        val warnings = lint(source, config)

        assertEquals(1, warnings.size)
        assertEquals(
            "readInput can only be called with an identifier or a literal, not an expression",
            warnings[0].message,
        )
    }

    @Test
    fun `finds a readInput nested inside the argument of another readInput`() {
        val source = """let name: string = readInput(readInput("a" + "b"));"""
        val config = LinterRules(readInputArgumentsMustBeLiteralOrIdentifier = true)

        val warnings = lint(source, config)

        assertEquals(2, warnings.size)
        assertEquals(
            listOf(
                "readInput can only be called with an identifier or a literal, not an expression",
                "readInput can only be called with an identifier or a literal, not an expression",
            ),
            warnings.map { it.message },
        )
    }

    @Test
    fun `readInput nested inside another readInput with literal argument only warns once for the outer call`() {
        val source = """let name: string = readInput(readInput("a"));"""
        val config = LinterRules(readInputArgumentsMustBeLiteralOrIdentifier = true)

        val warnings = lint(source, config)

        assertEquals(1, warnings.size)
        assertEquals(
            "readInput can only be called with an identifier or a literal, not an expression",
            warnings[0].message,
        )
    }
}
