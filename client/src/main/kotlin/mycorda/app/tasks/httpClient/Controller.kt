package mycorda.app.tasks.httpClient

import mycorda.app.registry.Registry
import mycorda.app.tasks.BlockingTask
import mycorda.app.tasks.TaskFactory
import mycorda.app.tasks.executionContext.SimpleExecutionContext
import org.http4k.core.*
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import kotlin.reflect.KFunction1
import mycorda.app.tasks.httpCommon.Serialiser
import mycorda.app.tasks.httpCommon.json
import mycorda.app.tasks.httpCommon.text
import mycorda.app.tasks.logging.InMemoryLoggingConsumerContext
import mycorda.app.tasks.logging.LoggingProducerToConsumer

class Controller(private val registry: Registry) : HttpHandler {
    private val serializer = registry.geteOrElse(Serialiser::class.java, Serialiser())
    private val taskFactory = registry.get(TaskFactory::class.java)

    private val routes: RoutingHttpHandler = routes(
        "/api" bind routes(
            "/status" bind Method.GET to {
                Response.text("running")
            },
            "/channel/{" bind Method.POST to {
                exceptionWrapper(::handleExecTask, it)
            }
        ),
        "/ping" bind Method.GET to {
            Response.text("pong")
        }
    )

    private fun handleExecTask(it: Request): Response {
        val model = serializer.deserialiseBlockingTaskRequest(it.bodyString())


        @Suppress("UNCHECKED_CAST")
        val task = taskFactory.createInstance(model.task) as BlockingTask<Any, Any>
        val inputDeserialised = serializer.deserialiseData(model.inputSerialized)

        // hook in logging producer / consumer pair
        val x = InMemoryLoggingConsumerContext()
        val loggingProducerContext = LoggingProducerToConsumer(x)
        val ctx = SimpleExecutionContext(loggingProducerContext = loggingProducerContext)

        return try {
            val output = task.exec(ctx, inputDeserialised.any())

            x.stdout()
            val outputSerialised = serializer.serialiseData(output)


            Response.json(outputSerialised)
        } catch (ex: Exception) {
            val exceptionSerialised = serializer.serialiseData(ex)
            Response.json(exceptionSerialised)
        }

    }

//    @Suppress("UNCHECKED_CAST")
//    fun clazz(clazzName: String): KClass<Any> {
//        return when (clazzName) {
//            "kotlin.Int" -> 1::class as KClass<Any>
//            "kotlin.Long" -> 1L::class as KClass<Any>
//            "kotlin.Double" -> 1.23::class as KClass<Any>
//            "kotlin.Float" -> 1.23f::class as KClass<Any>
//            "kotlin.Boolean" -> true::class as KClass<Any>
//            "kotlin.String" -> ""::class as KClass<Any>
//            else -> Class.forName(clazzName).kotlin as KClass<Any>
//        }
//    }

    private fun exceptionWrapper(x: KFunction1<Request, Response>, i: Request): Response {
        return try {
            x.invoke(i)
        } catch (ex: Exception) {
            ex.printStackTrace()
            Response(Status.INTERNAL_SERVER_ERROR).text(ex.message!!)
        }
    }

    override fun invoke(p1: Request): Response = routes(p1)

}