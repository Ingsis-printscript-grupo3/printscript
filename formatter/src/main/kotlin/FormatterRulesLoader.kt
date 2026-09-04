package printscript.formatter

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

object FormatterRulesLoader {
    private val jsonMapper = ObjectMapper().registerKotlinModule()
    private val yamlMapper = ObjectMapper(YAMLFactory()).registerKotlinModule()

    fun fromJson(json: String): FormatterRules = jsonMapper.readValue(json)

    fun fromYaml(yaml: String): FormatterRules = yamlMapper.readValue(yaml)
}
