package mycorda.app.tasks.httpClient

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isEmpty
import com.natpryce.hamkrest.isEmptyString
import mycorda.app.registry.Registry
import mycorda.app.tasks.TaskFactory
import mycorda.app.tasks.client.SimpleClientContext
import mycorda.app.tasks.client.SimpleTaskClient
import mycorda.app.tasks.demo.CalcSquareTask
import mycorda.app.tasks.demo.echo.*
import mycorda.app.tasks.httpServer.Controller
import mycorda.app.tasks.httpServer.TheApp
import mycorda.app.tasks.logging.LoggingReaderContext
import org.http4k.server.Http4kServer
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.junit.jupiter.api.*
import java.math.BigDecimal
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HttpTaskClientTests {
    private val app = TheApp()
    // client (for callback)
    private val theClient = TheClientApp()

    @BeforeAll
    fun `start`() {
        app.start()
        theClient.start()
    }

    @AfterAll
    fun `stop`() {
        app.stop()
        theClient.stop()
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

    @Test
    fun `should return stdout to client`() {
        val client = HttpTaskClient("http://localhost:1234")
        val ctx = SimpleClientContext()

        client.execBlocking(
            ctx, "mycorda.app.tasks.demo.echo.EchoToStdOutTask",
            "Hello, world\n",
            Unit::class
        )

//        val clientContext = SimpleClientContext()
//        SimpleTaskClient(registry).execBlocking(
//            clientContext,
//            "mycorda.app.tasks.demo.echo.EchoToStdOutTask",
//            "Hello, world\n",
//            Unit::class
//        )

//        val readerContext: LoggingReaderContext = ctx.inMemoryLoggingContext()
//        assertThat(readerContext.stdout(), equalTo("Hello, world\n"))
//        assertThat(readerContext.stderr(), isEmptyString)
//        assertThat(readerContext.messages(), isEmpty)
    }
}