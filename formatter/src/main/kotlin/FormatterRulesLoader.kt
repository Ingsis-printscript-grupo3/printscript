package printscript.formatter
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File

object FormatterRulesLoader {
    private val jsonMapper = ObjectMapper().registerKotlinModule()
    private val yamlMapper = ObjectMapper(YAMLFactory()).registerKotlinModule()

    // Traduce el contenido de una config en JSON al objeto de reglas
    fun fromJson(json: String): FormatterRules = jsonMapper.readValue(json)

    // Traduce el contenido de una config en YAML al objeto de reglas
    fun fromYaml(yaml: String): FormatterRules = yamlMapper.readValue(yaml)

    // Abre el archivo de config y elige el formato por la extension
    fun fromFile(path: String): FormatterRules = fromFile(File(path))

    fun fromFile(file: File): FormatterRules {
        require(file.isFile) { "Config file not found: ${file.path}" }
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
