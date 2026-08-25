data class FormatterRules(
    val spaceBeforeColon: Boolean = false,
    val spaceAfterColon: Boolean = true,
    val spaceAroundAssignment: Boolean = true,
    val lineBreaksBeforePrintln: Int = 1,
) {
    init {
        require(lineBreaksBeforePrintln in 0..2) {
            "lineBreaksBeforePrintln must be 0, 1 or 2, was $lineBreaksBeforePrintln"
        }
    }
}