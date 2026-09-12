plugins {
    id("printscript.kotlin-common-conventions")
}

dependencies {
    api(project(":ast"))
    api(project(":common"))
    // para los casos estilo TCK, que van de fuente PrintScript a warnings
    testImplementation(project(":lexer"))
    testImplementation(project(":parser"))
}
