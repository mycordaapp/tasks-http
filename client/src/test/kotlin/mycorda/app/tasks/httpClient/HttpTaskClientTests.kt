package mycorda.app.tasks.httpClient

import SimpleClientContext
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import mycorda.app.registry.Registry
import mycorda.app.tasks.TaskFactory
import mycorda.app.tasks.demo.CalcSquareTask
import mycorda.app.tasks.httpServer.Controller
import org.http4k.server.Http4kServer
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HttpTaskClientTests {
    private val server: Http4kServer

    init {
        val factory = TaskFactory()
        factory.register(CalcSquareTask::class)
        val registry = Registry().store(factory)
        server = Controller(registry).asServer(Jetty(1234))
    }

    @BeforeAll
    fun `start`() {
        server.start()
    }

    @AfterAll
    fun `stop`() {
        server.stop()
    }

    @Test
    fun `should call blocking task`() {
        val client = HttpTaskClient("http://localhost:1234")
        val ctx = SimpleClientContext()

        val result = client.execBlocking(
            ctx, "mycorda.app.tasks.demo.CalcSquareTask", 10, Int::class
        )
        assertThat(result, equalTo(100))
    }


}