package org.araqnid.kotlin.coroutines.resteasy

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.ws.rs.container.AsyncResponse
import kotlin.coroutines.CoroutineContext

fun <T> CoroutineScope.respondAsynchronously(asyncResponse: AsyncResponse, context: CoroutineContext = coroutineContext, block: suspend CoroutineScope.() -> T): Job {
    return launch(context + ResteasyContext()) {
        try {
            asyncResponse.resume(block().takeUnless { it == Unit })
        } catch (e: Throwable) {
            asyncResponse.resume(e)
        }
    }.also { job ->
        asyncResponse.setTimeoutHandler { job.cancel() }
    }
}
