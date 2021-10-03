package mycorda.app.tasks.httpServer

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import mycorda.app.tasks.serialisation.JsonSerialiser
import java.util.*
import org.junit.jupiter.api.Test


class JsonSerializerTests {
    private val serializer = JsonSerialiser()

    @Test
    fun `should serialize scalars`() {
        assertThat(serializer.serialiseData(123), equalTo("123"))
        assertThat(serializer.serialiseData("aString"), equalTo("aString"))
        assertThat(serializer.serialiseData(UUID(1, 2)), equalTo("00000000-0000-0001-0000-000000000002"))
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