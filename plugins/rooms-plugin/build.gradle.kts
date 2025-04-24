plugins {
    kotlin("jvm") version "2.1.10"
    id("com.github.johnrengelman.shadow") version "8.1.1"
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
    testImplementation(kotlin("test"))

    compileOnly("org.github.paperspigot:paperspigot-api:1.8.8-R0.1-20160806.221350-1")
    implementation(kotlin("stdlib"))

    implementation(project(":shared"))
}

tasks.test {
    useJUnitPlatform()
}

tasks {
    shadowJar {
        archiveClassifier.set("") // So it's not named "-all.jar"
        minimize() // Optional: strips unused classes, makes the JAR smaller
    }

    build {
        dependsOn(shadowJar)
    }
}

kotlin {
    jvmToolchain(8)
}