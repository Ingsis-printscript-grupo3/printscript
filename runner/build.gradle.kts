plugins {
    id("printscript.kotlin-common-conventions")
}

dependencies {
    api(project(":lexer"))
    api(project(":parser"))
    api(project(":semantic"))
    api(project(":interpreter"))
    api(project(":ast"))
    api(project(":common"))
    api(project(":formatter"))
    api(project(":linter"))
}
