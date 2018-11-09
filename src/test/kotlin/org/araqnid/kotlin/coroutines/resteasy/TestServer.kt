package org.araqnid.kotlin.coroutines.resteasy

import org.apache.http.HttpHost
import org.apache.http.conn.routing.HttpRoute
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
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
import java.util.EnumSet
import javax.servlet.DispatcherType
import javax.servlet.ServletContextEvent

fun <T> withServer(vararg jaxrsResources: Any, block: ServerScope.() -> T): T {
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
    val host = HttpHost("localhost", port)
    try {
        HttpClients.custom()
                .setRoutePlanner { requestedHost, _, _ ->
                    require(requestedHost == null || requestedHost == host)
                    HttpRoute(host)
                }
                .build().use { httpClient ->
            return block(ServerScope(URI("http://localhost:$port/"),
                    httpClient))
        }
    } finally {
        server.stop()
    }
}

data class ServerScope(val uri: URI, val httpClient: CloseableHttpClient)
