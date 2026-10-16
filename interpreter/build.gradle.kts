plugins {
    id("printscript.kotlin-common-conventions")
}

dependencies {
    api(project(":ast"))
    api(project(":common"))
    // los tests parsean codigo PrintScript en lugar de construir el AST a mano
    testImplementation(project(":lexer"))
    testImplementation(project(":parser"))
}
