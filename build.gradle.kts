plugins {
    kotlin("jvm") version "1.3.61"
    `maven-publish`
    `java-library`
    id("com.jfrog.bintray") version "1.8.4"
}

val buildNumber: String? = System.getenv("BUILD_NUMBER")
val versionPrefix = "1.4"

group = "org.araqnid"

if (buildNumber != null)
    version = "${versionPrefix}.${buildNumber}"

repositories {
    jcenter()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
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
    testImplementation("org.araqnid:hamkrest-json:1.0.3")
    testImplementation("org.eclipse.jetty:jetty-server:${LibraryVersions.jetty}")
    testImplementation("org.eclipse.jetty:jetty-servlet:${LibraryVersions.jetty}")
    testImplementation("org.apache.httpcomponents:httpclient:4.5.3")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${LibraryVersions.kotlinCoroutines}")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:${LibraryVersions.kotlinCoroutines}")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${LibraryVersions.kotlinCoroutines}")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
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
    if (project.version != Project.DEFAULT_VERSION) {
        pkg.version.name = project.version.toString()
        pkg.version.vcsTag = "v" + project.version
    }
}
