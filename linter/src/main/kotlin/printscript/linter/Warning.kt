package printscript.linter

import printscript.common.Position

data class Warning(
    val message: String,
    val position: Position,
)
