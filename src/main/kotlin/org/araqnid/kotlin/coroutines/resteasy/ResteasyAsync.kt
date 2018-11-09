@file:UseExperimental(InternalCoroutinesApi::class)

package org.araqnid.kotlin.coroutines.resteasy

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import org.jboss.resteasy.spi.ResteasyProviderFactory
import java.util.concurrent.Executor
import javax.ws.rs.container.AsyncResponse
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext

internal open class ResteasyInterceptor(
        private val data: Map<Class<*>, Any> = ResteasyProviderFactory.getContextDataMap(),
        private val nextDispatcher: CoroutineDispatcher
) : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        nextDispatcher.dispatch(context, Runnable {
            ResteasyProviderFactory.pushContextDataMap(data)
            try {
                block.run()
            } finally {
                ResteasyProviderFactory.removeContextDataLevel()
            }
        })
    }
}

internal class ResteasyInterceptorWithDelay(
        data: Map<Class<*>, Any> = ResteasyProviderFactory.getContextDataMap(),
        nextDispatcher: CoroutineDispatcher,
        underlyingDelay: Delay
) : ResteasyInterceptor(data, nextDispatcher), Delay by underlyingDelay

fun <T> CoroutineScope.respondAsynchronously(asyncResponse: AsyncResponse, context: CoroutineContext = Dispatchers.Default, block: suspend CoroutineScope.() -> T): Job {
    return this@respondAsynchronously.launch(createContext(context)) {
        try {
            asyncResponse.resume(block().let { if (it == Unit) null else it })
        } catch (e: Throwable) {
            asyncResponse.resume(e)
        }
    }.also { job ->
        asyncResponse.setTimeoutHandler { job.cancel() }
    }
}

private fun createContext(parentContext: CoroutineContext): CoroutineContext {
    val existingInterceptor = parentContext[ContinuationInterceptor]!! as CoroutineDispatcher
    val resteasyInterceptor = when (existingInterceptor) {
        is Delay -> ResteasyInterceptorWithDelay(nextDispatcher = existingInterceptor,
                underlyingDelay = existingInterceptor)
        else -> ResteasyInterceptor(nextDispatcher = existingInterceptor)
    }
    return parentContext + resteasyInterceptor
}

fun <T> CoroutineScope.respondAsynchronously(asyncResponse: AsyncResponse, executor: Executor, block: suspend CoroutineScope.() -> T): Job {
    return respondAsynchronously(asyncResponse,
            executor.asCoroutineDispatcher(),
            block)
}
