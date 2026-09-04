import org.gradle.kotlin.dsl.kotlin

plugins {
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint")
}

repositories{
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.withType<Test>().configureEach{
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}