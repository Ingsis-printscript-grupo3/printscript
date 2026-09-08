package printscript.cli

import printscript.ast.Statement
import printscript.common.LanguageVersion
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

    data class Failure(val type: String, val message: String) : ExecutionResult
}

sealed interface FormatResult {
    data class Success(val code: String) : FormatResult

    data class Failure(val type: String, val message: String) : FormatResult
}

sealed interface LintResult {
    data class Success(val warnings: List<Warning>) : LintResult

    data class Failure(val type: String, val message: String) : LintResult
}

class Engine(private val output: Output) {
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
            Interpreter(output).interpret(validStatements)
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
        format: (List<Statement>) -> String,
    ): FormatResult {
        return try {
            FormatResult.Success(format(parseStatements(reader, languageVersion, onProgress)))
        } catch (e: LexicalError) {
            FormatResult.Failure("Lexical", "${e.message} ${formatRange(e.start, e.end)}")
        } catch (e: SyntaxError) {
            FormatResult.Failure("Syntax", "${e.message} ${formatRange(e.start, e.end)}")
        } catch (e: Exception) {
            FormatResult.Failure("Internal", e.message ?: "Unknown error")
        }
    }

    // asi un error inesperado no le sale al usuario como stacktrace
    @Suppress("TooGenericExceptionCaught")
    fun lint(
        reader: Reader,
        languageVersion: LanguageVersion = LanguageVersion.V1_1,
        onProgress: (Int) -> Unit = {},
        lint: (List<Statement>) -> List<Warning>,
    ): LintResult {
        return try {
            LintResult.Success(lint(parseStatements(reader, languageVersion, onProgress)))
        } catch (e: LexicalError) {
            LintResult.Failure("Lexical", "${e.message} ${formatRange(e.start, e.end)}")
        } catch (e: SyntaxError) {
            LintResult.Failure("Syntax", "${e.message} ${formatRange(e.start, e.end)}")
        } catch (e: Exception) {
            LintResult.Failure("Internal", e.message ?: "Unknown error")
        }
    }

    private fun parseStatements(
        reader: Reader,
        languageVersion: LanguageVersion,
        onProgress: (Int) -> Unit,
    ): List<Statement> {
        val lexer = Lexer(CharStream(reader))
        val parser = Parser(lexer.tokenize(), languageVersion)
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
        } catch (e: LexicalError) {
            ExecutionResult.Failure("Lexical", "${e.message} ${formatRange(e.start, e.end)}")
        } catch (e: SyntaxError) {
            ExecutionResult.Failure("Syntax", "${e.message} ${formatRange(e.start, e.end)}")
        } catch (e: SemanticError) {
            ExecutionResult.Failure("Semantic", "${e.message} ${formatRange(e.start, e.end)}")
        } catch (e: InterpreterError) {
            ExecutionResult.Failure("Runtime", e.message ?: "Interpreter error")
        } catch (e: Exception) {
            ExecutionResult.Failure("Internal", e.message ?: "Unknown error")
        }
    }

    private fun formatRange(
        start: Position,
        end: Position,
    ): String = "(from line ${start.line}, column ${start.column} to line ${end.line}, column ${end.column})"
}
