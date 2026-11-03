package printscript.parser.expression

import printscript.ast.Assignment
import printscript.ast.Identifier
import printscript.ast.PrintCall
import printscript.ast.ReadEnv
import printscript.ast.ReadInput
import printscript.common.LanguageVersion
import printscript.common.Position
import printscript.parser.parseResults
import printscript.parser.parseStatements
import printscript.parser.result.ParseResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ReadInputReadEnvTest {
    private fun failureUnder10(code: String): ParseResult.Failure =
        parseResults(code, LanguageVersion.V1_0, lexerVersion = LanguageVersion.V1_1)[0] as ParseResult.Failure

    @Test
    fun `a standalone readInput inside an assignment`() {
        val statements = parseStatements("x = readInput(\"name:\");")

        val assignment = statements[0] as Assignment
        val readInput = assignment.value as ReadInput
        assertEquals(Position(1, 5), readInput.position)
    }

    @Test
    fun `readInput nested inside a println`() {
        val statements = parseStatements("\nprintln(readInput(x));")

        val printCall = statements[0] as PrintCall
        val readInput = printCall.value as ReadInput
        assertIs<Identifier>(readInput.argument)
        assertEquals(Position(2, 9), readInput.position)
    }

    @Test
    fun `readEnv nested inside a println`() {
        val statements = parseStatements("println(readEnv(x));")

        val printCall = statements[0] as PrintCall
        val readEnv = printCall.value as ReadEnv
        assertEquals(Position(1, 9), readEnv.position)
    }

    @Test
    fun `the opening parenthesis of readInput is missing`() {
        val results = parseResults("println(readInput x);")

        val failure = results[0] as ParseResult.Failure
        assert(failure.message.contains("Expected '('"))
    }

    @Test
    fun `readInput is not a valid expression under version 1_0`() {
        val failure = failureUnder10("println(readInput(x));")

        assert(failure.message.contains("found 'readInput'"))
    }

    @Test
    fun `readEnv is not a valid expression under version 1_0`() {
        val failure = failureUnder10("println(readEnv(x));")

        assert(failure.message.contains("found 'readEnv'"))
    }

    @Test
    fun `a boolean literal is not a valid expression under version 1_0`() {
        val failure = failureUnder10("println(true);")

        assert(failure.message.contains("found 'true'"))
    }
}
