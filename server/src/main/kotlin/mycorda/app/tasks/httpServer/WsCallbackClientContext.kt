package mycorda.app.tasks.httpServer


import mycorda.app.tasks.logging.LogMessage
import mycorda.app.tasks.logging.LoggingConsumerContext
import org.http4k.client.WebsocketClient
import org.http4k.core.Uri
import org.http4k.websocket.WsMessage


class WsCallbackLoggingConsumerContext(private val url: String) : LoggingConsumerContext {
    override fun acceptLog(msg: LogMessage) {
        val blockingClient = WebsocketClient.blocking(Uri.of("${url}/logChannel/1234/log"))
        blockingClient.send(WsMessage(msg.body))
        blockingClient.close()
        //println(msg)
    }

    override fun acceptStderr(error: String) {
        val blockingClient = WebsocketClient.blocking(Uri.of("${url}/logChannel/1234/stderr"))
        blockingClient.send(WsMessage(error))
        blockingClient.close()
        //println(error)
    }

    override fun acceptStdout(output: String) {
        val blockingClient = WebsocketClient.blocking(Uri.of("${url}/logChannel/1234/stdout"))
        blockingClient.send(WsMessage(output))
        blockingClient.close()
    }

}