plugins {
    kotlin("jvm") version "1.9.24"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "me.mathiasdevmc"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("dev.folia:folia-api:1.21.6-R0.1-SNAPSHOT")
    implementation("org.mongodb:mongodb-driver-sync:4.11.0")
    implementation(kotlin("stdlib"))
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "21"
}

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.main.get().output)
    from("src/main/resources")
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveFileName.set("dailyrewarder-1.0.jar")
    mergeServiceFiles()

}

tasks.build {
    dependsOn(tasks.shadowJar)
}
