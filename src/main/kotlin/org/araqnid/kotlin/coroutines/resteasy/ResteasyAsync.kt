package org.araqnid.kotlin.coroutines.resteasy

import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.newCoroutineContext
import org.jboss.resteasy.spi.ResteasyProviderFactory
import java.util.concurrent.Executor
import java.util.concurrent.ForkJoinPool
import javax.ws.rs.container.AsyncResponse
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.startCoroutine

class ResteasyAsync<in T>(
        private val asyncResponse: AsyncResponse,
        private val data: Map<Class<*>, Any> = ResteasyProviderFactory.getContextDataMap(),
        private val executor: Executor
) : CoroutineDispatcher(), Continuation<T> {
    override val context: CoroutineContext = newCoroutineContext(this@ResteasyAsync)

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        executor.execute {
            ResteasyProviderFactory.pushContextDataMap(data)
            try {
                block.run()
            } finally {
                ResteasyProviderFactory.removeContextDataLevel()
            }
        }
    }

    override fun resume(value: T) {
        asyncResponse.resume(if (value == Unit) null else value)
    }

    override fun resumeWithException(exception: Throwable) {
        asyncResponse.resume(exception)
    }
}

fun <T> respondAsynchronously(asyncResponse: AsyncResponse, executor: Executor = ForkJoinPool.commonPool(), block: suspend () -> T) {
    block.startCoroutine(ResteasyAsync(asyncResponse, executor = executor))
}
