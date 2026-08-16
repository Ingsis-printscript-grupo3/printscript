package printscript.parser

import printscript.parser.result.ParseResult

interface ParserInterface {
    fun parse(): Iterator<ParseResult>
}
