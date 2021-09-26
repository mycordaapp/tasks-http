package mycorda.app.tasks.common

import mycorda.app.tasks.NotRequired
import mycorda.app.tasks.Task
import java.io.File
import java.net.URL
import java.util.*
import java.util.concurrent.Future
import kotlin.reflect.KClass
import kotlin.reflect.full.functions
import kotlin.reflect.full.isSubclassOf

/**
 * Examine a Task via reflections to extract meta data to drive other
 * layers such as JSON mappings
 */
class TaskReflections() {

    companion object {
        fun isScalar(clazz: KClass<*>): Boolean {
            return (clazz == Int::class)
                    || (clazz == Long::class)
                    || (clazz == Double::class)
                    || (clazz == String::class)
                    || (clazz == Float::class)
                    || (clazz == Boolean::class)
                    || (clazz == String::class)
                    || (clazz == File::class)
                    || (clazz == UUID::class)
                    || (clazz == URL::class)
        }

        fun isEnum(type: KClass<out Any>) = type.isSubclassOf(Enum::class)


        fun isUnit(clazz: KClass<*>): Boolean {
            return (clazz == Unit::class)
                    || (clazz == Nothing::class)
        }

        fun isNotRequired(clazz: KClass<*>): Boolean {
            return (clazz == NotRequired::class)
        }

        fun isFuture(clazz: KClass<*>): Boolean {
            return (clazz == Future::class)

        }
    }

//    fun paramClass(): KClass<out Any> {
//        val execMethod = t.functions.single { it.name == "exec" }
//        val type = execMethod.parameters[2].type
//        return type.classifier as KClass<Any>
//    }
//
//    fun resultClass(): KClass<out Any> {
//        val execMethod = t.functions.single { it.name == "exec" }
//        return execMethod.returnType.classifier as KClass<Any>
//    }
//
//    fun isParamOptional(): Boolean {
//        val execMethod = t.functions.single { it.name == "exec" }
//        return (execMethod.parameters[2].type.isMarkedNullable)
//    }

//    fun isScalar(clazz: KClass<*>): Boolean {
//        return TaskReflections.isScalar(clazz)
//    }
//
//    fun isUnit(clazz: KClass<*>): Boolean {
//        return TaskReflections.isUnit(clazz)
//    }
//
//    fun isFuture(clazz: KClass<*>): Boolean {
//        return TaskReflections.isFuture(clazz)
//    }


}