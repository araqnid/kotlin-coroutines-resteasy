package org.araqnid.kotlin.coroutines.resteasy

import jakarta.ws.rs.container.AsyncResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.CancellationException
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

fun <T> CoroutineScope.respondAsynchronously(asyncResponse: AsyncResponse,
                                             context: CoroutineContext = EmptyCoroutineContext,
                                             block: suspend CoroutineScope.() -> T): Job {
    return launch(context + ResteasyContext()) {
        try {
            asyncResponse.resume(block().takeUnless { it == Unit })
        } catch (e: Throwable) {
            asyncResponse.resume(e)
        }
    }.also { job ->
        asyncResponse.setTimeoutHandler { job.cancel(CancellationException("Response timeout")) }
    }
}
