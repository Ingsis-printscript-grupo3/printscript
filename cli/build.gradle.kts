plugins {
    id("printscript.kotlin-common-conventions")
}

dependencies {
    implementation(project(":lexer"))
    implementation(project(":parser"))
    implementation(project(":semantic"))
    implementation(project(":interpreter"))
    implementation(project(":ast"))
    implementation(project(":common"))
}
