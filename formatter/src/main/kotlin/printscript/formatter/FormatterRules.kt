package printscript.formatter

import com.fasterxml.jackson.annotation.JsonProperty

const val MAX_LINE_BREAKS_AFTER_PRINTLN = 2

// si una regla no viene en la config, ese espacio queda como estaba en el archivo
data class FormatterRules(
    @JsonProperty("enforce-spacing-before-colon-in-declaration")
    val spaceBeforeColon: Boolean = false,
    @JsonProperty("enforce-spacing-after-colon-in-declaration")
    val spaceAfterColon: Boolean = false,
    @JsonProperty("enforce-spacing-around-equals")
    val spacingAroundEquals: Boolean = false,
    @JsonProperty("enforce-no-spacing-around-equals")
    val noSpacingAroundEquals: Boolean = false,
    @JsonProperty("line-breaks-after-println")
    val lineBreaksAfterPrintln: Int? = null,
    @JsonProperty("mandatory-single-space-separation")
    val singleSpaceSeparation: Boolean = false,
    @JsonProperty("mandatory-space-surrounding-operations")
    val spaceSurroundingOperations: Boolean = false,
    @JsonProperty("mandatory-line-break-after-statement")
    val lineBreakAfterStatement: Boolean = false,
    @JsonProperty("indent-inside-if")
    val indentInsideIf: Int? = null,
    @JsonProperty("if-brace-same-line")
    val ifBraceSameLine: Boolean = false,
    @JsonProperty("if-brace-below-line")
    val ifBraceBelowLine: Boolean = false,
) {
    init {
        require(lineBreaksAfterPrintln == null || lineBreaksAfterPrintln in 0..MAX_LINE_BREAKS_AFTER_PRINTLN) {
            "line-breaks-after-println must be 0..$MAX_LINE_BREAKS_AFTER_PRINTLN, was $lineBreaksAfterPrintln"
        }
        require(indentInsideIf == null || indentInsideIf >= 0) {
            "indent-inside-if must be 0 or more, was $indentInsideIf"
        }
        require(!(ifBraceSameLine && ifBraceBelowLine)) {
            "if-brace-same-line and if-brace-below-line cannot both be true"
        }
    }
}
