package printscript.cli

import kotlin.test.Test
import kotlin.test.assertEquals

class EndToEndTest {

    private fun run(code: String): List<String> {
        val output = mutableListOf<String>()
        runPrintScript(code) { line -> output.add(line) } //aca no printeo en consola, agrego a lista asi puedo comparar
        return output
    }

    @Test
    fun `declara una variable y la imprime`() {
        val output = run(
            """
            let saludo: string = "hola";
            println(saludo);
            """.trimIndent()
        )

        assertEquals(listOf("hola"), output)
    }

    @Test
    fun `usa una variable declarada dentro de una operacion`() {
        val output = run(
            """
            let x: number = 5;
            let y: number = x * 3;
            println(y);
            """.trimIndent()
        )

        assertEquals(listOf("15"), output)
    }

    @Test
    fun `reasigna una variable y el print refleja el valor nuevo`() {
        val output = run(
            """
            let contador: number = 1;
            println(contador);
            contador = contador + 9;
            println(contador);
            """.trimIndent()
        )

        assertEquals(listOf("1", "10"), output)
    }
}
