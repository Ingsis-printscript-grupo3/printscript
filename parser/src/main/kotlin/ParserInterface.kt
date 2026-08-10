package printscript.parser

interface ParserInterface
{
    fun parse(): List<printscript.ast.Statement>;

}
