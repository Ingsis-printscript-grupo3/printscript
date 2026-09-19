package printscript.runner

import printscript.ast.Statement
import printscript.common.LanguageVersion
import printscript.common.Position
import printscript.common.Token
import printscript.interpreter.InterpreterError
import printscript.interpreter.InterpreterFactory
import printscript.interpreter.env.EnvProvider
import printscript.interpreter.env.SystemEnvProvider
import printscript.interpreter.input.ConsoleInput
import printscript.interpreter.input.InputProvider
import printscript.interpreter.output.Output
import printscript.lexer.LexerFactory
import printscript.lexer.LexicalError
import printscript.parser.ParserFactory
import printscript.parser.ParserInterface
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

private val OOM_FAILURE = ExecutionResult.Failure("OutOfMemory", "Java heap space")

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
        languageVersion: LanguageVersion,
        onProgress: (Int) -> Unit = {},
    ): ExecutionResult = execute(StringReader(code), languageVersion, onProgress)

    fun execute(
        reader: Reader,
        languageVersion: LanguageVersion,
        onProgress: (Int) -> Unit = {},
    ): ExecutionResult =
        runPipeline(reader, languageVersion, onProgress) { validStatements ->
            val interpreter = InterpreterFactory.create(languageVersion, output, input, env)
            interpreter.interpret(validStatements)
        }

    fun validate(
        code: String,
        languageVersion: LanguageVersion,
        onProgress: (Int) -> Unit = {},
    ): ExecutionResult = validate(StringReader(code), languageVersion, onProgress)

    fun validate(
        reader: Reader,
        languageVersion: LanguageVersion,
        onProgress: (Int) -> Unit = {},
    ): ExecutionResult =
        runPipeline(reader, languageVersion, onProgress) { validStatements ->
            validStatements.forEach { }
        }

    // asi un error inesperado no le sale al usuario como stacktrace
    @Suppress("TooGenericExceptionCaught")
    fun format(
        openReader: () -> Reader,
        languageVersion: LanguageVersion,
        onProgress: (Int) -> Unit = {},
        format: (Iterator<Token>) -> Unit,
    ): FormatResult =
        try {
            openReader().use { parseIntoAst(parserFor(it, languageVersion), onProgress).forEach { } }
            openReader().use { format(LexerFactory.create(it).tokenize()) }
            FormatResult.Success
        } catch (e: Exception) {
            val failure = describe(e)
            FormatResult.Failure(failure.type, failure.message, failure.start, failure.end)
        }

    // asi un error inesperado no le sale al usuario como stacktrace
    @Suppress("TooGenericExceptionCaught")
    fun lint(
        reader: Reader,
        languageVersion: LanguageVersion,
        onProgress: (Int) -> Unit = {},
        lint: (Iterator<Statement>) -> Unit,
    ): LintResult =
        try {
            lint(parseIntoAst(parserFor(reader, languageVersion), onProgress))
            LintResult.Success
        } catch (e: Exception) {
            val failure = describe(e)
            LintResult.Failure(failure.type, failure.message, failure.start, failure.end)
        }

    // asi un error inesperado no le sale al usuario como stacktrace
    @Suppress("TooGenericExceptionCaught")
    private fun runPipeline(
        reader: Reader,
        languageVersion: LanguageVersion,
        onProgress: (Int) -> Unit,
        consume: (Iterator<Statement>) -> Unit,
    ): ExecutionResult =
        try {
            val astIterator = parseIntoAst(parserFor(reader, languageVersion), onProgress)
            consume(analyzeAst(astIterator, languageVersion))
            ExecutionResult.Success
        } catch (e: Throwable) {
            catchExecutionError(e)
        }
}

private fun parserFor(
    reader: Reader,
    languageVersion: LanguageVersion,
): ParserInterface = ParserFactory.create(LexerFactory.create(reader).tokenize(), languageVersion)

private fun parseIntoAst(
    parser: ParserInterface,
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

private fun buildValidStatementIterator(
    semanticResultIterator: Iterator<SemanticResult<Statement>>,
): Iterator<Statement> =
    iterator {
        for (result in semanticResultIterator) {
            when (result) {
                is SemanticResult.Success -> yield(result.value)
                is SemanticResult.Failure -> throw SemanticError(result.message, result.position)
            }
        }
    }

private fun analyzeAst(
    astIterator: Iterator<Statement>,
    languageVersion: LanguageVersion,
): Iterator<Statement> {
    val semanticAnalyzer = SemanticAnalyzer(languageVersion)
    return buildValidStatementIterator(semanticAnalyzer.analyze(astIterator))
}

private fun catchExecutionError(e: Throwable): ExecutionResult =
    if (e is OutOfMemoryError) {
        OOM_FAILURE
    } else {
        val failure = describe(e)
        ExecutionResult.Failure(failure.type, failure.message, failure.start, failure.end)
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
        is LexicalError -> ErrorInfo("Lexical", error.message, error.start, error.end)
        is SyntaxError -> ErrorInfo("Syntax", error.message, error.start, error.end)
        is SemanticError -> ErrorInfo("Semantic", error.message, error.start, error.end)
        is InterpreterError -> ErrorInfo("Runtime", error.message ?: "Interpreter error")
        else -> ErrorInfo("Internal", error.message ?: "Unknown error")
    }
