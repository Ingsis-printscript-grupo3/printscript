package printscript.linter

import printscript.ast.Statement
import printscript.common.LanguageVersion
import printscript.common.Position
import printscript.lexer.CharStream
import printscript.lexer.Lexer
import printscript.parser.Parser
import printscript.parser.result.ParseResult
import java.io.StringReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Casos equivalentes a los de linter/1.0 y linter/1.1 del TCK: van de fuente PrintScript
// a warnings, con la config entrando como JSON igual que la manda el TCK.
class LinterTckCasesTest {
    private fun lint(
        source: String,
        configJson: String,
        version: LanguageVersion = LanguageVersion.V1_1,
    ): List<Warning> {
        val tokens = Lexer(CharStream(StringReader(source))).tokenize()
        val statements = mutableListOf<Statement>()
        Parser(tokens, version).parse().forEach { result ->
            when (result) {
                is ParseResult.Success -> statements.add(result.statement)
                is ParseResult.Failure -> error("no debería fallar el parseo: ${result.message} en ${result.start}")
            }
        }
        val warnings = mutableListOf<Warning>()
        Linter(LinterRulesLoader.fromJson(configJson)).analyze(statements.iterator(), warnings::add)
        return warnings
    }

    private val camelCaseConfig =
        """
        {
          "identifier_format": "camel case",
          "mandatory-variable-or-literal-in-println": true,
          "mandatory-variable-or-literal-in-readInput": true
        }
        """.trimIndent()

    private val snakeCaseConfig =
        """
        {
          "identifier_format": "snake case",
          "mandatory-variable-or-literal-in-println": true,
          "mandatory-variable-or-literal-in-readInput": true
        }
        """.trimIndent()

    // ---------- linter 1.0 ----------

    @Test
    fun `10 - camel case declarations pass under camel case`() {
        val source =
            """
            let miVariable: number = 3;
            let otraCosa: string = "hola";
            """.trimIndent()

        assertTrue(lint(source, camelCaseConfig, LanguageVersion.V1_0).isEmpty())
    }

    @Test
    fun `10 - snake case declarations warn under camel case`() {
        val source = "let mi_variable: number = 3;"

        val warnings = lint(source, camelCaseConfig, LanguageVersion.V1_0)

        assertEquals(1, warnings.size)
        assertEquals("Identifier 'mi_variable' does not match format camel case", warnings[0].message)
    }

    @Test
    fun `10 - snake case declarations pass under snake case`() {
        val source = "let mi_variable: number = 3;"

        assertTrue(lint(source, snakeCaseConfig, LanguageVersion.V1_0).isEmpty())
    }

    // este es el caso que el ticket 1.5 vino a arreglar: antes pasaba sin warning
    @Test
    fun `10 - an assignment with a badly formatted name warns`() {
        val source =
            """
            let mi_variable: number = 3;
            mi_variable = 4;
            """.trimIndent()

        val warnings = lint(source, camelCaseConfig, LanguageVersion.V1_0)

        assertEquals(2, warnings.size)
        // el warning apunta al nombre: columna 5 en el let, columna 1 en la asignacion
        assertEquals(listOf(Position(1, 5), Position(2, 1)), warnings.map { it.position })
    }

    @Test
    fun `10 - println with a literal or an identifier passes`() {
        val source =
            """
            let miVariable: number = 3;
            println(miVariable);
            println("hola");
            println(5);
            """.trimIndent()

        assertTrue(lint(source, camelCaseConfig, LanguageVersion.V1_0).isEmpty())
    }

    @Test
    fun `10 - println with an expression warns`() {
        val source = "println(1 + 2);"

        val warnings = lint(source, camelCaseConfig, LanguageVersion.V1_0)

        assertEquals(1, warnings.size)
        assertEquals(
            "println can only be called with an identifier or a literal, not an expression",
            warnings[0].message,
        )
        assertEquals(Position(1, 1), warnings[0].position)
    }

    // ---------- linter 1.1 ----------

    @Test
    fun `11 - a const declaration with a badly formatted name warns`() {
        val source = "const mi_constante: number = 3;"

        val warnings = lint(source, camelCaseConfig)

        assertEquals(1, warnings.size)
        assertEquals("Identifier 'mi_constante' does not match format camel case", warnings[0].message)
        assertEquals(Position(1, 7), warnings[0].position)
    }

    @Test
    fun `11 - a well formatted const declaration passes`() {
        assertTrue(lint("const miConstante: number = 3;", camelCaseConfig).isEmpty())
    }

    @Test
    fun `11 - readInput with a literal or an identifier passes`() {
        val source =
            """
            let pregunta: string = "nombre?";
            let nombre: string = readInput("nombre?");
            let otro: string = readInput(pregunta);
            """.trimIndent()

        assertTrue(lint(source, camelCaseConfig).isEmpty())
    }

