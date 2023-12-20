package org.jetbrains.compose.resources

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.intl.Locale

internal data class ResourceEnvironment(
    val language: LanguageQualifier,
    val region: RegionQualifier,
    val theme: ThemeQualifier,
    val density: DensityQualifier
)

@Composable
internal fun rememberEnvironment(): ResourceEnvironment {
    val composeLocale = Locale.current
    val composeTheme = isSystemInDarkTheme()
    val composeDensity = LocalDensity.current

    //cache ResourceEnvironment unless compose environment is changed
    return remember(composeLocale, composeTheme, composeDensity) {
        ResourceEnvironment(
            LanguageQualifier(composeLocale.language),
            RegionQualifier(composeLocale.region),
            ThemeQualifier.selectByValue(composeTheme),
            DensityQualifier.selectByDensity(composeDensity.density)
        )
    }
}

/**
 * Provides the resource environment for non-composable access to string resources.
 * It is an expensive operation! Don't use it in composable functions with no cache!
 */
internal expect fun getResourceEnvironment(): ResourceEnvironment

internal fun Resource.getPathByEnvironment(environment: ResourceEnvironment): String {
    //Priority of environments: https://developer.android.com/guide/topics/resources/providing-resources#table2
    items.toList()
        .filterBy(environment.language)
        .also { if (it.size == 1) return it.first().path }
        .filterBy(environment.region)
        .also { if (it.size == 1) return it.first().path }
        .filterBy(environment.theme)
        .also { if (it.size == 1) return it.first().path }
        .filterBy(environment.density)
        .also { if (it.size == 1) return it.first().path }
        .let { items ->
            if (items.isEmpty()) {
                error("Resource with ID='$id' not found")
            } else {
                error("Resource with ID='$id' has more than one file: ${items.joinToString { it.path }}")
            }
        }
}

private fun List<ResourceItem>.filterBy(qualifier: Qualifier): List<ResourceItem> {
    //Android has a slightly different algorithm,
    //but it provides the same result: https://developer.android.com/guide/topics/resources/providing-resources#BestMatch

    //filter items with the requested qualifier
    val withQualifier = filter { item ->
        item.qualifiers.any { it == qualifier }
    }

    if (withQualifier.isNotEmpty()) return withQualifier

    //items with no requested qualifier type (default)
    return filter { item ->
        item.qualifiers.none { it::class == qualifier::class }
    }
}