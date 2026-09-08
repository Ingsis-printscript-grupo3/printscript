package printscript.semantic

import printscript.common.LanguageVersion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SemanticRulesTest {
    @Test
    fun `rules for version 1_0 disable const and booleans and restrict types`() {
        val rules = SemanticRules.from(LanguageVersion.V1_0)

        assertEquals(LanguageVersion.V1_0, rules.version)
        assertFalse(rules.allowsConst)
        assertFalse(rules.allowsBooleans)
        assertFalse(rules.allowsConditionals)
        assertEquals(setOf("number", "string"), rules.supportedTypes)
    }

    @Test
    fun `rules for version 1_1 enable const and booleans and include boolean type`() {
        val rules = SemanticRules.from(LanguageVersion.V1_1)

        assertEquals(LanguageVersion.V1_1, rules.version)
        assertTrue(rules.allowsConst)
        assertTrue(rules.allowsBooleans)
        assertTrue(rules.allowsConditionals)
        assertEquals(setOf("number", "string", "boolean"), rules.supportedTypes)
    }
}
