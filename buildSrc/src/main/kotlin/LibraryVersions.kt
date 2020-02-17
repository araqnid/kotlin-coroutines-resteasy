import kotlin.reflect.full.memberProperties

object LibraryVersions {
    const val jetty = "9.4.26.v20200117"
    const val resteasy = "3.1.4.Final"
    const val kotlinCoroutines = "1.3.3"
    const val slf4j = "1.7.30"

    fun toMap() =
            LibraryVersions::class.memberProperties
                    .associate { prop -> prop.name to prop.getter.call() as String }
}
