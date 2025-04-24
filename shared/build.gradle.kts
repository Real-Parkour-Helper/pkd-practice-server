plugins {
    kotlin("jvm") version "2.1.10"
}

group = "dev.spaghett"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    // https://mvnrepository.com/artifact/com.google.code.gson/gson
    implementation("com.google.code.gson:gson:2.13.0")
}

kotlin {
    jvmToolchain(8)
}
