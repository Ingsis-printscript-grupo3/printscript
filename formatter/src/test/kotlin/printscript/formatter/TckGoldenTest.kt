package printscript.formatter

import printscript.lexer.CharStream
import printscript.lexer.Lexer
import java.io.StringReader
import java.io.StringWriter
import kotlin.test.Test
import kotlin.test.assertEquals

// los mismos casos que el formatter del TCK: entrada, config y resultado esperado
class TckGoldenTest {
    private class Case(val name: String, val config: String, val input: String, val golden: String)

    private fun lines(vararg lines: String) = lines.joinToString("\n")

    private fun format(case: Case): String {
        val writer = StringWriter()
        val tokens = Lexer(CharStream(StringReader(case.input))).tokenize()
        Formatter(FormatterRulesLoader.fromJson(case.config)).format(tokens, writer)
        return writer.toString()
    }

    private val cases =
        listOf(
            Case(
                "assign-no-spacing-surrounding-equals",
                """{ "enforce-no-spacing-around-equals": true }""",
                lines(
                    "let something: string= \"a really cool thing\";",
                    "let another_thing: string =\"another really cool thing\";",
                    "let twice_thing: string = \"another really cool thing twice\";",
                    "let third_thing: string=\"another really cool thing three times\";",
                ),
                lines(
                    "let something: string=\"a really cool thing\";",
                    "let another_thing: string=\"another really cool thing\";",
                    "let twice_thing: string=\"another really cool thing twice\";",
                    "let third_thing: string=\"another really cool thing three times\";",
                ),
            ),
            Case(
                "assign-spacing-surrounding-equals",
                """{ "enforce-spacing-around-equals": true }""",
                lines(
                    "let something: string= \"a really cool thing\";",
                    "let another_thing: string =\"another really cool thing\";",
                    "let twice_thing: string=\"another really cool thing twice\";",
                    "let third_thing: string = \"another really cool thing three times\";",
                ),
                lines(
                    "let something: string = \"a really cool thing\";",
                    "let another_thing: string = \"another really cool thing\";",
                    "let twice_thing: string = \"another really cool thing twice\";",
                    "let third_thing: string = \"another really cool thing three times\";",
                ),
            ),
            Case(
                "enforce-decl-spacing-after-colon",
                """{ "enforce-spacing-after-colon-in-declaration": true }""",
                lines(
                    "let something:string = \"a really cool thing\";",
                    "let another_thing: string = \"another really cool thing\";",
                    "let twice_thing : string = \"another really cool thing twice\";",
                    "let third_thing :string=\"another really cool thing three times\";",
                ),
                lines(
                    "let something: string = \"a really cool thing\";",
                    "let another_thing: string = \"another really cool thing\";",
                    "let twice_thing : string = \"another really cool thing twice\";",
                    "let third_thing : string=\"another really cool thing three times\";",
                ),
            ),
            Case(
                "enforce-decl-spacing-before-colon",
                """{ "enforce-spacing-before-colon-in-declaration": true }""",
                lines(
                    "let something:string = \"a really cool thing\";",
                    "let another_thing :string = \"another really cool thing\";",
                    "let twice_thing : string = \"another really cool thing twice\";",
                    "let third_thing: string=\"another really cool thing three times\";",
                ),
                lines(
                    "let something :string = \"a really cool thing\";",
                    "let another_thing :string = \"another really cool thing\";",
                    "let twice_thing : string = \"another really cool thing twice\";",
                    "let third_thing : string=\"another really cool thing three times\";",
                ),
            ),
            Case(
                "enforce-single-space-separation",
                """{ "mandatory-single-space-separation": true }""",
                lines("let something:      string=\"a really cool thing\";", "println(something);"),
                lines("let something : string = \"a really cool thing\";", "println ( something );"),
            ),
            Case(
                "enforce-space-surrounding-operations",
                """{ "mandatory-space-surrounding-operations": true }""",
                "let result: number = 5+4*3/2;",
                "let result: number = 5 + 4 * 3 / 2;",
            ),
            Case(
                "line-break-after-statement-enforced",
                """{ "mandatory-line-break-after-statement": true }""",
                lines(
                    "let something:string = \"a really cool thing\";",
                    "let another_thing: string = \"another really cool thing\";" +
                        "let twice_thing : string = \"another really cool thing twice\";" +
                        "let third_thing :string=\"another really cool thing three times\";",
                ),
                lines(
                    "let something:string = \"a really cool thing\";",
                    "let another_thing: string = \"another really cool thing\";",
                    "let twice_thing : string = \"another really cool thing twice\";",
                    "let third_thing :string=\"another really cool thing three times\";",
                ),
            ),
            Case(
                "print-0-line-breaks-after",
                """{ "line-breaks-after-println": 0 }""",
                lines(
                    "let something:string = \"a really cool thing\";",
                    "println(something);",
                    "",
                    "",
                    "",
                    "",
                    "println(\"in the way she moves\");",
                ),
                lines(
                    "let something:string = \"a really cool thing\";",
                    "println(something);",
                    "println(\"in the way she moves\");",
                ),
            ),
            Case(
                "print-1-line-breaks-after",
                """{ "line-breaks-after-println": 1 }""",
                lines(
                    "let something:string = \"a really cool thing\";",
                    "println(something);",
                    "println(\"in the way she moves\");",
                ),
                lines(
                    "let something:string = \"a really cool thing\";",
                    "println(something);",
                    "",
                    "println(\"in the way she moves\");",
                ),
            ),
            Case(
                "print-2-line-breaks-after",
                """{ "line-breaks-after-println": 2 }""",
                lines(
                    "let something:string = \"a really cool thing\";",
                    "println(something);",
                    "println(\"in the way she moves\");",
                ),
                lines(
                    "let something:string = \"a really cool thing\";",
                    "println(something);",
                    "",
                    "",
                    "println(\"in the way she moves\");",
                ),
            ),
            Case(
                "if-brace-below-line",
                """{ "if-brace-below-line": true }""",
                lines("let something: boolean = true;", "if (something) {", "  println(\"Entered if\");", "}"),
                lines("let something: boolean = true;", "if (something)", "{", "  println(\"Entered if\");", "}"),
            ),
            Case(
                "if-brace-same-line",
                """{ "if-brace-same-line": true }""",
                lines("let something: boolean = true;", "if (something)", "{", "  println(\"Entered if\");", "}"),
                lines("let something: boolean = true;", "if (something) {", "  println(\"Entered if\");", "}"),
            ),
            Case(
                "if-indent-inside-2",
                """{ "indent-inside-if": 4 }""",
                lines(
                    "let something: boolean = true;",
                    "if (something) {",
                    "  if (something) {",
                    "    println(\"Entered two ifs\");",
                    "  }",
                    "}",
                ),
                lines(
                    "let something: boolean = true;",
                    "if (something) {",
                    "    if (something) {",
                    "        println(\"Entered two ifs\");",
                    "    }",
                    "}",
                ),
            ),
        )

    @Test
    fun `matches every golden file of the TCK formatter`() {
        cases.forEach { case -> assertEquals(case.golden, format(case), "caso ${case.name}") }
    }
}
