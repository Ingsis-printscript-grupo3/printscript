package printscript.semantic

import printscript.common.Position
import printscript.common.ScriptError

class SemanticError(
    override val message: String,
    override val start: Position,
    override val end: Position = start,
) : RuntimeException(message), ScriptError
