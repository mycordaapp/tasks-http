package mycorda.app.tasks.httpClient

import org.http4k.core.Request
import org.http4k.routing.*
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsConsumer
import org.http4k.websocket.WsFilter


val handler = websockets(
    "/logChannel/{id}/stdout" bind { ws: Websocket ->
        ws.onMessage {
            println("server received: $it")
        }
    }

)

class LogChannelController : RoutingWsHandler {
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