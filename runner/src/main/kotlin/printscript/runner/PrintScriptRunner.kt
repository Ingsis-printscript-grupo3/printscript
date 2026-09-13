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
import java.io.Writer
import java.nio.charset.StandardCharsets

object PrintScriptRunner {
    /**
     * Executes PrintScript code from an InputStream with the specified version and IO callbacks.
     * All errors (lexical, syntax, semantic, runtime, and OutOfMemoryError) are reported to onError.
     * No exceptions escape outside this method.
     */
    fun execute(
        src: InputStream,
        versionStr: String,
        onPrint: (String) -> Unit,
        onInput: (String) -> String,
        onError: (String) -> Unit,
    ) {
        val version = parseVersion(versionStr, onError) ?: return

        val output =
            object : Output {
                override fun emit(line: String) {
                    onPrint(line)
                }
            }

        val input =
            object : InputProvider {
                override fun readInput(prompt: String): String {
                    if (prompt.isNotEmpty()) {
                        onPrint(prompt)
                    }
                    return onInput(prompt)
                }
            }

        val engine = Engine(output, input, SystemEnvProvider())
        val reader = BufferedReader(InputStreamReader(src, StandardCharsets.UTF_8))
        when (val result = engine.execute(reader, version)) {
            is ExecutionResult.Success -> Unit
            is ExecutionResult.Failure -> onError(result.message)
        }
    }

    /**
     * Formats PrintScript code from an InputStream using rules from configStream.
     */
    @Suppress("SwallowedException", "TooGenericExceptionCaught")
    fun format(
        src: InputStream,
        versionStr: String,
        config: InputStream,
        writer: Writer,
        onError: (String) -> Unit = {},
    ) {
        val version = parseVersion(versionStr, onError) ?: return

        try {
            val rules = FormatterRulesLoader.fromStream(config)
            // el stream se puede leer una sola vez y el formatter recorre el codigo dos veces
            val source = File.createTempFile("printscript-format", ".ps")
            source.deleteOnExit()
            source.outputStream().use { src.copyTo(it) }
            val dummyOutput =
                object : Output {
                    override fun emit(line: String) = Unit
                }
            val engine = Engine(dummyOutput)
            val openSource = { source.bufferedReader(StandardCharsets.UTF_8) }

            when (val result = engine.format(openSource, version) { Formatter(rules).format(it, writer) }) {
                is FormatResult.Success -> Unit
                is FormatResult.Failure -> onError(result.message)
            }
            source.delete()
        } catch (e: OutOfMemoryError) {
            onError("Java heap space")
        } catch (t: Throwable) {
            onError(t.message ?: t.toString())
        }
    }

    /**
     * Analyzes PrintScript code for lint warnings and syntax errors.
     */
    @Suppress("SwallowedException", "TooGenericExceptionCaught")
    fun lint(
        src: InputStream,
        versionStr: String,
        config: InputStream,
        onError: (String) -> Unit,
    ) {
        val version = parseVersion(versionStr, onError) ?: return

        try {
            val rules = LinterRulesLoader.fromStream(config)
            val reader = BufferedReader(InputStreamReader(src, StandardCharsets.UTF_8))
            val dummyOutput =
                object : Output {
                    override fun emit(line: String) = Unit
                }
            val engine = Engine(dummyOutput)

            when (
                val result =
                    engine.lint(reader, version) { statements ->
                        Linter(rules).analyze(statements) { warning -> onError(warning.message) }
                    }
            ) {
                is LintResult.Success -> Unit
                is LintResult.Failure -> onError(result.message)
            }
        } catch (e: OutOfMemoryError) {
            onError("Java heap space")
        } catch (t: Throwable) {
            onError(t.message ?: t.toString())
        }
    }

    @Suppress("SwallowedException")
    private fun parseVersion(
        versionStr: String,
        onError: (String) -> Unit,
    ): LanguageVersion? =
        try {
            LanguageVersion.parse(versionStr)
        } catch (e: IllegalArgumentException) {
            onError("Unknown version: $versionStr")
            null
        }
}
