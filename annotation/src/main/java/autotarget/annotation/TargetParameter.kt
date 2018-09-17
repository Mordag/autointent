package autotarget.annotation

import kotlin.reflect.KClass

@Repeatable
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class TargetParameter(val key: String,
                                 val name: String = "unspecified",
                                 val type: KClass<*>,
                                 val optional: Boolean = false)
