package org.araqnid.kotlin.coroutines.resteasy

import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.container.AsyncResponse
import jakarta.ws.rs.container.Suspended
import jakarta.ws.rs.core.UriInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import org.araqnid.kotlin.assertthat.*
import org.junit.Test
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import org.jboss.resteasy.core.ResteasyContext as RealResteasyContext

class ResteasyAsyncTest {
    private val httpClient = HttpClient.newHttpClient()

    private suspend fun ServerScope.execGET(path: String): HttpResponse<String> {
        require(path.startsWith("/"))
        val request = HttpRequest.newBuilder(uri.resolve(path)).build()
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
    }

    @Test
    fun `passes normal response to client`() = runBlocking {
        withServer(SimpleResource(this)) {
            val response = execGET("/")
            assertThat(response, Matcher(HttpResponse<*>::isOk) and Matcher(HttpResponse<*>::isPlainText))
            assertThat(response.bodyText, equalTo("hello world"))
        }
    }

    @Test
    fun `converts Unit response to 204`() = runBlocking {
        withServer(SimpleResource(this)) {
            val response = execGET("/generate_204")
            assertThat(response, hasStatus(204))
        }
    }

    @Test
    fun `provides Resteasy context data during execution`() = runBlocking {
        withServer(SimpleResource(this)) {
            val response = execGET("/using_context_data")
            assertThat(response, Matcher(HttpResponse<*>::isOk) and Matcher(HttpResponse<*>::isPlainText))
            assertThat(response.bodyText, equalTo("baseUri=$uri"))
        }
    }

    @Test
    fun `passes exception response to client`() = runBlocking {
        withServer(SimpleResource(this)) {
            val response = execGET("/generate_400")
            assertThat(response, hasStatus(400))
        }
    }

    @Test
    fun `uses given thread pool to respond`() = runBlocking {
        val threadPool = Executor { command ->
            Thread(command, "TestServerWorker").start()
        }
        withServer(SimpleResource(CoroutineScope(threadPool.asCoroutineDispatcher()))) {
            val response = execGET("/indicate_thread")
            assertThat(response, Matcher(HttpResponse<*>::isOk) and Matcher(HttpResponse<*>::isPlainText))
            assertThat(response.bodyText, containsSubstring("TestServerWorker"))
        }
    }

    @Test
    fun `coroutine cancelled after timeout`() = runBlocking {
        val resource = ResourceWithSlowMethod(this)
        withServer(resource) {
            val response = execGET("/slow")
            assertThat(response, hasStatus(greaterThan(500) or equalTo(500)))
        }
        val job = resource.jobs.first()
        assertThat(job, Matcher(Job::isCancelled))
    }

    @Test
    fun `can specify additional coroutine context when responding`() = runBlocking {
        withServer(SimpleResource(this)) {
            val response = execGET("/specifying_coroutine_context")
            assertThat(response, Matcher(HttpResponse<*>::isOk) and Matcher(HttpResponse<*>::isPlainText))
            assertThat(response.bodyText, equalTo("CoroutineName=CoroutineName(test)"))
        }
    }
}

private fun hasStatus(statusCode: Int): Matcher<HttpResponse<*>> {
    return has("statusCode", { it.statusCode() }, equalTo(statusCode))
}

private fun hasStatus(statusCodeMatcher: Matcher<Int>): Matcher<HttpResponse<*>> {
    return has("statusCode", { it.statusCode() }, statusCodeMatcher)
}

private val HttpResponse<*>.isOk: Boolean get() = statusCode() in 200..299
private val HttpResponse<String>.bodyText: String get() = body()
private val HttpResponse<*>.mimeType: String?
    get() = headers().firstValue("Content-Type").map {  value ->
        val pos = value.indexOf(';')
        if (pos > 0)
            value.substring(0 until pos)
        else
            value
    }.orElse(null)
private val HttpResponse<*>.isPlainText: Boolean get() = mimeType.equals("text/plain", ignoreCase = true)

@Path("/")
class SimpleResource(private val scope: CoroutineScope) {
    @GET
    @Produces("text/plain")
    fun respond(@Suspended asyncResponse: AsyncResponse) {
        scope.respondAsynchronously(asyncResponse) {
            delay(50)
            "hello world"
        }
    }
    @GET
    @Path("indicate_thread")
    @Produces("text/plain")
    fun testResource(@Suspended asyncResponse: AsyncResponse) {
        scope.respondAsynchronously(asyncResponse) {
            "responding on ${Thread.currentThread().name}"
        }
    }

    @GET
    @Path("generate_204")
    fun responseWithNoContent(@Suspended asyncResponse: AsyncResponse) {
        scope.respondAsynchronously(asyncResponse) {
            delay(50)
        }
    }

    @GET
    @Path("generate_400")
    @Produces("text/plain")
    fun respondWithException(@Suspended asyncResponse: AsyncResponse) {
        scope.respondAsynchronously(asyncResponse) {
            throw BadRequestException()
        }
    }

    @GET
    @Path("using_context_data")
    @Produces("text/plain")
    fun respondUsingContextData(@Suspended asyncResponse: AsyncResponse) {
        scope.respondAsynchronously(asyncResponse) {
            "baseUri=${resteasyContextData<UriInfo>().baseUri}"
        }
    }

    @GET
    @Path("specifying_coroutine_context")
    @Produces("text/plain")
    fun respondUsingSpecifiedCoroutineContext(@Suspended asyncResponse: AsyncResponse) {
        scope.respondAsynchronously(asyncResponse, context = CoroutineName("test")) {
            "CoroutineName=${coroutineContext[CoroutineName]}"
        }
    }

    private inline fun <reified T : Any> resteasyContextData(): T {
        return RealResteasyContext.getContextData(T::class.java) as T
    }
}

@Path("/")
class ResourceWithSlowMethod(private val scope: CoroutineScope) {
    val jobs: MutableList<Job> = CopyOnWriteArrayList()

    @GET
    @Path("slow")
    @Produces("text/plain")
    fun respondSlowly(@Suspended asyncResponse: AsyncResponse) {
        val baseline = Duration.ofMillis(500)
        asyncResponse.setTimeout(baseline)
        val job = scope.respondAsynchronously(asyncResponse) {
            delay(baseline * 3)
        }
        jobs += job
    }
}

private fun AsyncResponse.setTimeout(duration: Duration) = setTimeout(duration.toNanos(), TimeUnit.NANOSECONDS)
private suspend fun delay(duration: Duration) = delay(duration.toMillis())
private operator fun Duration.times(n: Int) = Duration.ofNanos(toNanos() * n)
