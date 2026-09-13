package printscript.formatter.rule

import printscript.formatter.Gap

// una regla mira el espacio entre dos tokens y lo puede cambiar
internal fun interface FormatterRule {
    fun apply(gap: Gap)
}
