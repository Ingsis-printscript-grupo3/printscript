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
    implementation(project(":formatter"))
    implementation(project(":linter"))
    implementation("com.github.ajalt.clikt:clikt:4.4.0")
}
