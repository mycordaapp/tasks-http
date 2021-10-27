package mycorda.app.tasks.httpClient

import mycorda.app.registry.Registry
import org.http4k.core.Request
import org.http4k.routing.*
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsConsumer
import org.http4k.websocket.WsFilter

val handler = websockets(
    "/logChannel/{id}/stdout" bind { ws: Websocket ->
        ws.onMessage {
            println("server received stdout: $it")
        }
    },
    "/logChannel/{id}/stderr" bind { ws: Websocket ->
        ws.onMessage {
            println("server received stderr: $it")
        }
    },
    "/logChannel/{id}/log" bind { ws: Websocket ->
        ws.onMessage {
            println("server received log: $it")
        }
    }
)

class LogChannelController(registry: Registry) : RoutingWsHandler {
    override fun invoke(p1: Request): WsConsumer {
        return handler.invoke(p1)
    }

    override fun match(request: Request): WsRouterMatch {
        return handler.match(request)
    }

    override fun withBasePath(new: String): RoutingWsHandler {
        return handler.withBasePath(new)
    }

    override fun withFilter(new: WsFilter): RoutingWsHandler {
        return handler.withFilter(new)
    }
}