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
    maven("https://jitpack.io")
}

dependencies {
    implementation("io.javalin:javalin:6.6.0")
    implementation("org.slf4j:slf4j-simple:2.0.16")

    // https://mvnrepository.com/artifact/com.google.code.gson/gson
    implementation("com.google.code.gson:gson:2.13.0")

    implementation("com.github.Querz:NBT:6.1")

    implementation(project(":shared"))
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "dev.spaghett.generator.MainKt"
        )
    }
}

tasks.register<Jar>("fatJar") {
    group = "build"
    description = "Assembles a fat jar including all dependencies."

    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }

    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

kotlin {
    jvmToolchain(23)
}