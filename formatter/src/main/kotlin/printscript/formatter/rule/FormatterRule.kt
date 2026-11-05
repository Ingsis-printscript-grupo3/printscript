package printscript.formatter.rule

import printscript.formatter.Gap

// una regla dice cuando le toca (appliesTo) y que le hace al espacio entre dos tokens (apply).
// Corren en el orden del registry y cada una ve el gap como lo dejaron las anteriores,
// asi que si dos reglas tocan el mismo espacio gana la que va despues
interface FormatterRule {
    fun appliesTo(gap: Gap): Boolean

    fun apply(gap: Gap)
}
