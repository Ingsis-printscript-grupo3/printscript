plugins {
    id("printscript.kotlin-common-conventions")
}

dependencies {
    api(project(":ast"))
    implementation(project(":common"))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.17.2")
}
