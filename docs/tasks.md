# Tasks Http

## Introduction

An Http Client / Server for executing tasks on a remote server 
## The Hello World task.

Every time a `Task` is run, it is allocated a unique id, the `taskId()`. This is used in logging and events. Strictly
speaking it is a taskInstanceId, but taskId is shorter

```kotlin
interface Task {
    //a unique ID created for each run of the Task
    fun taskId(): UUID
}
```

There are few "flavours" of `Tasks`. The simplest is a `BlockingTask`

```kotlin
interface BlockingTask<in I, out O> : Task {
    fun exec(ctx: ExecutionContext = SimpleExeccutionContext(), params: I): O
}
```

`ExecutionContext` is explained in more detail later. All we need to know to understand a Task is that it injects
information that is only known at execution time, such as the connection to a stdout console, as well as providing easy
access to common services.

All `Tasks` follow a simple Request / Response pattern with a single class for each input (Request) and output (
Response). This is important as:

* for higher level services it allows for a lot of generalisations
* it works very well with generics in Kotlin.

Tasks have three key characteristics:

* do one thing well
* provide stable and unambiguous inputs and outputs to their clients
* conform to the rules
  in [Really Simple Serializer](https://github.com/mycordaapp/really-simple-serialization/blob/master/README.md)

## Blocking Tasks

The most basic form is a `BlockingTask`. A simple example is below:

```kotlin
class CalcSquareTask : BaseBlockingTask<Int, Int>() {

    override fun exec(ctx: ExecutionContext, params: Int): Int {
        // this is normally the first line - it ensures the task is stored in the context
        val ctx = ctx.withTaskId(this)

        ctx.log("Calculating square of $params")
        return params.times(params)
    }
}
```

This doesn't really do anything useful, but it shows the basic programming model in which everything needed is available
via the `ExeccutionContext`.

## Executing a Task

A `Task` can be created and executed in three ways

* by instantiating an instance of the Task and calling the `exec` method directly.
* by using the `TaskFactory`. Usually this is just part of the implementation of the `TaskClient` below
* by creating a `TaskClient` and calling the `exec` method on the client. This is the preferred pattern as it:
    - allows for remoting (calling `Tasks` on a remote agent)
    - controls setup of the `ExecutionContext`

`TaskDocExamples.kt` has full source code for the examples below.

### Executing a Blocking Task directly

The `SimpleExeccutionContext` is lightweight and suitable for testing and simple use cases

```kotlin
@Test
fun `should call task directly`() {
    val task = CalcSquareTask()
    val ctx = SimpleExeccutionContext()
    val result = task.exec(ctx, 10)
    assertThat(result, equalTo(100))
}
```

### Executing a Blocking Task via the `TaskFactory`

This is usually part of the server side. It allows for dynamic registration of tasks

```kotlin
@Test
fun `should call task via the TaskFactory`() {
    // register a real task
    val liveFactory = TaskFactory()
    liveFactory.register(ListDirectoryTaskImpl::class, ListDirectoryTask::class)

    // create by class
    val taskByClass = liveFactory.createInstance(ListDirectoryTask::class)
    val ctx = SimpleExeccutionContext()
    assert(taskByClass.exec(ctx, ".").contains("build.gradle"))

    // create by name
    val taskByName =
        liveFactory.createInstance("mycorda.app.tasks.ListDirectoryTask") as BlockingTask<String, List<String>>
    assert(taskByName.exec(ctx, ".").contains("build.gradle"))

    // register and create a fake task
    val fakeFactory = TaskFactory()
    fakeFactory.register(ListDirectoryTaskFake::class, ListDirectoryTask::class)
    val fakeTask = fakeFactory.createInstance(ListDirectoryTask::class)
    assert(fakeTask.exec(ctx, ".").contains("fake.txt"))
}
```

### Executing a Blocking Task via the `TaskClient`

This is the typical case as it reflects the usual client server model, with tasks invoked locally but running on another
agent of some form on another server. Note that for simplicity, in this example we create the SimpleTaskClient which
uses in memory communication.

```kotlin
@Test
fun `should call task via a task client`() {
    // 1. register a real task in the TaskFactory (server side)
    val taskFactory = TaskFactory()
    taskFactory.register(ListDirectoryTaskFake::class, ListDirectoryTask::class)
    val registry = Registry().store(taskFactory)

    // 2. get a task client (client side)
    val taskClient = SimpleTaskClient(registry)

    // 3. call the client
    val clientContext = SimpleClientContext()
    val result = taskClient.execBlocking<String, List<String>>(
        clientContext,
        "mycorda.app.tasks.ListDirectoryTask", "."
    )

    // 4. assert results
    assert(result.contains("fake.txt"))

    // 5. assert logging output
    assertThat(
        clientContext.inMemoryLoggingConsumerContext().stdout(),
        equalTo(
            "ListDirectoryTask:\n" +
                    "   params: .\n"
        )
    )
    assert(
        clientContext.inMemoryLoggingConsumerContext().messages()
            .hasMessage(LogLevel.INFO, "listing directory '.'")
    )
}
```

The example also shows another important characteristic of the design. Logging output from the Task (structured log
messages and stdout/stderr) are made available to the client, which can help greatly in debugging and fault detection.
The implementing task controls what is sent. Looking at the code for `ListDirectoryTaskFake` we can see this clearly.

This behaviour is in addition to any distributed logging that may be enabled, which is described later.

```kotlin
class ListDirectoryTaskFake : ListDirectoryTask, BaseBlockingTask<String, List<String>>() {
    override fun exec(executionContext: ExecutionContext, params: String): List<String> {
        val out = executionContext.stdout()
        out.println("ListDirectoryTask:")
        out.println("   params: $params")
        executionContext.log(LogMessage.info("listing directory '$params'"))
        return listOf("fake.txt")
    }
}
```

## Async Tasks

In most cases to build a robust application Async Tasks are needed. These manage three problems in distributed systems:

* It is usually unsafe to assume a Task will execute and respond quickly. Network latency, system failovers and the
  general inconsistency of timings on cloud platforms have to be considered.
* Both the client and server may restart while running a task
* It is safer to be pessimistic, and assume that any message between a client and server can be lost.

The Async Task version of CalcSquareTask is below.

```kotlin
class CalcSquareAsyncTask(registry: Registry) : AsyncTask<Int, Int> {
    private val resultChannelFactory = registry.get(AsyncResultChannelSinkFactory::class.java)
    private val taskId = UUID.randomUUID()
    override fun exec(
        ctx: ExecutionContext,
        channelLocator: AsyncResultChannelSinkLocator,
        channelId: UniqueId,
        input: Int
    ) {
        // 1. Find the channel
        val resultChannel = resultChannelFactory.create(channelLocator)

        ctx.executorService().submit<Unit> {
            // 2. Generate a result
            val result = AsyncResultChannelMessage(channelId, Success(input * input), Int::class.java)

            // 3. Simulate a delay 
            Thread.sleep(AsyncTask.platformTick())

            // 4. Write the result
            resultChannel.accept(result)
        }
    }
}

```
Breaking this code down:



and the basic test case is 

```kotlin
 fun `should call task directly`() {
    // 1. Setup a result sink,
    val resultSinkFactory = DefaultAsyncResultChannelSinkFactory()
    val reg = Registry().store(resultSinkFactory)

    // 2. setup a channel for the results
    val locator = AsyncResultChannelSinkLocator.LOCAL
    val channelId = UniqueId.random()

    // 3. setup the task
    val task = CalcSquareAsyncTask(reg)
    val ctx = SimpleExecutionContext()

    // 4. call the task. The result will come back on the results channel
    task.exec(ctx, locator, channelId, 10)

    // 5. run a query over the result channel. In this
    //    CalcSquareAsyncTask is a demo and has returned a result immediately
    val query = resultSinkFactory.channelQuery(locator)

    // 6. assert expected results
    // not yet read
    assertThat(query.hasResult(channelId), equalTo(false))
    // wait long enough
    Thread.sleep(AsyncTask.platformTick() * 2)
    // now ready
    assert(query.hasResult(channelId))
    val result = query.result<Int>(channelId)
    assertThat(result, equalTo(Success<Int>(100) as AsyncResult<Int>))
}

```

### #2 - Combining Tasks

The second principle is that is easy to call and combine tasks, even if the implementation is running on another server.
To support this there is prebuilt support for common problems including:

* creation of fully wired ExecutionContext
* serialisation of input and outputs
* 'executors' to encapsulate common patterns like logging, exceptions handling and retries
* building and securing servers running tasks

### #3 - Provisioners and Templates

The third principle is of Provisioners and Templates. Typically users will want to perform higher actions, for example
deploy a Corda Node to AWS with their apps pre-installed, without having to understand all the Tasks needed to
accomplish that goal. Provisioners simply take a predefined template (currently YAML or JSON) and use these rules to
drive calling the correct Tasks with required params. Written like this, it sounds simple. But in practice there must be
a fairly consistent set of rules that are followed when implementing a provisoner.

Provisioners can be constructed from prebuilt stages that define  
common steps. As an example, think of deploying a corda node that connects to Testnet to either Azure or AWS. Obviously
the steps that provision the VM on the cloud are different, but once past this stage the steps to configure the Corda
node (setting X500 name, overriding default settings, installing corDapps, joining Testnet, setting up unix systemctl to
run Corda, staring Corda, ... )
are the same.

### #4 - Testing is built in

The final principle is testability. Mock/Fake implementation of Tasks must be provided for use in the test suites.

*todo - expand this principle*

## Implementation

### Creating new Tasks

See [Create Instance](../aws-tasks/src/main/kotlin/net/corda/ccl/aws/task/CreateInstanceTask.kt) for an example.

All tasks share a
common [Execution Context](../commons/src/main/kotlin/net/corda/ccl/commons/executionContext/Context.kt).

Wiring everything up for an execution context can be simplified with the  `ContextManager` class - here is
an [example](../corda-tasks/src/main/kotlin/net/corda/ccl/tasks/cenm/identitymanager/ReceivePKITask.kt)

The input and output are always a single value. This may be a simple scalar, but in more complicated cases a class will
be required. Sometimes there is already a suitable domain class to use, but if not the standard pattern is to create
additional Kotlin data classes with the suffix `Params` and `Result`
in the same source file as the Task. For example:

```kotlin
data class ComplexCalculationParams(val p1: Int, val p2: Int, val p3: Int)
class ComplexCalculationTask : BlockingTask<ComplexCalculationParams, Int> {
    // implementation goes here 
}
```

Note that as custom serializers are not supported, be careful when reusing existing domain model - they may rely upon
external serializers. 
 