package printscript.formatter
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File

object FormatterRulesLoader {
    private val jsonMapper = ObjectMapper().registerKotlinModule()
    private val yamlMapper = ObjectMapper(YAMLFactory()).registerKotlinModule()

    fun fromJson(json: String): FormatterRules = jsonMapper.readValue(json)

    fun fromYaml(yaml: String): FormatterRules = yamlMapper.readValue(yaml)

    fun fromFile(path: String): FormatterRules {
        val file = File(path)
        require(file.isFile) { "Config file not found: $path" }
        val text = file.readText()
        return when (val extension = file.extension.lowercase()) {
            "json" -> fromJson(text)
            "yaml", "yml" -> fromYaml(text)
            else -> throw IllegalArgumentException(
                "Unsupported config file extension '$extension': expected json, yaml or yml",
            )
        }
    }
}
