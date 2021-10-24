package mycorda.app.tasks.httpServer

import mycorda.app.registry.Registry
import mycorda.app.tasks.TaskFactory
import mycorda.app.tasks.demo.DemoTasks
import mycorda.app.tasks.demo.echo.EchoTasks
import org.http4k.server.Http4kServer
import org.http4k.server.Jetty
import org.http4k.server.asServer

fun main(args: Array<String>) {
    // todo - how to inject in ?
    //   - server listner bindings
    //   - a list of TaskRegistrations
    val registry = Registry()
    TheApp(registry, 1234)
}


class TheApp(registry: Registry = Registry(), port: Int = 1234) {
    private val server: Http4kServer

    init {
        val factory = TaskFactory()
        factory.register(DemoTasks())
        factory.register(EchoTasks())
        registry.store(factory)

        server = Controller(registry).asServer(Jetty(port))
        println("Server started on $port")
    }

    fun start() {
        server.start()
    }

    fun stop() {
        server.stop()
    }
}