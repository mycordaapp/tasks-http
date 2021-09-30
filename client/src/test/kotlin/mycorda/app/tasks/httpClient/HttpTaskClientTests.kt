package mycorda.app.tasks.httpClient

import SimpleClientContext
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import mycorda.app.registry.Registry
import mycorda.app.tasks.TaskFactory
import mycorda.app.tasks.demo.CalcSquareTask
import mycorda.app.tasks.demo.echo.*
import mycorda.app.tasks.httpServer.Controller
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.*


class HttpTaskClientTests {
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

    @Test
    fun `should serialise scalars correctly`() {
        val random = Random()
        val combinations = listOf(
            Pair(random.nextLong(), "EchoLongTask"),
            Pair(random.nextInt(), "EchoIntTask"),
            Pair(random.nextDouble(), "EchoDoubleTask"),
            Pair(random.nextFloat(), "EchoFloatTask"),
            Pair(random.nextBoolean(), "EchoBooleanTask"),
            Pair(BigDecimal(random.nextDouble()), "EchoBigDecimalTask"),
            Pair(UUID.randomUUID(), "EchoUUIDTask")
        )

        val client = HttpTaskClient("http://localhost:1234")
        val ctx = SimpleClientContext()

        combinations.forEach {
            val param = it.first
            val result = client.execBlocking(
                ctx, "mycorda.app.tasks.demo.echo.${it.second}", param, param::class
            )
            assertThat(result, equalTo(param)) { "Didn't echo correct value for combination: $it" }
        }
    }

}