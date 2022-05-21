package org.araqnid.kotlin.coroutines.resteasy

import kotlinx.coroutines.ThreadContextElement
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import org.jboss.resteasy.core.ResteasyContext as RealResteasyContext

typealias ResteasyContextMap = Map<Class<*>, Any>

class ResteasyContext(val contextMap: ResteasyContextMap = RealResteasyContext.getContextDataMap()) :
        ThreadContextElement<ResteasyContextMap>,
        AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<ResteasyContext>

    override fun updateThreadContext(context: CoroutineContext): ResteasyContextMap {
        val oldState = RealResteasyContext.getContextDataMap()
        RealResteasyContext.pushContextDataMap(contextMap)
        return oldState
    }

    override fun restoreThreadContext(context: CoroutineContext, oldState: ResteasyContextMap) {
        RealResteasyContext.pushContextDataMap(oldState)
    }
}
