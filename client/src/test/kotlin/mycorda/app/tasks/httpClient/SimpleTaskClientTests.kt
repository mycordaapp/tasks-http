package mycorda.app.tasks.client

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isEmpty
import com.natpryce.hamkrest.isEmptyString
import com.natpryce.hamkrest.throws
import mycorda.app.registry.Registry
import mycorda.app.tasks.*
import mycorda.app.tasks.demo.CalcSquareTask
import mycorda.app.tasks.demo.ExceptionGeneratingBlockingTask
import mycorda.app.tasks.demo.echo.*
import mycorda.app.tasks.httpClient.HttpTaskClient
import mycorda.app.tasks.httpServer.Controller
import mycorda.app.tasks.logging.LoggingReaderContext
import org.http4k.server.Http4kServer
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SimpleTaskClientTests : BaseTaskClientTest() {
    private val server: Http4kServer
    private val registry: Registry
    private val port: Int

    init {
        val factory = TaskFactory()
        factory.register(CalcSquareTask::class)
        factory.register(EchoIntTask::class)
        factory.register(EchoLongTask::class)
        factory.register(EchoDoubleTask::class)
        factory.register(EchoFloatTask::class)
        factory.register(EchoBigDecimalTask::class)
        factory.register(EchoBooleanTask::class)
        factory.register(EchoStringTask::class)
        factory.register(EchoUUIDTask::class)
        factory.register(EchoEnumTask::class)
        factory.register(EchoDemoModelTask::class)
        factory.register(EchoToStdOutTask::class)
        factory.register(EchoToStdErrTask::class)
        factory.register(EchoToLogTask::class)
        factory.register(ExceptionGeneratingBlockingTask::class)

        registry = Registry()
        registry.store(factory)
        port = 12345   // todo - auto detect port
        server = Controller(registry).asServer(Jetty(port))
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
    fun `should call task and return output`() {
        val client = HttpTaskClient("http://localhost:$port")
        val clientContext = SimpleClientContext()
        val result = client.execBlocking(
            clientContext,
            "mycorda.app.tasks.demo.echo.EchoStringTask",
            "Hello, world",
            String::class
        )

        assertThat(result, equalTo("Hello, world"))
        assertNoOutput(clientContext)
    }

    @Test
    fun `should pass on task exception`() {
        val clientContext = SimpleClientContext()
        val client = HttpTaskClient("http://localhost:$port")

        assertThat({
            client.execBlocking(
                clientContext,
                "mycorda.app.tasks.demo.ExceptionGeneratingBlockingTask",
                "opps",
                String::class
            )
        }, throws<RuntimeException>())

//        assertPartialLogMessage(clientContext, "opps")
    }

    @Test
    fun `should return stdout to client`() {
        val clientContext = SimpleClientContext()
        SimpleTaskClient(registry).execBlocking(
            clientContext,
            "mycorda.app.tasks.demo.echo.EchoToStdOutTask",
            "Hello, world\n",
            Unit::class
        )

        val readerContext: LoggingReaderContext = clientContext.inMemoryLoggingContext()
        assertThat(readerContext.stdout(), equalTo("Hello, world\n"))
        assertThat(readerContext.stderr(), isEmptyString)
        assertThat(readerContext.messages(), isEmpty)
    }

    @Test
    fun `should return stderr to client`() {
        val clientContext = SimpleClientContext()
        SimpleTaskClient(registry).execBlocking(
            clientContext,
            "mycorda.app.tasks.demo.echo.EchoToStdErrTask",
            "Goodbye, cruel world\n",
            Unit::class
        )

        val readerContext: LoggingReaderContext = clientContext.inMemoryLoggingContext()
        assertThat(readerContext.stdout(), isEmptyString)
        assertThat(readerContext.stderr(), equalTo("Goodbye, cruel world\n"))
        assertThat(readerContext.messages(), isEmpty)
    }


}