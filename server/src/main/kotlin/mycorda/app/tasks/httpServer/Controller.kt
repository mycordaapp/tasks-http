package mycorda.app.tasks.httpServer

import mycorda.app.registry.Registry
import mycorda.app.tasks.BlockingTask
import mycorda.app.tasks.TaskFactory
import mycorda.app.tasks.common.JsonSerializer
import mycorda.app.tasks.executionContext.SimpleExecutionContext
import org.http4k.core.*
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import kotlin.reflect.KClass
import kotlin.reflect.KFunction1

data class ExecBlockingTaskRequest(val task: String, val input: Any)

class Controller(private val registry: Registry) : HttpHandler {

    private val serializer = registry.geteOrElse(JsonSerializer::class.java, JsonSerializer())
    private val taskFactory = registry.get(TaskFactory::class.java)

    private val routes: RoutingHttpHandler = routes(
        "/api" bind routes(
            "/status" bind Method.GET to {
                Response.text("running")
            },
            "/exec" bind Method.POST to {
                execeptionWrapper(::handleExecTask, it)
            }
        ),
        "/ping" bind Method.GET to {
            Response.text("pong")
        }
    )

    private fun handleExecTask(it: Request): Response {
        println("here we are")
        println(it.bodyString())

        val model = serializer.deserializeBlockingTaskRequest(it.bodyString())
        println(model)

        val i: Int = 1
        val t = taskFactory.createInstance(model.task) as BlockingTask<Any, Any>

        val ctx = SimpleExecutionContext()

        val inputClazz = clazz(model.inputClazz)

        val input = serializer.deserializeResult(model.input, inputClazz)

        val result = t.exec(ctx, input as Any)

        val x = serializer.serializeResult(result)

        return Response.text(x)
    }

    fun clazz(clazzName: String): KClass<Any> {
        return if (clazzName == "kotlin.Int") 1::class as KClass<Any>
        else Class.forName(clazzName).kotlin as KClass<Any>
    }

    fun execeptionWrapper(x: KFunction1<Request, Response>, i: Request): Response {
        return try {
            x.invoke(i)
        } catch (ex: Exception) {
            ex.printStackTrace()
            Response(Status.INTERNAL_SERVER_ERROR).text(ex.message!!)
        }
    }

    override fun invoke(p1: Request): Response = routes(p1)

}