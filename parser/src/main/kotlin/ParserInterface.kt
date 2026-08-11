package printscript.parser

import printscript.ast.Statement

interface ParserInterface {
    fun parse(): Iterator<Statement>
}
