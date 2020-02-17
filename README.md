Resteasy coroutine adapter
==========================

[ ![Build Status](https://travis-ci.org/araqnid/kotlin-coroutines-resteasy.svg?branch=master)](https://travis-ci.org/araqnid/kotlin-coroutines-resteasy) [ ![Download](https://api.bintray.com/packages/araqnid/maven/kotlin-coroutines-resteasy/images/download.svg) ](https://bintray.com/araqnid/maven/kotlin-coroutines-resteasy/_latestVersion) [ ![Kotlin](https://img.shields.io/badge/kotlin-1.3.61-blue.svg)](http://kotlinlang.org)

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

This library is available on jcenter, e.g.:

```groovy

dependencies {
   compile "org.araqnid:kotlin-coroutines-resteasy:1.0.0"
}
```
