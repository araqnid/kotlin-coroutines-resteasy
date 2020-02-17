package org.araqnid.kotlin.coroutines.resteasy

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.ws.rs.container.AsyncResponse

fun <T> CoroutineScope.respondAsynchronously(asyncResponse: AsyncResponse, block: suspend CoroutineScope.() -> T): Job {
    return this@respondAsynchronously.launch(coroutineContext + ResteasyContext()) {
        try {
            asyncResponse.resume(block().let { if (it == Unit) null else it })
        } catch (e: Throwable) {
            asyncResponse.resume(e)
        }
    }.also { job ->
        asyncResponse.setTimeoutHandler { job.cancel() }
    }
}
