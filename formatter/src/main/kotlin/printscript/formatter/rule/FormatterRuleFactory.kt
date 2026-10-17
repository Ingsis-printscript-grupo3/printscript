package printscript.formatter.rule

// la receta de una regla: que key de la config la prende, como se lee su valor, cuando es valido
// y como se crea la regla con el. Cada tipo de valor sabe hacerlo solo, asi nadie pregunta de que tipo es
interface FormatterRuleFactory {
    val key: String

    // lo usa el loader: pasa el texto del archivo al valor. null = la regla no se pidio
    fun parse(text: String): Any?

    // falla si el valor no es del tipo o del rango esperado, venga del archivo o armado en codigo
    fun validate(value: Any)

    // null = el valor deja la regla apagada
    fun create(value: Any): FormatterRule?
}

class BoolRuleFactory(
    override val key: String,
    private val rule: () -> FormatterRule,
) : FormatterRuleFactory {
    // un false, un null o un valor vacio es como no mencionarla
    override fun parse(text: String): Boolean? =
        when (text.lowercase()) {
            "", "null", "false" -> null
            "true" -> true
            else -> throw IllegalArgumentException("Expected boolean for '$key', got '$text'")
        }

    override fun validate(value: Any) =
        require(value is Boolean) { "Expected boolean for '$key', got ${value::class.simpleName} ($value)" }

    override fun create(value: Any): FormatterRule? = if (value == true) rule() else null
}

class IntRuleFactory(
    override val key: String,
    private val range: IntRange,
    private val rule: (Int) -> FormatterRule,
) : FormatterRuleFactory {
    override fun parse(text: String): Int? {
        if (text.isEmpty() || text == "null") return null
        return text.toIntOrNull() ?: throw IllegalArgumentException("Expected integer for '$key', got '$text'")
    }

    override fun validate(value: Any) {
        require(value is Int) { "Expected integer for '$key', got ${value::class.simpleName} ($value)" }
        require(value in range) { "$key must be ${range.first}..${range.last}, was $value" }
    }

    override fun create(value: Any): FormatterRule = rule(value as Int)
}
