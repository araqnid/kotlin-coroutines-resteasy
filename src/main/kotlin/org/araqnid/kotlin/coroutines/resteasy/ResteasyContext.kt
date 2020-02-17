package org.araqnid.kotlin.coroutines.resteasy

import kotlinx.coroutines.ThreadContextElement
import org.jboss.resteasy.spi.ResteasyProviderFactory
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

typealias ResteasyContextMap = Map<Class<*>, Any>

class ResteasyContext(val contextMap: ResteasyContextMap = ResteasyProviderFactory.getContextDataMap()) :
        ThreadContextElement<ResteasyContextMap>,
        AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<ResteasyContext>

    override fun updateThreadContext(context: CoroutineContext): ResteasyContextMap {
        val oldState = ResteasyProviderFactory.getContextDataMap()
        ResteasyProviderFactory.pushContextDataMap(contextMap)
        return oldState
    }

    override fun restoreThreadContext(context: CoroutineContext, oldState: ResteasyContextMap) {
        ResteasyProviderFactory.pushContextDataMap(oldState)
    }
}
