import org.apache.tools.ant.filters.ReplaceTokens
import java.time.Instant
import java.time.format.DateTimeFormatter

fun property(key: String) = project.findProperty(key).toString()

plugins {
    id("java")
    id("net.minecraftforge.gradle")
}

repositories {
    maven("https://ldtteam.jfrog.io/ldtteam/modding")
    flatDir {
      dirs("libs")
    }
}

val modName = property("modName")
val modId = property("modId")
val modVersion = property("modVersion")

val mcVersion = property("mcVersion")
val forgeVersion = property("forgeVersion")
val mappingsVersion = property("mappingsVersion")

version = "$mcVersion-$modVersion"
group = property("group")

minecraft {
    mappings("snapshot", mappingsVersion)
    accessTransformer(file("src/main/resources/META-INF/accesstransformer.cfg"))

    runs {
        create("client") {
            workingDirectory = file("run").absolutePath

            property("forge.logging.markers", "SCAN,REGISTRIES,REGISTRYDUMP")
            property("forge.logging.console.level", "debug")

            if (project.hasProperty("mcUuid")) {
                args("--uuid", project.property("mcUuid"))
            }
            if (project.hasProperty("mcUsername")) {
                args("--username", project.property("mcUsername"))
            }
            if (project.hasProperty("mcAccessToken")) {
                args("--accessToken", project.property("mcAccessToken"))
            }
        }

        create("server") {
            workingDirectory = file("run").absolutePath

            property("forge.logging.markers", "SCAN,REGISTRIES,REGISTRYDUMP")
            property("forge.logging.console.level", "debug")
        }
    }
}

dependencies {
    minecraft("net.minecraftforge:forge:$mcVersion-$forgeVersion")

    // Deobfed OptiFine jar by https://github.com/octarine-noise/simpledeobf.
    compileOnly(group = "blank", "OptiFine_${mcVersion}_${property("optifineVersion")}", classifier = "deobf")
    implementation(group = "cofh", name = "ThermalDynamics", version = "$mcVersion-${property("thermalDynamicsVersion")}", classifier = "universal")
}

// Workaround for resources issue. Use gradle tasks rather than generated runs for now.
sourceSets {
    main {
        output.setResourcesDir(file("build/combined"))
        java.destinationDirectory.set(file("build/combined"))
    }
}

tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE

    inputs.property("version", project.version)
    inputs.property("mcversion", mcVersion)

    from(sourceSets.main.get().resources.srcDirs) {
        include("mcmod.info")

        expand("version" to project.version, "mcversion" to mcVersion)
    }

    from(sourceSets.main.get().resources.srcDirs) {
        exclude("mcmod.info")
    }
}

// Assign version constant in ModConstants.
val prepareSources = tasks.register("prepareSources", Copy::class) {
    from("src/main/java")
    into("build/src/main/java")
    filter<ReplaceTokens>("tokens" to mapOf("VERSION" to version.toString()))
}

tasks.compileJava {
    source = prepareSources.get().outputs.files.asFileTree
}

tasks.jar {
    manifest.attributes(
        "Specification-Title" to project.name,
        "Specification-Vendor" to "ferreusveritas",
        "Specification-Version" to "1",
        "Implementation-Title" to project.name,
        "Implementation-Version" to project.version,
        "Implementation-Vendor" to "ferreusveritas",
        "Implementation-Timestamp" to DateTimeFormatter.ISO_INSTANT.format(Instant.now())
    )

    archiveBaseName.set(modName)
    finalizedBy("reobfJar")
}

java {
    withSourcesJar()

    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

val deobfJar = tasks.register("deobfJar", Jar::class) {
    archiveClassifier.set("deobf")
    from(sourceSets.main.get().output)
}

tasks.build {
    dependsOn(deobfJar)
}
