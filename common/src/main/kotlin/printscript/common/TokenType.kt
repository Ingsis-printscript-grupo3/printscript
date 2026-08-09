package printscript.common

sealed class TokenType {
   object LET: TokenType()
   object IDENTIFIER: TokenType()
   object PRINTLN: TokenType()
   object NUMBERTYPE: TokenType()
   object NUMBERLITERAL: TokenType()
   object STRINGLITERAL: TokenType()
   object STRINGTYPE: TokenType()
   object PLUS: TokenType()
   object MINUS: TokenType()
   object MULTIPLY: TokenType()
   object DIVIDE: TokenType()
   object ASSIGN: TokenType()
   object COLON : TokenType()
   object SEMICOLON : TokenType()
   object LEFTPAREN : TokenType()
   object RIGHTTPAREN : TokenType()
   object EOF: TokenType()
}