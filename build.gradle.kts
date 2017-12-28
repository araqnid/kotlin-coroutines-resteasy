import org.gradle.kotlin.dsl.get
import org.jetbrains.kotlin.daemon.common.toHexString
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

plugins {
    kotlin("jvm") version "1.2.10"
    `maven-publish`
    `java-library`
    id("com.jfrog.bintray") version "1.7.3"
}

val resteasyVersion by extra("3.1.4.Final")
val jettyVersion by extra("9.4.8.v20171121")

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
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
        options.encoding = "UTF-8"
        options.isIncremental = true
        options.isDeprecation = true
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }
}

kotlin {
    experimental.coroutines = Coroutines.ENABLE
}

dependencies {
    api("org.jboss.spec.javax.ws.rs:jboss-jaxrs-api_2.0_spec:1.0.1.Beta1")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:0.20")
    implementation("org.jboss.resteasy:resteasy-jaxrs:$resteasyVersion")
    implementation(kotlin("stdlib-jdk8", "1.2.10"))
    implementation(kotlin("reflect", "1.2.10"))
    testCompile(kotlin("test-junit", "1.2.10"))
    testCompile("org.araqnid:hamkrest-json:1.0.3")
    testCompile("org.eclipse.jetty:jetty-server:$jettyVersion")
    testCompile("org.eclipse.jetty:jetty-servlet:$jettyVersion")
    testCompile("org.apache.httpcomponents:httpclient:4.5.3")
}


val sourcesJar by tasks.creating(Jar::class) {
    classifier = "sources"
    from(java.sourceSets["main"].allSource)
}

publishing {
    (publications) {
        "mavenJava"(MavenPublication::class) {
            from(components["java"])
            artifact(sourcesJar)
        }
    }
}

bintray {
    user = (project.properties["bintray.user"] ?: "").toString()
    key = (project.properties["bintray.apiKey"] ?: "").toString()
    publish = true
    setPublications("mavenJava")
    pkg.repo = "maven"
    pkg.name = "kotlin-coroutines-resteasy"
    pkg.setLicenses("Apache-2.0")
    pkg.vcsUrl = "https://github.com/araqnid/kotlin-coroutines-resteasy"
    pkg.desc = "Adapt Resteasy asynchronous requests to Kotlin coroutines"
    pkg.version.name = gitVersion
    if (!gitVersion.contains(".g")) {
        pkg.version.vcsTag = "v" + gitVersion
    }
}
