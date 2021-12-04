plugins {
    kotlin("jvm") version "1.5.10"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/")
    maven("https://jitpack.io/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.github.FabricMc:mapping-io:5879155d42")

    implementation("org.zeroturnaround:zt-zip:1.14")

    implementation("com.github.doyaaaaaken:kotlin-csv-jvm:0.15.2")

    implementation("org.ow2.asm:asm:9.2")
    implementation("org.ow2.asm:asm-tree:9.2")
    implementation("org.ow2.asm:asm-util:9.2")
    implementation("org.ow2.asm:asm-commons:9.2")
    implementation("org.ow2.asm:asm-analysis:9.2")

    implementation("com.github.Steveice10:OpenNBT:df93b74170")
    implementation("com.github.Steveice10:MCProtocolLib:728e673fcf") {
        exclude(module = "opennbt")
    }
    implementation("com.github.ReplayMod:ReplayStudio:c9de2f5d4f") {
        exclude(module = "opennbt")
    }

    implementation("net.kyori:adventure-text-serializer-plain:4.0.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.+")

}

application {
    mainClass.set("McpToTinyKt")
}
