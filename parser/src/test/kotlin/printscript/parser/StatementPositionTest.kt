package printscript.parser

import printscript.ast.VariableDeclaration
import printscript.common.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class StatementPositionTest {
    @Test
    fun `a declaration takes the position of the let keyword`() {
        val statements = parseStatements("\n\nlet x: number;")

        assertEquals(Position(3, 1), statements[0].position)
    }

    // position apunta al let y namePosition al nombre: son distintas a proposito,
    // y el linter usa la segunda para senalar el identificador
    @Test
    fun `a declaration carries the position of its name apart from the one of the let`() {
        val statements = parseStatements("\n\nlet miVariable: number;")

        val declaration = assertIs<VariableDeclaration>(statements[0])
        assertEquals(Position(3, 1), declaration.position)
        assertEquals(Position(3, 5), declaration.namePosition)
    }

    @Test
    fun `a const declaration also carries the position of its name`() {
        val statements = parseStatements("const miConstante: number = 3;")

        val declaration = assertIs<VariableDeclaration>(statements[0])
        assertEquals(Position(1, 1), declaration.position)
        assertEquals(Position(1, 7), declaration.namePosition)
    }

    @Test
    fun `an assignment takes the position of the variable name`() {
        val statements = parseStatements("\n".repeat(6) + " x = 5;")

        assertEquals(Position(7, 2), statements[0].position)
    }

    @Test
    fun `a println takes the position of the println keyword`() {
        val statements = parseStatements("\n".repeat(8) + "  println(x);")

        assertEquals(Position(9, 3), statements[0].position)
    }

    @Test
    fun `every statement keeps its own position`() {
        val statements = parseStatements("let x: number;\nprintln(x);")

        assertEquals(listOf(Position(1, 1), Position(2, 1)), statements.map { it.position })
    }
}
