package mycorda.app.tasks.http

import mycorda.app.tasks.BlockingTask
import mycorda.app.tasks.demo.CalcSquareTask


fun main(args: Array<String>) {
    val t: BlockingTask<Int, Int> = CalcSquareTask()
    val result = t.exec(input = 10)

    println("hi $result")
}

class TheApp(port : Int = 12345){

}