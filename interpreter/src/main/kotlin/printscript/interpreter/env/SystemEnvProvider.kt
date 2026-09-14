package printscript.interpreter.env

class SystemEnvProvider : EnvProvider {
    override fun getEnv(name: String): String? = System.getenv(name)
}
