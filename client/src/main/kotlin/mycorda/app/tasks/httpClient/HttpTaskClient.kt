package mycorda.app.tasks.httpClient

import ClientContext
import TaskClient
import mycorda.app.tasks.AsyncResultChannelSinkLocator
import mycorda.app.tasks.UniqueId
import mycorda.app.tasks.common.BlockingTaskRequest
import mycorda.app.tasks.common.JsonSerializer
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.core5.util.Timeout
import org.http4k.client.ApacheClient
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import java.lang.RuntimeException
import java.lang.StringBuilder
import kotlin.reflect.KClass

class HttpTaskClient(
    private val baseUrl: String
) : TaskClient {
    private val serializer = JsonSerializer()
    override fun <I, O : Any> execAsync(
        ctx: ClientContext,
        taskName: String,
        channelLocator: AsyncResultChannelSinkLocator,
        channelId: UniqueId,
        input: I,
        outputClazz: KClass<O>
    ) {
        TODO("Not yet implemented")
    }

    override fun <I, O : Any> execBlocking(
        ctx: ClientContext,
        taskName: String,
        input: I,
        outputClazz: KClass<O>
    ): O {
        val url = buildUrl(baseUrl, ctx, null)
        val model = BlockingTaskRequest(
            task = taskName,
            input = inputToString(input),
            inputClazz = input!!::class.qualifiedName!!,
            outputClazz = outputClazz.qualifiedName!!
        )
        val body = serializer.serialize(model)

        val request = Request(Method.POST, url)
            .body(body)

        val result = runRequest(request, taskName, 10)

        val deserialized = serializer.deserializeResult(result, outputClazz)
        return deserialized as O
    }


    private fun <I> inputToString(input: I): String {
        return if (input != null) {
            serializer.serializeResult(input as Any, true)
        } else {
            ""
        }
    }

    private fun runRequest(request: Request, task: String, timeoutSec: Int = 120): String {
        val client: HttpHandler = apacheClient(timeoutSec)
        val result = client(request)

        if (result.status != Status.OK) {
            throw RuntimeException("opps, status of ${result.status} running ${task}\n${result.bodyString()} at $request")
        } else {
            return result.bodyString()
        }
    }

    private fun apacheClient(timeoutSec: Int): HttpHandler {
        val closeable = HttpClients.custom().setDefaultRequestConfig(
            RequestConfig.custom()
                .setRedirectsEnabled(false)
                .setConnectTimeout(Timeout.ofMilliseconds(1000))
                //.setSocketTimeout(timeoutSec * 1000)
                //.setCookieSpec(CookieSpecs.IGNORE_COOKIES)
                .build()
        ).build()

        return ApacheClient(client = closeable)
    }

    private fun buildUrl(
        baseUrl: String,
        ctx: ClientContext,
        timeout: Int?
    ): String {
        var paramMarker = "?"
        val sb = StringBuilder(baseUrl)
        if (!sb.endsWith("/")) sb.append("/")
        sb.append("api/exec/")
        if (timeout != null) {
            sb.append(paramMarker)
            sb.append("timeout=$timeout")
        }
        return sb.toString()
    }
}