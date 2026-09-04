package printscript.cli

import printscript.ast.Statement
import printscript.common.Position
import printscript.interpreter.Interpreter
import printscript.interpreter.InterpreterError
import printscript.interpreter.output.Output
import printscript.lexer.CharStream
import printscript.lexer.Lexer
import printscript.lexer.LexicalError
import printscript.parser.Parser
import printscript.parser.SyntaxError
import printscript.parser.result.ParseResult
import printscript.semantic.SemanticAnalyzer
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

class SemanticException(message: String) : RuntimeException(message)

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

    fun format(
        reader: Reader,
        onProgress: (Int) -> Unit = {},
        format: (List<Statement>) -> String,
    ): FormatResult {
        return try {
            FormatResult.Success(format(parseStatements(reader, onProgress)))
        } catch (e: LexicalError) {
            FormatResult.Failure("Lexical", "${e.message} ${formatRange(e.start, e.end)}")
        } catch (e: SyntaxError) {
            FormatResult.Failure("Syntax", "${e.message} ${formatRange(e.start, e.end)}")
        } catch (e: Exception) {
            FormatResult.Failure("Internal", e.message ?: "Unknown error")
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

    private fun runPipeline(
        reader: Reader,
        onProgress: (Int) -> Unit,
        consume: (Iterator<Statement>) -> Unit,
    ): ExecutionResult {
        return try {
            val lexer = Lexer(CharStream(reader))
            val parser = Parser(lexer.tokenize())
            val semanticAnalyzer = SemanticAnalyzer()

            var parsedCount = 0
            val astIterator =
                iterator {
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

            val semanticResultIterator = semanticAnalyzer.analyze(astIterator)

            val validStatementIterator =
                iterator {
                    for (result in semanticResultIterator) {
                        when (result) {
                            is SemanticResult.Success -> yield(result.value)
                            is SemanticResult.Failure -> throw SemanticException(result.message)
                        }
                    }
                }

            consume(validStatementIterator)
            ExecutionResult.Success
        } catch (e: LexicalError) {
            ExecutionResult.Failure("Lexical", "${e.message} ${formatRange(e.start, e.end)}")
        } catch (e: SyntaxError) {
            ExecutionResult.Failure("Syntax", "${e.message} ${formatRange(e.start, e.end)}")
        } catch (e: SemanticException) {
            ExecutionResult.Failure("Semantic", e.message ?: "Unknown semantic error")
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