    @Test
    fun `11 - readInput with an expression warns`() {
        val source = """let nombre: string = readInput("a" + "b");"""

        val warnings = lint(source, camelCaseConfig)

        assertEquals(1, warnings.size)
        assertEquals(
            "readInput can only be called with an identifier or a literal, not an expression",
            warnings[0].message,
        )
    }

    @Test
    fun `11 - the readInput warning carries the real row and column of the call`() {
        val source =
            """
            let nombre: string = "x";
            nombre = readInput("a" + "b");
            """.trimIndent()

        val warnings = lint(source, camelCaseConfig)

        assertEquals(1, warnings.size)
        assertEquals(Position(2, 10), warnings[0].position)
    }

    @Test
    fun `11 - readInput is not checked when the config turns the rule off`() {
        val config =
            """
            {
              "identifier_format": "camel case",
              "mandatory-variable-or-literal-in-println": true,
              "mandatory-variable-or-literal-in-readInput": false
            }
            """.trimIndent()

        assertTrue(lint("""let nombre: string = readInput("a" + "b");""", config).isEmpty())
    }

    // ---------- 1.1: sentencias dentro de bloques ----------

    // el caso exacto del informe de QA: hoy esto reportaba 0 warnings
    @Test
    fun `11 - every infraction inside an if block is reported`() {
        val source =
            """
            let x: number = 1;
            if (x) {
                let mi_variable: number = 1;
                mi_variable = 2;
                let dato: string = readInput("a" + "b");
            }
            """.trimIndent()

        val warnings = lint(source, camelCaseConfig)

        assertEquals(3, warnings.size)
        assertEquals(
            listOf(
                "Identifier 'mi_variable' does not match format camel case",
                "Identifier 'mi_variable' does not match format camel case",
                "readInput can only be called with an identifier or a literal, not an expression",
            ),
            warnings.map { it.message },
        )
    }

    @Test
    fun `11 - the warnings point at the inner lines, not at the if`() {
        val source =
            """
            let x: number = 1;
            if (x) {
                let mi_variable: number = 1;
                mi_variable = 2;
            }
            """.trimIndent()

        val warnings = lint(source, camelCaseConfig)

        assertEquals(listOf(3, 4), warnings.map { it.position.line })
        assertTrue(warnings.none { it.position == Position(2, 1) })
    }

    @Test
    fun `11 - a badly formatted const inside an if block warns`() {
        val source =
            """
            let x: number = 1;
            if (x) {
                const mi_constante: number = 1;
            }
            """.trimIndent()

        val warnings = lint(source, camelCaseConfig)

        assertEquals(1, warnings.size)
        assertEquals(3, warnings[0].position.line)
    }

    @Test
    fun `11 - readInput with an expression warns inside then and inside else`() {
        val source =
            """
            let x: number = 1;
            if (x) {
                let uno: string = readInput("a" + "b");
            } else {
                let dos: string = readInput("c" + "d");
            }
            """.trimIndent()

        val warnings = lint(source, camelCaseConfig)

        assertEquals(2, warnings.size)
        assertEquals(listOf(3, 5), warnings.map { it.position.line })
    }

    @Test
    fun `11 - infractions nested two blocks deep are reported`() {
        val source =
            """
            let x: number = 1;
            if (x) {
                if (x) {
                    let mi_variable: number = 1;
                }
            }
            """.trimIndent()

        val warnings = lint(source, camelCaseConfig)

        assertEquals(1, warnings.size)
        assertEquals(4, warnings[0].position.line)
    }

    @Test
    fun `11 - clean code with if and else produces no warnings`() {
        val source =
            """
            let miVariable: number = 1;
            if (miVariable) {
                const miConstante: string = "hola";
                println(miConstante);
            } else {
                let otroDato: string = readInput(miVariable);
            }
            """.trimIndent()

        assertTrue(lint(source, camelCaseConfig).isEmpty())
    }

    // ---------- transversales ----------

    @Test
    fun `clean code produces no warnings at all`() {
        val source =
            """
            let miVariable: number = 3;
            const miConstante: string = "hola";
            miVariable = 4;
            println(miVariable);
            let nombre: string = readInput(miConstante);
            """.trimIndent()

        assertTrue(lint(source, camelCaseConfig).isEmpty())
    }

    @Test
    fun `every warning of a multi warning run carries a real row and column, never zero`() {
        val source =
            """
            let mi_variable: number = 3;
            mi_variable = 4;
            println(1 + 2);
            const otra_cosa: string = readInput("a" + "b");
            """.trimIndent()

        val warnings = lint(source, camelCaseConfig)

        assertEquals(5, warnings.size)
        assertTrue(warnings.none { it.position == Position(0, 0) })
        assertEquals(
            listOf(
                Position(1, 5),
                Position(2, 1),
                Position(3, 1),
                Position(4, 7),
                Position(4, 27),
            ),
            warnings.map { it.position },
        )
    }
}
