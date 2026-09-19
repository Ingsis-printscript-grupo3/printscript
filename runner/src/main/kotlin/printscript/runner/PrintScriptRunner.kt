package printscript.runner

import printscript.common.LanguageVersion
import printscript.formatter.Formatter
import printscript.formatter.FormatterRulesLoader
import printscript.interpreter.env.SystemEnvProvider
import printscript.interpreter.input.InputProvider
import printscript.interpreter.output.Output
import printscript.linter.LinterFactory
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Writer
import java.nio.charset.StandardCharsets

object PrintScriptRunner {
    private val NO_OP_OUTPUT =
        object : Output {
            override fun emit(line: String) = Unit
        }

    private fun createOutput(onPrint: (String) -> Unit): Output =
        object : Output {
            override fun emit(line: String) = onPrint(line)
        }

    private fun createInput(
        onPrint: (String) -> Unit,
        onInput: (String) -> String,
    ): InputProvider =
        object : InputProvider {
            override fun readInput(prompt: String): String {
                if (prompt.isNotEmpty()) {
                    onPrint(prompt)
                }
                return onInput(prompt)
            }
        }

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

    /** Executes PrintScript code from an input stream, reporting output and errors through callbacks. */
    @Suppress("SwallowedException")
    fun execute(
        src: InputStream,
        versionStr: String,
        onPrint: (String) -> Unit,
        onInput: (String) -> String,
        onError: (String) -> Unit,
    ) {
        val version = parseVersion(versionStr, onError) ?: return
        handleExecution(onError) {
            val engine = Engine(createOutput(onPrint), createInput(onPrint, onInput), SystemEnvProvider())
            val reader = BufferedReader(InputStreamReader(src, StandardCharsets.UTF_8))
            when (val result = engine.execute(reader, version)) {
                is ExecutionResult.Success -> Unit
                is ExecutionResult.Failure -> onError(result.message)
            }
        }
    }

    /** Formats PrintScript code from an InputStream applying configured formatting rules. */
    @Suppress("SwallowedException")
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
            val bytes = src.readAllBytes()
            val openSource = { ByteArrayInputStream(bytes).reader(StandardCharsets.UTF_8) }
            val result = Engine(NO_OP_OUTPUT).format(openSource, version) { Formatter(rules).format(it, writer) }
            if (result is FormatResult.Failure) onError(result.message)
        }
    }

    /** Analyzes PrintScript code for style warnings and syntax errors. */
    @Suppress("SwallowedException")
    fun lint(
        src: InputStream,
        versionStr: String,
        config: InputStream,
        onError: (String) -> Unit,
    ) {
        val version = parseVersion(versionStr, onError) ?: return
        handleExecution(onError) {
            val linter = LinterFactory.fromStream(config, version)
            val reader = BufferedReader(InputStreamReader(src, StandardCharsets.UTF_8))
            val result =
                Engine(NO_OP_OUTPUT).lint(reader, version) { statements ->
                    linter.analyze(statements) { warning -> onError(warning.message) }
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
}
