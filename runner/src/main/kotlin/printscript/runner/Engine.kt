package printscript.runner

import printscript.ast.Statement
import printscript.common.LanguageVersion
import printscript.common.Position
import printscript.interpreter.InterpreterError
import printscript.interpreter.InterpreterFactory
import printscript.interpreter.env.EnvProvider
import printscript.interpreter.env.SystemEnvProvider
import printscript.interpreter.input.ConsoleInput
import printscript.interpreter.input.InputProvider
import printscript.interpreter.output.Output
import printscript.lexer.CharStream
import printscript.lexer.Lexer
import printscript.lexer.LexicalError
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
    object Success : FormatResult

    data class Failure(
        val type: String,
        val message: String,
        val start: Position? = null,
        val end: Position? = null,
    ) : FormatResult
}

sealed interface LintResult {
    object Success : LintResult

    data class Failure(
        val type: String,
        val message: String,
        val start: Position? = null,
        val end: Position? = null,
    ) : LintResult
}

class Engine(
    private val output: Output,
    private val input: InputProvider = ConsoleInput(),
    private val env: EnvProvider = SystemEnvProvider(),
) {
    fun execute(
        code: String,
        languageVersion: LanguageVersion = LanguageVersion.V1_1,
        onProgress: (Int) -> Unit = {},
    ): ExecutionResult {
        return execute(StringReader(code), languageVersion, onProgress)
    }

    fun execute(
        reader: Reader,
        languageVersion: LanguageVersion = LanguageVersion.V1_1,
        onProgress: (Int) -> Unit = {},
    ): ExecutionResult {
        return runPipeline(reader, languageVersion, onProgress) { validStatements ->
            val interpreter = InterpreterFactory.create(languageVersion, output, input, env)
            interpreter.interpret(validStatements)
        }
    }

    fun validate(
        code: String,
        languageVersion: LanguageVersion = LanguageVersion.V1_1,
        onProgress: (Int) -> Unit = {},
    ): ExecutionResult {
        return validate(StringReader(code), languageVersion, onProgress)
    }

    fun validate(
        reader: Reader,
        languageVersion: LanguageVersion = LanguageVersion.V1_1,
        onProgress: (Int) -> Unit = {},
    ): ExecutionResult {
        return runPipeline(reader, languageVersion, onProgress) { validStatements -> validStatements.forEach { } }
    }

    // asi un error inesperado no le sale al usuario como stacktrace
    @Suppress("TooGenericExceptionCaught")
    fun format(
        reader: Reader,
        languageVersion: LanguageVersion = LanguageVersion.V1_1,
        onProgress: (Int) -> Unit = {},
        format: (Iterator<Statement>) -> Unit,
    ): FormatResult {
        return try {
            format(parseIntoAst(parserFor(reader, languageVersion), onProgress))
            FormatResult.Success
        } catch (e: Exception) {
            val failure = describe(e)
            FormatResult.Failure(failure.type, failure.message, failure.start, failure.end)
        }
    }

    // asi un error inesperado no le sale al usuario como stacktrace
    @Suppress("TooGenericExceptionCaught")
    fun lint(
        reader: Reader,
        languageVersion: LanguageVersion = LanguageVersion.V1_1,
        onProgress: (Int) -> Unit = {},
        lint: (Iterator<Statement>) -> Unit,
    ): LintResult {
        return try {
            lint(parseIntoAst(parserFor(reader, languageVersion), onProgress))
            LintResult.Success
        } catch (e: Exception) {
            val failure = describe(e)
            LintResult.Failure(failure.type, failure.message, failure.start, failure.end)
        }
    }

    private fun parserFor(
        reader: Reader,
        languageVersion: LanguageVersion,
    ): Parser = Parser(Lexer(CharStream(reader)).tokenize(), languageVersion)

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
        languageVersion: LanguageVersion,
        onProgress: (Int) -> Unit,
        consume: (Iterator<Statement>) -> Unit,
    ): ExecutionResult {
        return try {
            val lexer = Lexer(CharStream(reader))
            val parser = Parser(lexer.tokenize(), languageVersion)
            val semanticAnalyzer = SemanticAnalyzer(languageVersion)

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
        } catch (e: Throwable) {
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
    private fun describe(error: Throwable): ErrorInfo =
        when (error) {
            is OutOfMemoryError -> ErrorInfo("OutOfMemory", "Java heap space")
            is LexicalError -> ErrorInfo("Lexical", error.message, error.start, error.end)
            is SyntaxError -> ErrorInfo("Syntax", error.message, error.start, error.end)
            is SemanticError -> ErrorInfo("Semantic", error.message, error.start, error.end)
            is InterpreterError -> ErrorInfo("Runtime", error.message ?: "Interpreter error")
            else -> ErrorInfo("Internal", error.message ?: "Unknown error")
        }
}
