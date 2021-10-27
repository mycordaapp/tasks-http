package mycorda.app.tasks.httpServer


import mycorda.app.tasks.logging.LogMessage
import mycorda.app.tasks.logging.LoggingConsumerContext
import org.http4k.client.WebsocketClient
import org.http4k.core.Uri
import org.http4k.websocket.WsMessage


class WsCallbackLoggingConsumerContext(private val baseUrl: String, private val channelId: String) :
    LoggingConsumerContext {
    override fun acceptLog(msg: LogMessage) {
        val blockingClient = WebsocketClient.blocking(Uri.of("${baseUrl}/logChannel/${channelId}/log"))
        blockingClient.send(WsMessage(msg.body))
        blockingClient.close()
    }

    override fun acceptStderr(error: String) {
        val blockingClient = WebsocketClient.blocking(Uri.of("${baseUrl}/logChannel/${channelId}/stderr"))
        blockingClient.send(WsMessage(error))
        blockingClient.close()
    }

    override fun acceptStdout(output: String) {
        val uri = Uri.of("${baseUrl}/logChannel/${channelId}/stdout")
        val blockingClient = WebsocketClient.blocking(uri)
        blockingClient.send(WsMessage(output))
        blockingClient.close()
    }

}