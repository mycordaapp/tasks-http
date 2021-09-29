package mycorda.app.tasks.common

data class BlockingTaskRequest(
    val task: String,
    val input: String,
    val inputClazz: String,
    val outputClazz: String
)
