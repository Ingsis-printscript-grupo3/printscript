package printscript.parser.result

import printscript.common.Position

sealed interface ASTResult<out T> {
    data class Success<T>(val value: T) : ASTResult<T>

    data class Failure(val message: String, val start: Position, val end: Position) : ASTResult<Nothing>
}

inline fun <T> ASTResult<T>.unwrap(onFailure: (ASTResult.Failure) -> Nothing): T =
    when (this) {
        is ASTResult.Success -> value
        is ASTResult.Failure -> onFailure(this)
    }
