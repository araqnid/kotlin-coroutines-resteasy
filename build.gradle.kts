plugins {
    kotlin("jvm") version "1.4.30"
    `maven-publish`
    `java-library`
}

val buildNumber: String? = System.getenv("BUILD_NUMBER")
val versionPrefix = "1.4"

group = "org.araqnid"

if (buildNumber != null)
    version = "${versionPrefix}.${buildNumber}"

repositories {
    mavenCentral()

    if (isGithubUserAvailable(project)) {
        for (repo in listOf("assert-that")) {
            maven(url = "https://maven.pkg.github.com/araqnid/$repo") {
                name = "github-$repo"
                credentials(githubUserCredentials(project))
            }
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withSourcesJar()
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
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }

    repositories {
        if (isGithubUserAvailable(project)) {
            maven(url = "https://maven.pkg.github.com/araqnid/kotlin-coroutines-resteasy") {
                name = "github"
                credentials(githubUserCredentials(project))
            }
        }
    }
}
