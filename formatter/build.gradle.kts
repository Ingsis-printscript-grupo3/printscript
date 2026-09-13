plugins {
    id("printscript.kotlin-common-conventions")
}

dependencies {
    api(project(":common"))
    testImplementation(project(":lexer"))
}
