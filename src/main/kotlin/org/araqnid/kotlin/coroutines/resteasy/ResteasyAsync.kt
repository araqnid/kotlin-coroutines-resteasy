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

internal open class ResteasyInterceptorWithDelay(
        data: Map<Class<*>, Any> = ResteasyProviderFactory.getContextDataMap(),
        nextDispatcher: CoroutineDispatcher,
        private val underlyingDelay: Delay
) : ResteasyInterceptor(data, nextDispatcher), Delay by underlyingDelay

fun <T> respondAsynchronously(asyncResponse: AsyncResponse, context: CoroutineContext = DefaultDispatcher, block: suspend CoroutineScope.() -> T): Job {
    val existingInterceptor = context[ContinuationInterceptor]!! as CoroutineDispatcher
    val resteasyInterceptor = when (existingInterceptor) {
        is Delay -> ResteasyInterceptorWithDelay(nextDispatcher = existingInterceptor, underlyingDelay = existingInterceptor)
        else -> ResteasyInterceptor(nextDispatcher = existingInterceptor)
    }
    return launch(context + resteasyInterceptor) {
        try {
            asyncResponse.resume(block().let { if (it == Unit) null else it })
        } catch (e: Throwable) {
            asyncResponse.resume(e)
        }
    }
}

fun <T> respondAsynchronously(asyncResponse: AsyncResponse, executor: Executor, block: suspend CoroutineScope.() -> T): Job {
    return respondAsynchronously(asyncResponse, executor.asCoroutineDispatcher(), block)
}
