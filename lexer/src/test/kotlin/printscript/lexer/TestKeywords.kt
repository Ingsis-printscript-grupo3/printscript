package printscript.lexer

import printscript.common.TokenType

// el vocabulario que usan los tests: el lexer no sabe de versiones, asi que se lo pasan armado
// como lo hace el runner. Que palabras trae cada version se prueba en LanguageDefinitionTest
internal val KEYWORDS: Map<String, TokenType> =
    mapOf(
        "let" to TokenType.LET,
        "println" to TokenType.PRINTLN,
        "number" to TokenType.NUMBERTYPE,
        "string" to TokenType.STRINGTYPE,
        "const" to TokenType.CONST,
        "boolean" to TokenType.BOOLEANTYPE,
        "true" to TokenType.BOOLEANLITERAL,
        "false" to TokenType.BOOLEANLITERAL,
        "if" to TokenType.IF,
        "else" to TokenType.ELSE,
        "readInput" to TokenType.READINPUT,
        "readEnv" to TokenType.READENV,
    )
