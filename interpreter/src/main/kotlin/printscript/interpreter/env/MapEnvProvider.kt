package printscript.interpreter.env

class MapEnvProvider(private val env: Map<String, String> = emptyMap()) : EnvProvider {
    constructor(vararg pairs: Pair<String, String>) : this(pairs.toMap())

    override fun getEnv(name: String): String? = env[name]
}
