plugins {
    id("printscript.kotlin-common-conventions")
}

dependencies {
    // aparecen en la API publica de Engine y PrintScriptRunner
    api(project(":common"))
    api(project(":ast"))
    api(project(":interpreter"))
    // se usan solo por dentro
    implementation(project(":lexer"))
    implementation(project(":parser"))
    implementation(project(":semantic"))
    implementation(project(":formatter"))
    implementation(project(":linter"))
}
