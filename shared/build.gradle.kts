plugins {
    kotlin("jvm") version "2.1.10"
}

group = "dev.spaghett"
version = "1.0"

repositories {
    mavenCentral()

    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    // https://mvnrepository.com/artifact/com.google.code.gson/gson
    implementation("com.google.code.gson:gson:2.13.0")

    compileOnly("org.github.paperspigot:paperspigot-api:1.8.8-R0.1-20160806.221350-1")
}

kotlin {
    jvmToolchain(8)
}
