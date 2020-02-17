package org.araqnid.kotlin.coroutines.resteasy

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.entity.ContentType
import org.apache.http.util.EntityUtils
import org.jboss.resteasy.spi.ResteasyProviderFactory
import org.junit.Test
import java.time.Duration
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Executor
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import javax.ws.rs.BadRequestException
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.container.AsyncResponse
import javax.ws.rs.container.Suspended
import javax.ws.rs.core.UriInfo
import kotlin.coroutines.CoroutineContext

class ResteasyAsyncTest {
    @Test
    fun `passes normal response to client`() {
        withServer(SimpleResource) {
            httpClient.execute(HttpGet("/")).use { response ->
                assertThat(response, Matcher(HttpResponse::isOk) and Matcher(HttpResponse::contentTypeIs, "text/plain"))
                assertThat(response.bodyText(), equalTo("hello world"))
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
                assertThat(response, Matcher(HttpResponse::isOk) and Matcher(HttpResponse::contentTypeIs, "text/plain"))
                assertThat(response.bodyText(), equalTo("baseUri=$uri"))
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
                assertThat(response, Matcher(HttpResponse::isOk) and Matcher(HttpResponse::contentTypeIs, "text/plain"))
                assertThat(response.bodyText(), containsSubstring("TestServerWorker"))
            }
        }
    }

    @Test
    fun `coroutine cancelled after timeout`() {
        val resource = ResourceWithSlowMethod()
        withServer(resource) {
            httpClient.execute(HttpGet("/slow")).use { response ->
                assertThat(response.statusLine.statusCode, equalTo(503))
            }
        }
        val job = resource.jobs.take()
        assertThat(job, Matcher(Job::isCancelled))
    }

    @Test
    fun `can specify additional coroutine context when responding`() {
        withServer(SimpleResource) {
            httpClient.execute(HttpGet("/specifying_coroutine_context")).use { response ->
                assertThat(response, Matcher(HttpResponse::isOk) and Matcher(HttpResponse::contentTypeIs, "text/plain"))
                assertThat(response.bodyText(), equalTo("CoroutineName=CoroutineName(test)"))
            }
        }
    }
}

private fun HttpResponse.isOk(): Boolean = statusLine.statusCode in 200..299
private fun HttpResponse.bodyText(): String = EntityUtils.toString(entity)
private fun HttpResponse.contentTypeIs(mimeType: String): Boolean {
    val contentType = ContentType.get(entity)
    return contentType != null && contentType.mimeType.equals(mimeType, ignoreCase = true)
}

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
        return ResteasyProviderFactory.getContextData(T::class.java) as T
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
