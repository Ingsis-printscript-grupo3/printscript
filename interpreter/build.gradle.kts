plugins {
    id("printscript.kotlin-common-conventions")
}

dependencies {
    implementation(project(":ast"))
    implementation(project(":common"))
    testImplementation(kotlin("reflect"))
}
