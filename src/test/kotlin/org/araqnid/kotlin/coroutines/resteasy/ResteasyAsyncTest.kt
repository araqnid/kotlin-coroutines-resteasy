package org.araqnid.kotlin.coroutines.resteasy

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import kotlinx.coroutines.experimental.delay
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.entity.ContentType
import org.apache.http.util.EntityUtils
import org.jboss.resteasy.spi.ResteasyProviderFactory
import org.junit.Test
import java.util.concurrent.Executor
import javax.ws.rs.BadRequestException
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.container.AsyncResponse
import javax.ws.rs.container.Suspended
import javax.ws.rs.core.UriInfo

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
}

fun HttpResponse.isOk(): Boolean = statusLine.statusCode in 200..299
fun HttpResponse.bodyText(): String = EntityUtils.toString(entity)
fun HttpResponse.contentTypeIs(mimeType: String): Boolean {
    val contentType = ContentType.get(entity)
    return contentType != null && contentType.mimeType.equals(mimeType, ignoreCase = true)
}

@Path("/")
object SimpleResource {
    @GET
    @Produces("text/plain")
    fun respond(@Suspended asyncResponse: AsyncResponse) {
        respondAsynchronously(asyncResponse) {
            delay(50)
            "hello world"
        }
    }

    @GET
    @Path("generate_204")
    fun responseWithNoContent(@Suspended asyncResponse: AsyncResponse) {
        respondAsynchronously(asyncResponse) {
            delay(50)
        }
    }

    @GET
    @Path("generate_400")
    @Produces("text/plain")
    fun respondWithException(@Suspended asyncResponse: AsyncResponse) {
        respondAsynchronously(asyncResponse) {
            throw BadRequestException()
        }
    }

    @GET
    @Path("using_context_data")
    @Produces("text/plain")
    fun respondUsingContextData(@Suspended asyncResponse: AsyncResponse) {
        respondAsynchronously(asyncResponse) {
            "baseUri=${resteasyContextData<UriInfo>().baseUri}"
        }
    }

    private inline fun <reified T : Any> resteasyContextData(): T {
        return ResteasyProviderFactory.getContextData(T::class.java) as T
    }
}

@Path("/")
class ResourceWithThreadPool(private val threadPool: Executor) {
    @GET
    @Produces("text/plain")
    fun testResource(@Suspended asyncResponse: AsyncResponse) {
        respondAsynchronously(asyncResponse, executor = threadPool) {
            "responding on ${Thread.currentThread().name}"
        }
    }
}
