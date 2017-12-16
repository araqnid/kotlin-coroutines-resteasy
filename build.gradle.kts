import org.gradle.kotlin.dsl.get
import org.jetbrains.kotlin.daemon.common.toHexString
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

plugins {
    kotlin("jvm") version "1.2.10"
}

val resteasyVersion by extra("3.1.4.Final")

val gitVersion by extra {
    val capture = ByteArrayOutputStream()
    project.exec {
        commandLine("git", "describe", "--tags", "--always")
        standardOutput = capture
    }
    String(capture.toByteArray())
            .trim()
            .removePrefix("v")
            .replace('-', '.')
}

group = "org.araqnid"
version = gitVersion

repositories {
    jcenter()
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "1.7"
        targetCompatibility = "1.7"
        options.encoding = "UTF-8"
        options.isIncremental = true
        options.isDeprecation = true
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.7"
        }
    }
}

kotlin {
    experimental.coroutines = Coroutines.ENABLE
}

dependencies {
    compile("org.jboss.resteasy:resteasy-jaxrs:$resteasyVersion")
    compile("org.jetbrains.kotlinx:kotlinx-coroutines-core:0.20")
    compile(kotlin("stdlib-jdk8"))
    compile(kotlin("reflect"))
    testCompile(kotlin("test-junit"))
    testCompile("org.araqnid:hamkrest-json:1.0.3")
}
