package printscript.cli

import printscript.ast.Statement
import printscript.common.Position
import printscript.interpreter.Interpreter
import printscript.interpreter.InterpreterError
import printscript.interpreter.output.Output
import printscript.lexer.CharStream
import printscript.lexer.Lexer
import printscript.lexer.LexicalError
import printscript.linter.Warning
import printscript.parser.Parser
import printscript.parser.SyntaxError
import printscript.parser.result.ParseResult
import printscript.semantic.SemanticAnalyzer
import printscript.semantic.SemanticError
import printscript.semantic.SemanticResult
import java.io.Reader
import java.io.StringReader

sealed interface ExecutionResult {
    object Success : ExecutionResult

    data class Failure(
        val type: String,
        val message: String,
        val start: Position? = null,
        val end: Position? = null,
    ) : ExecutionResult
}

sealed interface FormatResult {
    data class Success(val code: String) : FormatResult

    data class Failure(
        val type: String,
        val message: String,
        val start: Position? = null,
        val end: Position? = null,
    ) : FormatResult
}

sealed interface LintResult {
    data class Success(val warnings: List<Warning>) : LintResult

    data class Failure(
        val type: String,
        val message: String,
        val start: Position? = null,
        val end: Position? = null,
    ) : LintResult
}

class Engine(private val output: Output) {
    fun execute(
        code: String,
        onProgress: (Int) -> Unit = {},
    ): ExecutionResult {
        return execute(StringReader(code), onProgress)
    }

    fun execute(
        reader: Reader,
        onProgress: (Int) -> Unit = {},
    ): ExecutionResult {
        return runPipeline(reader, onProgress) { validStatements -> Interpreter(output).interpret(validStatements) }
    }

    fun validate(
        code: String,
        onProgress: (Int) -> Unit = {},
    ): ExecutionResult {
        return validate(StringReader(code), onProgress)
    }

    fun validate(
        reader: Reader,
        onProgress: (Int) -> Unit = {},
    ): ExecutionResult {
        return runPipeline(reader, onProgress) { validStatements -> validStatements.forEach { } }
    }

    // asi un error inesperado no le sale al usuario como stacktrace
    @Suppress("TooGenericExceptionCaught")
    fun format(
        reader: Reader,
        onProgress: (Int) -> Unit = {},
        format: (List<Statement>) -> String,
    ): FormatResult {
        return try {
            FormatResult.Success(format(parseStatements(reader, onProgress)))
        } catch (e: Exception) {
            val failure = describe(e)
            FormatResult.Failure(failure.type, failure.message, failure.start, failure.end)
        }
    }

    // asi un error inesperado no le sale al usuario como stacktrace
    @Suppress("TooGenericExceptionCaught")
    fun lint(
        reader: Reader,
        onProgress: (Int) -> Unit = {},
        lint: (List<Statement>) -> List<Warning>,
    ): LintResult {
        return try {
            LintResult.Success(lint(parseStatements(reader, onProgress)))
        } catch (e: Exception) {
            val failure = describe(e)
            LintResult.Failure(failure.type, failure.message, failure.start, failure.end)
        }
    }

    private fun parseStatements(
        reader: Reader,
        onProgress: (Int) -> Unit,
    ): List<Statement> {
        val lexer = Lexer(CharStream(reader))
        val parser = Parser(lexer.tokenize())
        var parsedCount = 0
        return buildList {
            for (result in parser.parse()) {
                when (result) {
                    is ParseResult.Success -> {
                        add(result.statement)
                        onProgress(++parsedCount)
                    }
                    is ParseResult.Failure -> throw SyntaxError(result.message, result.start, result.end)
                }
            }
        }
    }

    private fun parseIntoAst(
        parser: Parser,
        onProgress: (Int) -> Unit,
    ): Iterator<Statement> {
        var parsedCount = 0
        return iterator {
            for (result in parser.parse()) {
                when (result) {
                    is ParseResult.Success -> {
                        yield(result.statement)
                        onProgress(++parsedCount)
                    }
                    is ParseResult.Failure -> throw SyntaxError(result.message, result.start, result.end)
                }
            }
        }
    }

    // asi un error inesperado no le sale al usuario como stacktrace
    @Suppress("TooGenericExceptionCaught")
    private fun runPipeline(
        reader: Reader,
        onProgress: (Int) -> Unit,
        consume: (Iterator<Statement>) -> Unit,
    ): ExecutionResult {
        return try {
            val lexer = Lexer(CharStream(reader))
            val parser = Parser(lexer.tokenize())
            val semanticAnalyzer = SemanticAnalyzer()

            val astIterator = parseIntoAst(parser, onProgress)
            val semanticResultIterator = semanticAnalyzer.analyze(astIterator)

            val validStatementIterator =
                iterator {
                    for (result in semanticResultIterator) {
                        when (result) {
                            is SemanticResult.Success -> yield(result.value)
                            is SemanticResult.Failure -> throw SemanticError(result.message, result.position)
                        }
                    }
                }

            consume(validStatementIterator)
            ExecutionResult.Success
        } catch (e: Exception) {
            val failure = describe(e)
            ExecutionResult.Failure(failure.type, failure.message, failure.start, failure.end)
        }
    }

    private data class ErrorInfo(
        val type: String,
        val message: String,
        val start: Position? = null,
        val end: Position? = null,
    )

    // un solo lugar que traduce la excepcion de cada capa al resultado del cli
    private fun describe(error: Exception): ErrorInfo =
        when (error) {
            is LexicalError -> ErrorInfo("Lexical", error.message, error.start, error.end)
            is SyntaxError -> ErrorInfo("Syntax", error.message, error.start, error.end)
            is SemanticError -> ErrorInfo("Semantic", error.message, error.start, error.end)
            is InterpreterError -> ErrorInfo("Runtime", error.message ?: "Interpreter error")
            else -> ErrorInfo("Internal", error.message ?: "Unknown error")
        }
}
