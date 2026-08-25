package printscript.linter

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

object LinterRulesLoader {
    private val jsonMapper = ObjectMapper().registerKotlinModule()
    private val yamlMapper = ObjectMapper(YAMLFactory()).registerKotlinModule()

    fun fromJson(json: String): LinterRules = jsonMapper.readValue(json)

    fun fromYaml(yaml: String): LinterRules = yamlMapper.readValue(yaml)
}
