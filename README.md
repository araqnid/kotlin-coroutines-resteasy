Resteasy coroutine adapter
==========================

[ ![Kotlin](https://img.shields.io/badge/kotlin-1.4.30-blue.svg)](http://kotlinlang.org)
[![build](https://github.com/araqnid/kotlin-coroutines-resteasy/actions/workflows/gradle-build.yml/badge.svg)](https://github.com/araqnid/kotlin-coroutines-resteasy/actions/workflows/gradle-build.yml)
[![Maven Central](https://img.shields.io/maven-central/v/org.araqnid.kotlin.resteasy/kotlin-coroutines-resteasy.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.araqnid.kotlin.resteasy%22%20AND%20a%3A%22kotlin-coroutines-resteasy%22)

This allows responding to requests from a Kotlin coroutine, using Resteasy's
(actually JAX-RS's) AsyncResponse support. What is Resteasy-specific is that
when the coroutine is executing, the context data from ResteasyProviderFactory
is kept available, so coroutines have access to context data about the request.

An example (from the tests):

```kotlin
    @GET
    @Produces("text/plain")
    fun respond(@Suspended asyncResponse: AsyncResponse) {
        coroutineScope.respondAsynchronously(asyncResponse) {
            delay(50)
            "hello world"
        }
    }
```
