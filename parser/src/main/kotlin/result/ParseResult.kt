package printscript.parser.result

import printscript.ast.Statement
import printscript.common.Position

sealed interface ParseResult {
    data class Success(val statement: Statement) : ParseResult
    data class Failure(
        val message: String,
        val start: Position,
        val end: Position
    ) : ParseResult
}
