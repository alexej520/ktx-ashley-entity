package ktx.ashley.processor

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import ktx.ashley.annotation.EntityExtension
import java.io.File
import javax.annotation.Generated
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.PackageElement
import javax.lang.model.element.TypeElement

@AutoService(Processor::class)
class KtxAshleyProcessor : AbstractProcessor() {
    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(EntityExtension::class.java.name)
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latest()
    }

    class ExtensionSpec(val annotation: EntityExtension, val component: Element)

    override fun process(set: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        println("process")
        val map = mutableMapOf<PackageElement, MutableList<ExtensionSpec>>()
        roundEnv.getElementsAnnotatedWith(EntityExtension::class.java)
            .forEach {
                val componentType = processingEnv.elementUtils
                    .getTypeElement(Component::class.qualifiedName)
                    .asType()
                if (!processingEnv.typeUtils.isAssignable(it.asType(), componentType)) {
                    throw Exception("${it.simpleName} must implement Component")
                }
                val annotation = it.getAnnotation(EntityExtension::class.java)!!
                map.getOrPut(processingEnv.elementUtils.getPackageOf(it)) { mutableListOf() } += ExtensionSpec(
                    annotation,
                    it
                )
            }
        return generateExtension(map)
    }

    val reg = Regex("Component$")

    private fun generateExtension(map: Map<PackageElement, List<ExtensionSpec>>): Boolean {
        map.forEach { (pack, specList) ->
            val file = FileSpec.builder(pack.qualifiedName.toString(), "EntityExtensions").apply {
                addAnnotation(GENERATED)
                val mappers = TypeSpec.objectBuilder("Mapper")
                val extensions = mutableListOf<PropertySpec>()
                specList.forEach { spec ->
                    val customName = spec.annotation.name
                    val componentClassName = spec.component.simpleName

                    val propName = if (customName.isNotEmpty()) {
                        customName
                    } else {
                        with(componentClassName.replace(reg, "")) {
                            substring(0, 1).toLowerCase() + substring(1)
                        }
                    }

                    val propTypeName = spec.component.asType().asTypeName()
                    val mapperTypeName =
                        ComponentMapper::class.asTypeName().parameterizedBy(propTypeName)

                    mappers.addProperty(
                        PropertySpec.builder(propName, mapperTypeName)
                            .initializer("ComponentMapper.getFor($componentClassName::class.java)")
                            .mutable(false)
                            .build()
                    )

                    extensions += PropertySpec.builder(propName, propTypeName.copy(nullable = true))
                        .mutable(true)
                        .receiver(Entity::class)
                        .getter(
                            FunSpec.getterBuilder()
                                .addModifiers(KModifier.INLINE)
                                .addStatement("return Mapper.$propName.get(this)")
                                .build()
                        )
                        .setter(
                            FunSpec.setterBuilder()
                                .addModifiers(KModifier.INLINE)
                                .addParameter("value", propTypeName)
                                .addCode(
                                    """
                                        if (value == null){
                                        remove($componentClassName::class.java)
                                        } else {
                                        add(value)
                                        }
                                    """.trimIndent()
                                )
                                .build()
                        )
                        .build()
                }
                addType(mappers.build())
                extensions.forEach {
                    addProperty(it)
                }
            }.build().writeTo(File(processingEnv.options["kapt.kotlin.generated"]))
        }
        return true
    }

    @Generated()
    companion object {
        val GENERATED = AnnotationSpec.builder(Generated::class)
            .addMember("value = [%S]", "ktx.ashley.processor.KtxAshleyProcessor")
            .build()
    }
}