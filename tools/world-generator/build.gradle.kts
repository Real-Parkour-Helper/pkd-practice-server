plugins {
    kotlin("jvm") version "2.1.10"
    application
}

application {
    mainClass.set("dev.spaghett.generator.MainKt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.javalin:javalin:6.6.0")
    implementation("org.slf4j:slf4j-simple:2.0.16")
}

kotlin {
    jvmToolchain(23)
}