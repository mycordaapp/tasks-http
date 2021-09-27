package mycorda.app.tasks.httpServer

import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK

class Http4K
{

    val httpHandler: HttpHandler = { request: Request ->
        Response(OK).body("Hello 🌍")
    }
}