package mycorda.app.tasks.httpClient

import mycorda.app.registry.Registry
import org.http4k.server.Http4kServer
import org.http4k.server.Jetty
import org.http4k.server.asServer

fun main(args: Array<String>) {
    // todo - how to inject in ?
    //   - server listner bindings
    val registry = Registry()
    TheClientApp(registry, 12345)
}

class TheClientApp(private val registry: Registry = Registry(), port: Int = 12345) {
    private val server: Http4kServer

    init {
        server = LogChannelController(registry).asServer(Jetty(port)).start()
    }

    fun start() {
        server.start()
    }

    fun stop() {
        server.stop()
    }

}
