package printscript.runner

import printscript.common.LanguageVersion
import printscript.formatter.Formatter
import printscript.formatter.FormatterRulesLoader
import printscript.interpreter.env.SystemEnvProvider
import printscript.interpreter.input.InputProvider
import printscript.interpreter.output.Output
import printscript.linter.Linter
import printscript.linter.LinterRulesLoader
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.io.Writer
import java.nio.charset.StandardCharsets

object PrintScriptRunner {
    private val NO_OP_OUTPUT = CallbackOutput {}

    /** Executes PrintScript code from an input stream, reporting output and errors through callbacks. */
    fun execute(
        src: InputStream,
        versionStr: String,
        onPrint: (String) -> Unit,
        onInput: (String) -> String,
        onError: (String) -> Unit,
    ) {
        val version = parseVersion(versionStr, onError) ?: return
        handleExecution(onError) {
            val engine = Engine(CallbackOutput(onPrint), CallbackInput(onPrint, onInput), SystemEnvProvider())
            when (val result = engine.execute(utf8Reader(src), version)) {
                is ExecutionResult.Success -> Unit
                is ExecutionResult.Failure -> onError(result.message)
            }
        }
    }

    /** Formats PrintScript code from an InputStream applying configured formatting rules. */
    fun format(
        src: InputStream,
        versionStr: String,
        config: InputStream,
        writer: Writer,
        onError: (String) -> Unit = {},
    ) {
        val version = parseVersion(versionStr, onError) ?: return
        handleExecution(onError) {
            val rules = FormatterRulesLoader.fromStream(config)
            val result =
                withTempCopy(src) { openSource ->
                    Engine(NO_OP_OUTPUT).format(openSource, version) { Formatter(rules).format(it, writer) }
                }
            if (result is FormatResult.Failure) onError(result.message)
        }
    }

    /** Analyzes PrintScript code for style warnings and syntax errors. */
    fun lint(
        src: InputStream,
        versionStr: String,
        config: InputStream,
        onError: (String) -> Unit,
    ) {
        val version = parseVersion(versionStr, onError) ?: return
        handleExecution(onError) {
            val rules = LinterRulesLoader.fromStream(config)
            val result =
                Engine(NO_OP_OUTPUT).lint(utf8Reader(src), version) { statements ->
                    Linter(rules).analyze(statements) { warning -> onError(warning.message) }
                }
            if (result is LintResult.Failure) onError(result.message)
        }
    }

    // Suppress exception to handle invalid version strings and report unknown versions via callback.
    @Suppress("SwallowedException")
    private fun parseVersion(
        versionStr: String,
        onError: (String) -> Unit,
    ): LanguageVersion? =
        try {
            LanguageVersion.parse(versionStr)
        } catch (_: IllegalArgumentException) {
            onError("Unknown version: $versionStr")
            null
        }

    // el TCK espera errores por callback, nunca una excepcion que se escape
    @Suppress("TooGenericExceptionCaught")
    private inline fun handleExecution(
        onError: (String) -> Unit,
        block: () -> Unit,
    ) {
        try {
            block()
        } catch (_: OutOfMemoryError) {
            onError("Java heap space")
        } catch (t: Throwable) {
            onError(t.message ?: t.toString())
        }
    }

    private fun utf8Reader(src: InputStream): Reader = BufferedReader(InputStreamReader(src, StandardCharsets.UTF_8))

    /**
     * Copia el fuente a un archivo temporal y le pasa a [block] una forma de abrirlo cuantas veces haga falta.
     * Existe porque el Engine lee el fuente dos veces al formatear (parsear y despues tokenizar)
     * y un InputStream no se puede rebobinar.
     */
    private fun <T> withTempCopy(
        src: InputStream,
        block: (() -> Reader) -> T,
    ): T {
        val source = File.createTempFile("printscript-format", ".ps").apply { deleteOnExit() }
        return try {
            source.outputStream().use { src.copyTo(it) }
            block { source.bufferedReader(StandardCharsets.UTF_8) }
        } finally {
            source.delete()
        }
    }
}

// manda cada println del programa al callback que dio quien nos llamo (el TCK o el CLI)
private class CallbackOutput(
    private val onPrint: (String) -> Unit,
) : Output {
    override fun emit(line: String) = onPrint(line)
}

// el prompt del readInput se imprime por el canal de salida y la respuesta se pide por el de entrada
private class CallbackInput(
    private val onPrint: (String) -> Unit,
    private val onInput: (String) -> String,
) : InputProvider {
    override fun readInput(prompt: String): String {
        if (prompt.isNotEmpty()) {
            onPrint(prompt)
        }
        return onInput(prompt)
    }
}
