package printscript.interpreter

import printscript.interpreter.output.BucketOutput
import printscript.interpreter.plugin.statement.PrintCallInterpreter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class InterpreterTest {
    @Test
    fun `a statement with no interpreter registered fails`() {
        val code = "println(\"line\");"
        val interpreter = Interpreter(emptyList(), emptyList())

        assertFailsWith<UnknownStatementError> {
            interpreter.interpret(parse(code))
        }
    }

    @Test
    fun `an expression with no evaluator registered fails`() {
        val code = "println(\"line\");"
        val output = BucketOutput()
        val interpreter =
            Interpreter(
                statementInterpreters = listOf(PrintCallInterpreter(output)),
                expressionEvaluators = emptyList(),
            )

        assertFailsWith<UnknownExpressionError> {
            interpreter.interpret(parse(code))
        }

        assertEquals(emptyList(), output.lines())
    }
}
