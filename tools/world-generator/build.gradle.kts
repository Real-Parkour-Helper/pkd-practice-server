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
    implementation("com.google.code.gson:gson:2.13.0")
    implementation("com.github.Querz:NBT:6.1")
    implementation(project(":shared"))
}

kotlin {
    jvmToolchain(23)
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }

    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith("jar") }
            .map { zipTree(it) }
    })

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.register<Copy>("copyJar") {
    dependsOn("build") // make sure the jar exists

    from(tasks.named<Jar>("jar"))
    into(rootDir.resolve("tools"))
    rename { "world-generator.jar" }
}
