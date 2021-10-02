package mycorda.app.tasks.httpClient

import SimpleClientContext
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import mycorda.app.registry.Registry
import mycorda.app.tasks.TaskFactory
import mycorda.app.tasks.demo.CalcSquareTask
import mycorda.app.tasks.demo.echo.*
import mycorda.app.tasks.httpServer.Controller
import org.http4k.server.Http4kServer
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.junit.jupiter.api.*
import java.math.BigDecimal
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HttpTaskClientTests {
    private val server: Http4kServer

    init {
        val factory = TaskFactory()
        factory.register(CalcSquareTask::class)
        factory.register(EchoIntTask::class)
        factory.register(EchoLongTask::class)
        factory.register(EchoDoubleTask::class)
        factory.register(EchoFloatTask::class)
        factory.register(EchoBigDecimalTask::class)
        factory.register(EchoBooleanTask::class)
        factory.register(EchoUUIDTask::class)
        factory.register(EchoEnumTask::class)
        factory.register(EchoDemoModelTask::class)

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

    @Test
    fun `should serialise echoed output back to original input`() {
        // basically we check "round-tripping" via the serialiser, i.e. do we get back the
        // original inpout
        val random = Random()
        val combinations = listOf(
            Pair(random.nextLong(), "EchoLongTask"),
            Pair(random.nextInt(), "EchoIntTask"),
            Pair(random.nextDouble(), "EchoDoubleTask"),
            Pair(random.nextFloat(), "EchoFloatTask"),
            Pair(random.nextBoolean(), "EchoBooleanTask"),
            Pair(BigDecimal(random.nextDouble()), "EchoBigDecimalTask"),
            Pair(UUID.randomUUID(), "EchoUUIDTask"),
            Pair(Colour.random(), "EchoEnumTask"),
            Pair(DemoModel(), "EchoDemoModelTask")
        )
        val client = HttpTaskClient("http://localhost:1234")
        val ctx = SimpleClientContext()

        combinations.forEach {
            try {
                val param = it.first
                val result = client.execBlocking(
                    ctx, "mycorda.app.tasks.demo.echo.${it.second}", param, param::class
                )
                assertThat(result, equalTo(param)) { "Didn't echo correct value for combination: $it" }
            } catch (ex: Exception) {
                fail { "Combination $it failed with ${ex.message}" }
            }
        }
    }

}