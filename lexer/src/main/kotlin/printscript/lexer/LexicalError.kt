package printscript.lexer

import printscript.common.Position
import printscript.common.ScriptError

class LexicalError(
    override val message: String,
    override val start: Position,
    override val end: Position,
) : RuntimeException(message), ScriptError
