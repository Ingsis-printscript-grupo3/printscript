package printscript.semantic

import printscript.common.LanguageVersion
import kotlin.test.Test
import kotlin.test.assertEquals

class SemanticRulesTest {
    @Test
    fun `rules for version 1_0 restrict types to number and string`() {
        val rules = SemanticRules.from(LanguageVersion.V1_0)

        assertEquals(LanguageVersion.V1_0, rules.version)
        assertEquals(setOf("number", "string"), rules.supportedTypes)
    }

    @Test
    fun `rules for version 1_1 include boolean type`() {
        val rules = SemanticRules.from(LanguageVersion.V1_1)

        assertEquals(LanguageVersion.V1_1, rules.version)
        assertEquals(setOf("number", "string", "boolean"), rules.supportedTypes)
    }
}
