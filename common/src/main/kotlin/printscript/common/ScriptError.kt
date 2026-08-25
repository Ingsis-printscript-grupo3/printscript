package printscript.common

interface ScriptError {
    val message: String
    val start: Position
    val end: Position
}
