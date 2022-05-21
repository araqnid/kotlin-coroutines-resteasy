package org.araqnid.kotlin.coroutines.resteasy

import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.container.AsyncResponse
import jakarta.ws.rs.container.Suspended
import jakarta.ws.rs.core.UriInfo
import kotlinx.coroutines.*
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.entity.ContentType
import org.apache.http.util.EntityUtils
import org.araqnid.kotlin.assertthat.*
import org.junit.Test
import java.time.Duration
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Executor
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext
import org.jboss.resteasy.core.ResteasyContext as RealResteasyContext

class ResteasyAsyncTest {
    @Test
    fun `passes normal response to client`() {
        withServer(SimpleResource) {
            httpClient.execute(HttpGet("/")).use { response ->
                assertThat(response, Matcher(HttpResponse::isOk) and Matcher(HttpResponse::isPlainText))
                assertThat(response.bodyText, equalTo("hello world"))
            }
        }
    }

    @Test
    fun `converts Unit response to 204`() {
        withServer(SimpleResource) {
            httpClient.execute(HttpGet("/generate_204")).use { response ->
                assertThat(response.statusLine.statusCode, equalTo(204))
            }
        }
    }

    @Test
    fun `provides Resteasy context data during execution`() {
        withServer(SimpleResource) {
            httpClient.execute(HttpGet("/using_context_data")).use { response ->
                assertThat(response, Matcher(HttpResponse::isOk) and Matcher(HttpResponse::isPlainText))
                assertThat(response.bodyText, equalTo("baseUri=$uri"))
            }
        }
    }

    @Test
    fun `passes exception response to client`() {
        withServer(SimpleResource) {
            httpClient.execute(HttpGet("/generate_400")).use { response ->
                assertThat(response.statusLine.statusCode, equalTo(400))
            }
        }
    }

    @Test
    fun `uses given thread pool to respond`() {
        val threadPool = Executor { command ->
            Thread(command, "TestServerWorker").start()
        }
        withServer(ResourceWithThreadPool(threadPool)) {
            httpClient.execute(HttpGet("/")).use { response ->
                assertThat(response, Matcher(HttpResponse::isOk) and Matcher(HttpResponse::isPlainText))
                assertThat(response.bodyText, containsSubstring("TestServerWorker"))
            }
        }
    }

    @Test
    fun `coroutine cancelled after timeout`() {
        val resource = ResourceWithSlowMethod()
        withServer(resource) {
            httpClient.execute(HttpGet("/slow")).use { response ->
                assertThat(response.statusLine.statusCode, greaterThan(500) or equalTo(500))
            }
        }
        val job = resource.jobs.take()
        assertThat(job, Matcher(Job::isCancelled))
    }

    @Test
    fun `can specify additional coroutine context when responding`() {
        withServer(SimpleResource) {
            httpClient.execute(HttpGet("/specifying_coroutine_context")).use { response ->
                assertThat(response, Matcher(HttpResponse::isOk) and Matcher(HttpResponse::isPlainText))
                assertThat(response.bodyText, equalTo("CoroutineName=CoroutineName(test)"))
            }
        }
    }
}

private val HttpResponse.isOk: Boolean get() = statusLine.statusCode in 200..299
private val HttpResponse.bodyText: String get() = EntityUtils.toString(entity)
private val HttpResponse.mimeType: String
    get() {
        val contentType = ContentType.get(entity)
        return contentType.mimeType
    }
private val HttpResponse.isPlainText: Boolean get() = mimeType.equals("text/plain", ignoreCase = true)

@Path("/")
object SimpleResource {
    @GET
    @Produces("text/plain")
    fun respond(@Suspended asyncResponse: AsyncResponse) {
        GlobalScope.respondAsynchronously(asyncResponse) {
            delay(50)
            "hello world"
        }
    }

    @GET
    @Path("generate_204")
    fun responseWithNoContent(@Suspended asyncResponse: AsyncResponse) {
        GlobalScope.respondAsynchronously(asyncResponse) {
            delay(50)
        }
    }

    @GET
    @Path("generate_400")
    @Produces("text/plain")
    fun respondWithException(@Suspended asyncResponse: AsyncResponse) {
        GlobalScope.respondAsynchronously(asyncResponse) {
            throw BadRequestException()
        }
    }

    @GET
    @Path("using_context_data")
    @Produces("text/plain")
    fun respondUsingContextData(@Suspended asyncResponse: AsyncResponse) {
        GlobalScope.respondAsynchronously(asyncResponse) {
            "baseUri=${resteasyContextData<UriInfo>().baseUri}"
        }
    }

    @GET
    @Path("specifying_coroutine_context")
    @Produces("text/plain")
    fun respondUsingSpecifiedCoroutineContext(@Suspended asyncResponse: AsyncResponse) {
        GlobalScope.respondAsynchronously(asyncResponse, context = CoroutineName("test")) {
            "CoroutineName=${coroutineContext[CoroutineName]}"
        }
    }

    private inline fun <reified T : Any> resteasyContextData(): T {
        return RealResteasyContext.getContextData(T::class.java) as T
    }
}

@Path("/")
class ResourceWithThreadPool(private val threadPool: Executor) {
    private val coroutineScope = object : CoroutineScope {
        override val coroutineContext: CoroutineContext
            get() = threadPool.asCoroutineDispatcher()
    }

    @GET
    @Produces("text/plain")
    fun testResource(@Suspended asyncResponse: AsyncResponse) {
        coroutineScope.respondAsynchronously(asyncResponse) {
            "responding on ${Thread.currentThread().name}"
        }
    }
}

@Path("/")
class ResourceWithSlowMethod {
    val jobs: BlockingQueue<Job> = LinkedBlockingQueue()

    @GET
    @Path("slow")
    @Produces("text/plain")
    fun respondSlowly(@Suspended asyncResponse: AsyncResponse) {
        val baseline = Duration.ofMillis(500)
        asyncResponse.setTimeout(baseline)
        val job = GlobalScope.respondAsynchronously(asyncResponse) {
            delay(baseline * 3)
        }
        jobs += job
    }
}

private fun AsyncResponse.setTimeout(duration: Duration) = setTimeout(duration.toNanos(), TimeUnit.NANOSECONDS)
private suspend fun delay(duration: Duration) = delay(duration.toMillis())
private operator fun Duration.times(n: Int) = Duration.ofNanos(toNanos() * n)
