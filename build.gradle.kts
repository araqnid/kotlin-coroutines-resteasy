import java.net.URI

plugins {
    kotlin("jvm") version "1.4.30"
    `maven-publish`
    signing
}

val buildNumber: String? = System.getenv("BUILD_NUMBER")

group = "org.araqnid.kotlin.resteasy"
version = "1.4.4"
description = "Resteasy coroutine adapter"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withSourcesJar()
    withJavadocJar()
}

tasks {
    withType<JavaCompile> {
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

dependencies {
    api("org.jboss.spec.javax.ws.rs:jboss-jaxrs-api_2.0_spec:1.0.1.Beta1")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:${LibraryVersions.kotlinCoroutines}")
    implementation("org.jboss.resteasy:resteasy-jaxrs:${LibraryVersions.resteasy}")
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    testImplementation(kotlin("test-junit"))
    testImplementation("org.eclipse.jetty:jetty-server:${LibraryVersions.jetty}")
    testImplementation("org.eclipse.jetty:jetty-servlet:${LibraryVersions.jetty}")
    testImplementation("org.apache.httpcomponents:httpclient:4.5.3")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${LibraryVersions.kotlinCoroutines}")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:${LibraryVersions.kotlinCoroutines}")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${LibraryVersions.kotlinCoroutines}")
    testImplementation("org.araqnid.kotlin.assert-that:assert-that:${LibraryVersions.assertThat}")
}

publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "kotlin-coroutines-resteasy"
            pom {
                name.set(project.name)
                description.set(project.description)
                licenses {
                    license {
                        name.set("Apache")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                url.set("https://github.com/araqnid/kotlin-coroutines-resteasy")
                issueManagement {
                    system.set("Github")
                    url.set("https://github.com/araqnid/kotlin-coroutines-resteasy/issues")
                }
                scm {
                    connection.set("https://github.com/araqnid/kotlin-coroutines-resteasy.git")
                    url.set("https://github.com/araqnid/kotlin-coroutines-resteasy")
                }
                developers {
                    developer {
                        name.set("Steven Haslam")
                        email.set("araqnid@gmail.com")
                    }
                }
            }
        }
    }

    repositories {
        val sonatypeUser: String? by project
        if (sonatypeUser != null) {
            maven {
                name = "OSSRH"
                url = URI("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
                val sonatypePassword: String by project
                credentials {
                    username = sonatypeUser
                    password = sonatypePassword
                }
            }
        }
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications)
}
