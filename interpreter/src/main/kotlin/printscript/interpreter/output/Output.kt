package printscript.interpreter.output

//donde van los println del programa PrintScript
interface Output {
    fun emit(line: String)
}
