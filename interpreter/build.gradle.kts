plugins {
    id("printscript.kotlin-common-conventions")
}

dependencies {
    api(project(":ast"))
    implementation(project(":common"))
}
