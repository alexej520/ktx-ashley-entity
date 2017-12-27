Entity extension properties generation based on Component
# Usage
1) Annotate the components for which you want to have extension properties
```kotlin
package com.company.example

import com.badlogic.ashley.core.Component
import ktx.ashley.annotation.EntityExtension

@EntityExtension
class VelocityComponent: Component {
  var x: Float = 0f
  var y: Float = 0f
}

@EntityExtension("tfm")
class TransformComponent: Component {
  var x: Float = 0f
  var y: Float = 0f
}
```
2) Rebuild project. It will generate file EntityExtensions.kt
```kotlin
package com.company.example

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity

object Mapper {
  val velocity: ComponentMapper<VelocityComponent> = ComponentMapper.getFor(VelocityComponent::class.java)

  val tfm: ComponentMapper<TransformComponent> = ComponentMapper.getFor(TransformComponent::class.java)
}

var Entity.velocity: VelocityComponent?
  inline get() = Mapper.visual.get(this)
  inline set(value) {
    if (value == null){
      remove(VelocityComponent::class.java)
    } else {
      add(value)
    }
  }

var Entity.tfm: TransformComponent?
  inline get() = Mapper.visual.get(this)
  inline set(value) {
    if (value == null){
      remove(TransformComponent::class.java)
    } else {
      add(value)
    }
  }
``` 
3) Use extension properties and mappers
```kotlin
import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.company.example.*

fun processEntity(entity: Entity){
  entity.velocity?.let { v ->
    entyty.get(Mapper.tfm)!!.apply {
      x += v.x
      y += v.y
    }
    entity.velocity = null
  }
}
```
# Dependencies
```groovy
repositories {
    maven { url 'https://jitpack.io' }
}


dependencies {
    compile 'com.github.alexej520.ktx-ashley-entity:ashleyannotation:1.0'
    kapt 'com.github.alexej520.ktx-ashley-entity:ashleyprocessor:1.0'
}
```
