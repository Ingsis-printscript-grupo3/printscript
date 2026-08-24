package printscript.parser

import printscript.common.Position
import printscript.common.ScriptError

// error de sintaxis como excepcion
// el parser tamb lo reporta como dato en ParseResult.Failure
class SyntaxError(
    override val message: String,
    override val start: Position,
    override val end: Position,
) : RuntimeException(message), ScriptError
