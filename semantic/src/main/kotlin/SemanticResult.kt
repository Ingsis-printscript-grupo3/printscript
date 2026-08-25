package printscript.semantic

sealed interface SemanticResult<out T> {
    data class Success<T>(val value: T) : SemanticResult<T>
    data class Failure(val message: String) : SemanticResult<Nothing>
}
