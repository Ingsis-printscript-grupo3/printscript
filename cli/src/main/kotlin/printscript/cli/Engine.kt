package printscript.cli

import printscript.ast.Statement
import printscript.interpreter.Interpreter
import printscript.lexer.CharStream
import printscript.lexer.Lexer
import printscript.lexer.LexicalError
import printscript.parser.Parser
import printscript.parser.SyntaxError
import printscript.parser.result.ParseResult
import printscript.semantic.SemanticAnalyzer
import printscript.semantic.SemanticResult
import printscript.interpreter.output.Output
import printscript.interpreter.InterpreterError
import java.io.Reader
import java.io.StringReader

sealed interface ExecutionResult {
    object Success : ExecutionResult
    data class Failure(val type: String, val message: String) : ExecutionResult
}

class SemanticException(message: String) : RuntimeException(message)

class Engine(private val output: Output) {

    fun execute(code: String): ExecutionResult {
        return execute(StringReader(code))
    }

    fun execute(reader: Reader): ExecutionResult {
        return try {
            val lexer = Lexer(CharStream(reader))
            val parser = Parser(lexer.tokenize())
            val semanticAnalyzer = SemanticAnalyzer()
            val interpreter = Interpreter(output)

            val astIterator = iterator {
                for (result in parser.parse()) {
                    when (result) {
                        is ParseResult.Success -> yield(result.statement)
                        is ParseResult.Failure -> throw SyntaxError(result.message, result.start, result.end)
                    }
                }
            }

            val semanticResultIterator = semanticAnalyzer.analyze(astIterator)

            val validStatementIterator = iterator {
                for (result in semanticResultIterator) {
                    when (result) {
                        is SemanticResult.Success -> yield(result.value)
                        is SemanticResult.Failure -> throw SemanticException(result.message)
                    }
                }
            }

            interpreter.interpret(validStatementIterator)
            ExecutionResult.Success
        } catch (e: LexicalError) {
            ExecutionResult.Failure("Lexical", "${e.message} (line ${e.start.line})")
        } catch (e: SyntaxError) {
            ExecutionResult.Failure("Syntax", "${e.message} (line ${e.start.line})")
        } catch (e: SemanticException) {
            ExecutionResult.Failure("Semantic", e.message ?: "Unknown semantic error")
        } catch (e: InterpreterError) {
            ExecutionResult.Failure("Runtime", e.message ?: "Interpreter error")
        } catch (e: Exception) {
            ExecutionResult.Failure("Internal", e.message ?: "Unknown error")
        }
    }
}
