package printscript.linter

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler
import com.fasterxml.jackson.databind.exc.ValueInstantiationException
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File
import java.io.InputStream

object LinterRulesLoader {
    private val jsonMapper = mapperOf(null)
    private val yamlMapper = mapperOf(YAMLFactory())

    fun fromJson(json: String): LinterRules = unwrap { jsonMapper.readValue(json) }

    fun fromYaml(yaml: String): LinterRules = unwrap { yamlMapper.readValue(yaml) }

    // la config puede llegar como stream, sin extension que mirar
    fun fromStream(input: InputStream): LinterRules {
        val text = input.readBytes().decodeToString()
        return if (text.trimStart().startsWith("{")) fromJson(text) else fromYaml(text)
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

    private fun mapperOf(factory: YAMLFactory?): ObjectMapper {
        val mapper = if (factory == null) ObjectMapper() else ObjectMapper(factory)
        mapper.registerKotlinModule()
        mapper.addHandler(
            object : DeserializationProblemHandler() {
                override fun handleUnknownProperty(
                    ctxt: DeserializationContext,
                    p: JsonParser,
                    deserializer: JsonDeserializer<*>,
                    beanOrClass: Any,
                    propertyName: String,
                ): Boolean {
                    System.err.println("linter: ignoro la clave desconocida '$propertyName'")
                    p.skipChildren()
                    return true
                }
            },
        )
        return mapper
    }

    // Jackson envuelve el error del init, lo desenvolvemos
    private fun unwrap(load: () -> LinterRules): LinterRules =
        try {
            load()
        } catch (e: ValueInstantiationException) {
            throw e.cause ?: e
        }
}
