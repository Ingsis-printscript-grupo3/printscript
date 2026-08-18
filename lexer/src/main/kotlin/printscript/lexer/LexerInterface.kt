package printscript.lexer

import printscript.common.Token

interface LexerInterface {
    fun tokenize(): Iterator<Token>
}
