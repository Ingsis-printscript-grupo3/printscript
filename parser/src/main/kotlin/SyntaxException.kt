package printscript.parser

import printscript.common.Position

class SyntaxException(
    val errorMessage: String,
    val start: Position,
    val end: Position
) : RuntimeException(errorMessage)
