
plugins {
    id("printscript.kotlin-common-conventions")
}

dependencies {
    implementation(project(":lexer"))
    implementation(project(":common"))
    implementation(project(":ast"))
}