package printscript.linter.rule

import printscript.ast.Assignment
import printscript.ast.Identifier
import printscript.ast.Statement
import printscript.common.Position
import printscript.linter.Warning
import kotlin.test.Test
import kotlin.test.assertEquals

class CompositeRuleTest {
    private fun statement() = Assignment("miVariable", Identifier("x", Position(0, 0)), Position(4, 2))

    @Test
    fun `an empty group returns no warnings`() {
        assertEquals(emptyList(), CompositeRule(emptyList()).check(statement()))
    }

    @Test
    fun `it concatenates the warnings of every rule, in order`() {
        val group = CompositeRule(listOf(WarnsWith("uno"), WarnsWith("dos"), WarnsWith("tres")))

        assertEquals(listOf("uno", "dos", "tres"), group.check(statement()).map { it.message })
    }

    @Test
    fun `a rule that does not apply contributes nothing and the others still run`() {
        val group = CompositeRule(listOf(NoOpRule(), WarnsWith("uno"), NoOpRule(), WarnsWith("dos")))

        assertEquals(listOf("uno", "dos"), group.check(statement()).map { it.message })
    }

    // esto es lo que lo hace un Composite y no una lista con otro nombre:
    // un grupo es una regla mas, asi que puede ir adentro de otro grupo
    @Test
    fun `a group can contain another group and flattens all the same`() {
        val inner = CompositeRule(listOf(WarnsWith("adentro 1"), WarnsWith("adentro 2")))
        val outer = CompositeRule(listOf(WarnsWith("afuera"), inner))

        assertEquals(listOf("afuera", "adentro 1", "adentro 2"), outer.check(statement()).map { it.message })
    }

    @Test
    fun `every rule receives the same statement`() {
        val spies = listOf(SpyRule(), SpyRule())
        val statement = statement()

        CompositeRule(spies).check(statement)

        spies.forEach { assertEquals(listOf<Statement>(statement), it.seen.toList()) }
    }

    private class WarnsWith(private val message: String) : LinterRule {
        override fun check(statement: Statement): List<Warning> = listOf(Warning(message, statement.position))
    }

    private class NoOpRule : LinterRule {
        override fun check(statement: Statement): List<Warning> = emptyList()
    }

    private class SpyRule : LinterRule {
        val seen = mutableListOf<Statement>()

        override fun check(statement: Statement): List<Warning> {
            seen.add(statement)
            return emptyList()
        }
    }
}
