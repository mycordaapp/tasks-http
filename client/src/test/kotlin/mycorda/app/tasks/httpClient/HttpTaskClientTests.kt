package mycorda.app.tasks.httpClient

import SimpleClientContext
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import mycorda.app.registry.Registry
import mycorda.app.tasks.TaskFactory
import mycorda.app.tasks.demo.CalcSquareTask
import mycorda.app.tasks.httpServer.Controller
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.junit.jupiter.api.Test


class HttpTaskClientTests {
    init {
        val factory = TaskFactory()
        factory.register(CalcSquareTask::class)
        val registry = Registry().store(factory)
        val server = Controller(registry).asServer(Jetty(1234)).start()
    }

//    @Test
//    fun `should do something`() {
//        var x = 2
//        assertThat(x + 1, equalTo(3))
//        assert(x == 2)
//    }

    @Test
    fun `should call blocking task`() {
        val client = HttpTaskClient("http://localhost:1234")
        val ctx = SimpleClientContext()

        val result = client.execBlocking(
            ctx, "mycorda.app.tasks.demo.CalcSquareTask", 10, Int::class
        )
        println(result)
    }

}