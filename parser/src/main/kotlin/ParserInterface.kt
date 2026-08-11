package printscript.parser

interface ParserInterface {
    fun parse(): Iterator<ParseResult>
}
