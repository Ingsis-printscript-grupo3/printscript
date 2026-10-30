package printscript.parser.statement.handlers

import printscript.ast.BooleanLiteral
import printscript.ast.VariableDeclaration
import printscript.common.LanguageVersion
import printscript.common.Position
import printscript.parser.parseResults
import printscript.parser.parseStatements
import printscript.parser.result.ParseResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConstDeclarationTest {
    @Test
    fun `a const with an initializer sets isConst to true`() {
        val statements = parseStatements("\nconst x: number = 5;")

        val declaration = statements[0] as VariableDeclaration
        assertTrue(declaration.isConst)
        assertEquals(Position(2, 1), declaration.position)
    }

    @Test
    fun `a const without an initializer fails`() {
        val results = parseResults("const x: number;")

        val failure = results[0] as ParseResult.Failure
        assert(failure.message.contains("'const' requires an initializer"))
    }

    @Test
    fun `let keeps its optional initializer`() {
        val statements = parseStatements("let x: number;")

        val declaration = statements[0] as VariableDeclaration
        assertFalse(declaration.isConst)
        assertNull(declaration.value)
    }

    @Test
    fun `boolean is a valid type for both let and const`() {
        val statements = parseStatements("let a: boolean = true; const b: boolean = false;")

        val first = statements[0] as VariableDeclaration
        assertEquals("boolean", first.type)
        assertEquals(true, (first.value as BooleanLiteral).value)

        val second = statements[1] as VariableDeclaration
        assertEquals(true, second.isConst)
        assertEquals(false, (second.value as BooleanLiteral).value)
    }

    // el lexer 1.0 no emite CONST: se usan tokens de 1.1 para verificar que el parser 1.0 lo rechaza
    @Test
    fun `const is not a valid statement under version 1_0`() {
        val results = parseResults("const x: number = 5;", LanguageVersion.V1_0, lexerVersion = LanguageVersion.V1_1)

        val failure = results[0] as ParseResult.Failure
        assert(failure.message.contains("Unexpected token 'const'"))
    }

    @Test
    fun `the boolean type is not accepted under version 1_0`() {
        val results = parseResults("let x: boolean;", LanguageVersion.V1_0)

        val failure = results[0] as ParseResult.Failure
        assert(failure.message.contains("Expected 'number' or 'string'"))
    }
}
