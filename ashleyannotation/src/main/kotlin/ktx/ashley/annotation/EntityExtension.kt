package ktx.ashley.annotation

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class EntityExtension(
        val name: String = ""
)