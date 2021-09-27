package mycorda.app.tasks.httpClient

import SimpleClientContext
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test


class HttpTaskClientTests {

    @Test
    fun `should do something`() {
        var x = 2
        assertThat(x+1, equalTo(3))
        assert(x == 2)
    }


    @Test
    fun `should call blocking task`() {
        val client = HttpTaskClient("http://dont-care")
        val ctx = SimpleClientContext()

        //val result = client.execBlocking<Int,Int>(ctx, "com.example.CalcSquareTask", 10)
    }

}