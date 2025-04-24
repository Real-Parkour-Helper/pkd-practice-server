plugins {
    kotlin("jvm") version "2.1.10"
    application
}

application {
    mainClass.set("dev.spaghett.generator.MainKt")
}

group = "dev.spaghett"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.javalin:javalin:6.6.0")
    implementation("org.slf4j:slf4j-simple:2.0.16")

    // https://mvnrepository.com/artifact/com.google.code.gson/gson
    implementation("com.google.code.gson:gson:2.13.0")

    implementation(project(":shared"))
}

kotlin {
    jvmToolchain(23)
}