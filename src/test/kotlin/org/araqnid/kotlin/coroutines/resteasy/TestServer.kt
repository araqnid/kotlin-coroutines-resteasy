package org.araqnid.kotlin.coroutines.resteasy

import jakarta.servlet.DispatcherType
import jakarta.servlet.ServletContextEvent
import org.eclipse.jetty.server.NetworkConnector
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.servlet.DefaultServlet
import org.eclipse.jetty.servlet.FilterHolder
import org.eclipse.jetty.servlet.ServletContextHandler
import org.jboss.resteasy.plugins.server.servlet.Filter30Dispatcher
import org.jboss.resteasy.plugins.server.servlet.ResteasyBootstrap
import org.jboss.resteasy.spi.ResteasyDeployment
import java.net.URI
import java.util.*

suspend fun <T> withServer(vararg jaxrsResources: Any, block: suspend ServerScope.() -> T): T {
    val server = Server().apply {
        addConnector(ServerConnector(this).apply {
            port = 0
        })
        handler = ServletContextHandler().apply {
            val resteasyBootstrap = object : ResteasyBootstrap() {
                override fun contextInitialized(event: ServletContextEvent) {
                    super.contextInitialized(event)
                    val context = event.servletContext
                    val deployment = context.getAttribute(ResteasyDeployment::class.java.name) as ResteasyDeployment
                    val registry = deployment.registry
                    jaxrsResources.forEach { registry.addSingletonResource(it) }
                }
            }
            val resteasyFilter = Filter30Dispatcher()
            addFilter(FilterHolder(resteasyFilter), "/*", EnumSet.of(DispatcherType.REQUEST))
            addServlet(DefaultServlet::class.java, "/")
            addEventListener(resteasyBootstrap)
        }
    }

    server.start()
    val port = (server.connectors[0] as NetworkConnector).localPort
    try {
        return block(ServerScope(URI("http://localhost:$port/")))
    } finally {
        server.stop()
    }
}

data class ServerScope(val uri: URI)
