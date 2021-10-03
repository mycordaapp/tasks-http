package mycorda.app.tasks.httpServer

import mycorda.app.registry.Registry
import mycorda.app.tasks.BlockingTask
import mycorda.app.tasks.TaskFactory
import mycorda.app.tasks.executionContext.SimpleExecutionContext
import mycorda.app.tasks.serialisation.JsonSerialiser
import org.http4k.core.*
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import kotlin.reflect.KClass
import kotlin.reflect.KFunction1

data class ExecBlockingTaskRequest(val task: String, val input: Any)

class Controller(private val registry: Registry) : HttpHandler {
    private val serializer = registry.geteOrElse(JsonSerialiser::class.java, JsonSerialiser())
    private val taskFactory = registry.get(TaskFactory::class.java)

    private val routes: RoutingHttpHandler = routes(
        "/api" bind routes(
            "/status" bind Method.GET to {
                Response.text("running")
            },
            "/exec" bind Method.POST to {
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
        val ctx = SimpleExecutionContext()
        val inputClazz = clazz(model.inputClazz)

        val input = serializer.deserialiseData(model.inputSerialized, inputClazz)

        val result = task.exec(ctx, input as Any)

        val x = serializer.serialiseData(result)

        return Response.text(x)
    }

    @Suppress("UNCHECKED_CAST")
    fun clazz(clazzName: String): KClass<Any> {
        return when (clazzName) {
            "kotlin.Int" -> 1::class as KClass<Any>
            "kotlin.Long" -> 1L::class as KClass<Any>
            "kotlin.Double" -> 1.23::class as KClass<Any>
            "kotlin.Float" -> 1.23f::class as KClass<Any>
            "kotlin.Boolean" -> true::class as KClass<Any>
            else -> Class.forName(clazzName).kotlin as KClass<Any>
        }
    }

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