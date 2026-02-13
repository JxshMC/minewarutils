plugins {
    `java-library`
    id("io.github.goooler.shadow") version "8.1.8"
    // id("io.papermc.paper.plugin-yml") version "1.5.0"
}

group = "com.jxsh.misc"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.dmulloy2.net/repository/public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.5")
    compileOnly("net.luckperms:api:5.4")
    compileOnly("net.dmulloy2:ProtocolLib:5.1.0")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.3.0")
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Core:2.9.2")
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit:2.9.2")
    implementation("dev.dejvokep:boosted-yaml:1.3.7")
    implementation("com.zaxxer:HikariCP:5.0.1")
}

tasks.shadowJar {
    // Don't relocate - just bundle the library as-is to avoid ASM errors
    // relocate("dev.dejvokep.boostedyaml", "com.jxsh.misc.libs.boostedyaml")
}

tasks.jar {
    enabled = false
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

