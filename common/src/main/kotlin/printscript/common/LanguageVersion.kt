package printscript.common

// cada version declara aca lo que la distingue, asi los modulos reciben las piezas ya elegidas
// y no preguntan de que version son. Por ahora solo las keywords que lee el lexer
enum class LanguageVersion(val label: String, val keywords: Map<String, TokenType>) {
    V1_0("1.0", KEYWORDS_1_0),
    V1_1("1.1", KEYWORDS_1_1),
    ;

    companion object {
        val DEFAULT = V1_0

        fun parse(value: String): LanguageVersion =
            entries.firstOrNull { it.label == value }
                ?: throw IllegalArgumentException("Unsupported PrintScript version: $value")
    }
}

private val KEYWORDS_1_0: Map<String, TokenType> =
    mapOf(
        "let" to TokenType.LET,
        "println" to TokenType.PRINTLN,
        "number" to TokenType.NUMBERTYPE,
        "string" to TokenType.STRINGTYPE,
    )

// 1.1 se monta sobre 1.0: en 1.0 estas palabras son identificadores comunes
private val KEYWORDS_1_1: Map<String, TokenType> =
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
