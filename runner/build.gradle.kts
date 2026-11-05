plugins {
    id("printscript.kotlin-common-conventions")
}

dependencies {
    // aparecen en la API publica de Engine
    api(project(":common"))
    api(project(":ast"))
    api(project(":interpreter"))
    // se usan solo por dentro
    implementation(project(":lexer"))
    implementation(project(":parser"))
    implementation(project(":semantic"))
    // el Engine no formatea ni lintea: recibe una funcion que lo hace. Los tests la arman con estos
    testImplementation(project(":formatter"))
    testImplementation(project(":linter"))
}
