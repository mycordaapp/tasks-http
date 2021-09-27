package mycorda.app.tasks.httpClient

import ClientContext
import TaskClient
import mycorda.app.tasks.AsyncResultChannelSinkLocator
import mycorda.app.tasks.UniqueId
import mycorda.app.tasks.common.TaskSerializer
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

class HttpTaskClient(
    private val baseUrl: String
) : TaskClient {
    private val taskSerializer = TaskSerializer()
    override fun <I, O> execAsync(
        ctx: ClientContext,
        taskName: String,
        channelLocator: AsyncResultChannelSinkLocator,
        channelId: UniqueId,
        input: I
    ) {
        TODO("Not yet implemented")
    }

    override fun <I, O> execBlocking(ctx: ClientContext, taskName: String, input: I): O {
        val body = inputToString(input)
        val url = buildUrl(baseUrl, taskName, ctx, null)

        val request = Request(Method.POST, url)
            .body(body)

        val result = runRequest(request, taskName, 10)

        val deserialised = taskSerializer.deserializeResult(result, Any::class)
        return deserialised as O

    }

    private fun <I> inputToString(input: I): String {
        if (input != null) {
            return taskSerializer.serializeResult(input as Any)
        } else {
            return ""
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
            .build()).build()

        return ApacheClient(client = closeable)
    }

    private fun buildUrl(
        baseUrl: String,
        task: String,
        ctx: ClientContext,
        timeout: Int?
    ): String {
        var paramMarker = "?"
        val sb = StringBuilder(baseUrl)
        if (!sb.endsWith("/")) sb.append("/")
        sb.append("api/task/").append(task).append("/exec")
//        if (ctx.provisioningState() != null && ctx.provisioningState().stages().isNotEmpty()) {
//            paramMarker = "&"
//            val data = HashMap<String, Any>()
//            ctx.provisioningState().stages().forEach {
//                data.put(it, ctx.provisioningState().outputs(it))
//            }
//            val json = URLEncoder.encode(JSONObject(data).toString(2))
//            sb.append("?provisioningState=$json")
//        }
//        if (ctx.instanceQualifier() != null) {
//            sb.append(paramMarker)
//            sb.append("instanceQualifier=${ctx.instanceQualifier()}")
//            paramMarker = "&"
//        }
        if (timeout != null) {
            sb.append(paramMarker)
            sb.append("timeout=$timeout")
        }
        return sb.toString()
    }
}