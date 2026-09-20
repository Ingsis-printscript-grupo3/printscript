package printscript.semantic

import printscript.common.Position

sealed interface SemanticResult<out T> {
    data class Success<T>(val value: T) : SemanticResult<T>

    data class Failure(val message: String, val position: Position) : SemanticResult<Nothing>
}
