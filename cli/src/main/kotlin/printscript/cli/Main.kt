package printscript.cli

import printscript.interpreter.output.ConsoleOutput

private val CODIGO =
    """
    let x: number = 5;
    let y: number = x * 3;
    println(y);
    let saludo: string = "hola";
    println(saludo);
    """.trimIndent()

fun main() {
    println("code:")
    println(CODIGO)
    println("output:")

    val engine = Engine(output = ConsoleOutput())

    when (val result = engine.execute(CODIGO)) {
        is ExecutionResult.Success -> {
            // Execution finished successfully, outputs are handled by the callback
        }
        is ExecutionResult.Failure -> {
            println("Error ${result.type}: ${result.message}")
        }
    }
}
