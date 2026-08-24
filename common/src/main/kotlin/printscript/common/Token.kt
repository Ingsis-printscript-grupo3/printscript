package printscript.common

data class Token(val type: TokenType, val start: Position, val end: Position, val value: String)
