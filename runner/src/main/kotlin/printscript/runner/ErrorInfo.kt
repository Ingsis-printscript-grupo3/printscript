package printscript.runner

import printscript.common.Position
import printscript.interpreter.InterpreterError
import printscript.lexer.LexicalError
import printscript.parser.SyntaxError
import printscript.semantic.SemanticError

// Lo que el Engine necesita para no propagar excepciones: atraparlas, traducirlas
// y convertirlas en el resultado de cada operacion.

// el error ya traducido, antes de saber si va a viajar como ExecutionResult, FormatResult o LintResult
internal data class ErrorInfo(
    val type: String,
    val message: String,
    val start: Position? = null,
    val end: Position? = null,
)

// unico lugar donde se atrapa una excepcion de cualquier capa, asi al usuario nunca le sale un stacktrace.
// Devuelve null cuando no hubo errores; cada operacion traduce ese null a su propio resultado.
@Suppress("TooGenericExceptionCaught")
internal inline fun runCatchingErrors(block: () -> Unit): ErrorInfo? =
    try {
        block()
        null
    } catch (e: Throwable) {
        describe(e)
    }

// un solo lugar que traduce la excepcion de cada capa al error que entiende el cli
internal fun describe(error: Throwable): ErrorInfo =
    when (error) {
        is OutOfMemoryError -> ErrorInfo("OutOfMemory", "Java heap space")
        is LexicalError -> ErrorInfo("Lexical", error.message, error.start, error.end)
        is SyntaxError -> ErrorInfo("Syntax", error.message, error.start, error.end)
        is SemanticError -> ErrorInfo("Semantic", error.message, error.start, error.end)
        is InterpreterError -> ErrorInfo("Runtime", error.message, error.start, error.end)
        else -> ErrorInfo("Internal", error.message ?: "Unknown error")
    }

// ejecutar, formatear y lintear son operaciones distintas, y cada una devuelve su propio resultado

internal fun ErrorInfo?.asExecutionResult(): ExecutionResult =
    if (this == null) ExecutionResult.Success else ExecutionResult.Failure(type, message, start, end)

internal fun ErrorInfo?.asFormatResult(): FormatResult =
    if (this == null) FormatResult.Success else FormatResult.Failure(type, message, start, end)

internal fun ErrorInfo?.asLintResult(): LintResult =
    if (this == null) LintResult.Success else LintResult.Failure(type, message, start, end)
