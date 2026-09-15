import org.gradle.api.attributes.java.TargetJvmVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

repositories {
    maven("https://maven.blamejared.com/")
    maven("https://maven.nucleoid.xyz/")
}

dependencies {
    compileOnly(project(":project:common"))
    compileOnly(project(":project:common-impl"))
    compileOnly(project(":project:module-editor"))
    compileOnly("io.github.toxicity188:bettermodel-bukkit-api:3.4.1") {
        exclude(group = "org.jetbrains.kotlin")
    }
    compileOnly("org.joml:joml:1.10.5")
}

taboolib { subproject = true }

configurations.compileClasspath {
    attributes {
        attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 25)
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

@Suppress("DEPRECATION")
configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
