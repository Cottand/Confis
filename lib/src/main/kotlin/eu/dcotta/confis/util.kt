package eu.dcotta.confis.dsl

import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

fun <C, V> oneTimeProperty(instantiate: (prop: KProperty<*>) -> V) =
    PropertyDelegateProvider<C, ReadOnlyProperty<C, V>> { _, prop ->
        val v = instantiate(prop)
        ReadOnlyProperty { _, _ -> v }
    }