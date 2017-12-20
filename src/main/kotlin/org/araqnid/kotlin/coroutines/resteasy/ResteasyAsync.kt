package org.araqnid.kotlin.coroutines.resteasy

import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.DefaultDispatcher
import kotlinx.coroutines.experimental.Delay
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.asCoroutineDispatcher
import kotlinx.coroutines.experimental.launch
import org.jboss.resteasy.spi.ResteasyProviderFactory
import java.util.concurrent.Executor
import javax.ws.rs.container.AsyncResponse
import kotlin.coroutines.experimental.ContinuationInterceptor
import kotlin.coroutines.experimental.CoroutineContext

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

fun <T> respondAsynchronously(asyncResponse: AsyncResponse, context: CoroutineContext = DefaultDispatcher, parent: Job? = null, block: suspend CoroutineScope.() -> T): Job {
    return launch(createContext(context), parent = parent) {
        try {
            asyncResponse.resume(block().let { if (it == Unit) null else it })
        } catch (e: Throwable) {
            asyncResponse.resume(e)
        }
    }.also { asyncResponse.setTimeoutHandler { it.cancel() } }
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

fun <T> respondAsynchronously(asyncResponse: AsyncResponse, executor: Executor, parent: Job? = null, block: suspend CoroutineScope.() -> T): Job {
    return respondAsynchronously(asyncResponse, executor.asCoroutineDispatcher(), parent, block)
}
