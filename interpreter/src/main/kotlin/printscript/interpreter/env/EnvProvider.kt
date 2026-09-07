package printscript.interpreter.env

interface EnvProvider {
    fun getEnv(name: String): String?
}
