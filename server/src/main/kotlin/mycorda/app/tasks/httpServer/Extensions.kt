package mycorda.app.tasks.httpServer

import org.http4k.core.Response
import org.http4k.core.Status


fun Response.Companion.text(text: String): Response {
    return Response(Status.OK).text(text)
}

fun Response.text(text: String): Response {
    return this
        .body(text)
        .header("Content-Type", "text/plain; charset=utf-8")
}