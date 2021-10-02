package mycorda.app.tasks.httpServer

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import mycorda.app.tasks.common.JsonSerializer
import java.util.*
import org.junit.jupiter.api.Test


class JsonSerializerTests {
    private val serializer = JsonSerializer()

    @Test
    fun `should serialize scalars`() {
        assertThat(serializer.serializeData(123), equalTo("123"))
        assertThat(serializer.serializeData("aString"), equalTo("aString"))
        assertThat(serializer.serializeData(UUID(1, 2)), equalTo("00000000-0000-0001-0000-000000000002"))
    }

    @Test
    fun `should derserialize scalars`() {

       // serializer.deserializeResult("123")
       // assertThat(serializer.deserializeResult("123"), equalTo(123))
      //  assertThat(serializer.serializeResult("aString"), equalTo("aString"))
      //  assertThat(serializer.serializeResult(UUID(1, 2)), equalTo("00000000-0000-0001-0000-000000000002"))
    }


}

data class Test(val data: String)