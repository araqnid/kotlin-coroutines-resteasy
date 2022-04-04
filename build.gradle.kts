import java.net.URI

plugins {
    kotlin("jvm") version "1.6.20"
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
    api("org.jboss.spec.javax.ws.rs:jboss-jaxrs-api_2.0_spec:1.0.1.Final")
    api(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.5.1"))
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jboss.resteasy:resteasy-jaxrs:3.1.4.Final")
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    testImplementation(kotlin("test-junit"))
    testImplementation(platform("org.eclipse.jetty:jetty-bom:9.4.26.v20200117"))
    testImplementation("org.eclipse.jetty:jetty-server")
    testImplementation("org.eclipse.jetty:jetty-servlet")
    testImplementation("org.apache.httpcomponents:httpclient:4.5.13")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    testImplementation("org.araqnid.kotlin.assert-that:assert-that:0.1.1")
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
