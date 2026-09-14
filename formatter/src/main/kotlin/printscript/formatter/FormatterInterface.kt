package printscript.formatter

import printscript.common.Token
import java.io.Writer

interface FormatterInterface {
    fun format(
        tokens: Iterator<Token>,
        output: Writer,
    )
}
