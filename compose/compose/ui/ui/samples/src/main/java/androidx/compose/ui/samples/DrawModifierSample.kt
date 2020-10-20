/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.ui.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ContentDrawScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradient
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.Path
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.unit.dp

/**
 * Sample showing how to leverage [Modifier.drawWithCache] in order
 * to cache contents in between draw calls that depend on sizing information.
 * In the example below, the LinearGradient is created once and re-used across
 * calls to onDraw. If the size of the drawing area changes, then the
 * LinearGradient is re-created with the updated width and height.
 */
@Sampled
@Composable
fun DrawWithCacheModifierSample() {
    Box(
        Modifier.drawWithCache {
            val gradient = LinearGradient(
                startX = 0.0f,
                startY = 0.0f,
                endX = size.width,
                endY = size.height,
                colors = listOf(Color.Red, Color.Blue)
            )
            onDraw {
                drawRect(gradient)
            }
        }
    )
}

/**
 * Sample showing how to leverage [Modifier.drawWithCache] to persist data across
 * draw calls. In the example below, the [LinearGradient] will be re-created if either
 * the size of the drawing area changes, or the toggle flag represented by a mutable state
 * object changes. Otherwise the same [LinearGradient] instance is re-used for each call
 * to drawRect.
 */
@Sampled
@Composable
fun DrawWithCacheModifierStateParameterSample() {
    val colors1 = listOf(Color.Red, Color.Blue)
    val colors2 = listOf(Color.Yellow, Color.Green)
    var toggle by remember { mutableStateOf(true) }
    Box(
        Modifier.clickable { toggle = !toggle }.drawWithCache {
            val gradient = LinearGradient(
                startX = 0.0f,
                startY = 0.0f,
                endX = size.width,
                endY = size.height,
                colors = if (toggle) colors1 else colors2
            )
            onDraw {
                drawRect(gradient)
            }
        }
    )
}

/**
 * Sample showing how to leverage [Modifier.drawWithCache] to cache a LinearGradient
 * if the size is unchanged. Additionally this sample illustrates how to re-arrange
 * drawing order using [ContentDrawScope.drawContent] in order to draw the desired
 * content first to support blending against the sample vector graphic of a triangle
 */
@Sampled
@Composable
fun DrawWithCacheContentSample() {
    val vectorPainter = VectorPainter(24.dp, 24.dp) { viewportWidth, viewportHeight ->
        Path(
            pathData = PathData {
                lineTo(viewportWidth, 0f)
                lineTo(0f, viewportHeight)
                close()
            },
            fill = SolidColor(Color.Black)
        )
    }
    Image(
        painter = vectorPainter,
        modifier = Modifier.size(120.dp).drawWithCache {
            val gradient = LinearGradient(
                colors = listOf(Color.Red, Color.Blue),
                startX = 0f,
                startY = 0f,
                endX = 0f,
                endY = size.height
            )
            onDrawWithContent {
                drawContent()
                drawRect(gradient, blendMode = BlendMode.Plus)
            }
        }
    )
}