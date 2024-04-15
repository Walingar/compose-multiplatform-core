@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package my.lib.res

import kotlin.OptIn
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.PluralStringResource

@ExperimentalResourceApi
private object CommonMainPlurals0 {
    public val numberOfSongsAvailable: PluralStringResource by
    lazy { init_numberOfSongsAvailable() }
}

@ExperimentalResourceApi
public val Res.plurals.numberOfSongsAvailable: PluralStringResource
    get() = CommonMainPlurals0.numberOfSongsAvailable

@ExperimentalResourceApi
private fun init_numberOfSongsAvailable(): PluralStringResource =
    org.jetbrains.compose.resources.PluralStringResource(
        "plurals:numberOfSongsAvailable", "numberOfSongsAvailable",
        setOf(
            org.jetbrains.compose.resources.ResourceItem(setOf(), "values/strings.commonMain.cvr", 10,
                124),
        )
    )