package io.github.kmbisset89.azurekeystore.plugin.logic

import java.util.Properties

internal class PropertyResolver(private val localProps: Properties?, private val projectProps: Map<String, *>) {

    fun resolveProperty(key: String): String? {
        return localProps?.getProperty(key) ?: projectProps[key] as? String
    }

    fun resolveProperty(key: String, defaultValue: String): String {
        return localProps?.getProperty(key) ?: projectProps[key] as String? ?: defaultValue
    }
}
