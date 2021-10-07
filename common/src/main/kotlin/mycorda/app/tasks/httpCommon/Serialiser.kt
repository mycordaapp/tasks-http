package mycorda.app.tasks.httpCommon

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import mycorda.app.rss.JsonSerialiser
import mycorda.app.rss.SerialisationPacket

class Serialiser {
    private val mapper: ObjectMapper = ObjectMapper()
    private val rss = JsonSerialiser()

    init {
        val module = KotlinModule()
        mapper.registerModule(module)
    }

    fun serialiseData(data: Any): String {
        return rss.serialiseData(data)
    }

    fun deserialiseData(serialised: String): SerialisationPacket {
        return rss.deserialiseData(serialised)
    }


    fun serialiseBlockingTaskRequest(model: BlockingTaskRequest): String {
        return mapper.writeValueAsString(model)
    }

    fun deserialiseBlockingTaskRequest(json: String): BlockingTaskRequest {
        return mapper.readValue(json, BlockingTaskRequest::class.java)
    }
}

data class BlockingTaskRequest(
    val task: String,
    val inputSerialized: String)