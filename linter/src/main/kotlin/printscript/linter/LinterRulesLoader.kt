package printscript.linter

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.ValueInstantiationException
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File

object LinterRulesLoader {
    private val jsonMapper = ObjectMapper().registerKotlinModule()
    private val yamlMapper = ObjectMapper(YAMLFactory()).registerKotlinModule()

    fun fromJson(json: String): LinterRules =
        try {
            jsonMapper.readValue(json)
        } catch (e: ValueInstantiationException) {
            throw e.cause ?: e
        }

    fun fromYaml(yaml: String): LinterRules =
        try {
            yamlMapper.readValue(yaml)
        } catch (e: ValueInstantiationException) {
            throw e.cause ?: e
        }

    fun fromFile(path: String): LinterRules {
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
