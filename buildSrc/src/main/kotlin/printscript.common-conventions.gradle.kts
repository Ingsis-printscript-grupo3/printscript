plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    "testImplementation"(kotlin("test"))
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
