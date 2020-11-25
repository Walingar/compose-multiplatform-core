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

package androidx.compose.foundation.lazy

import androidx.compose.foundation.assertNotNestingScrollableContainers
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.gesture.scrollorientationlocking.Orientation
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.AmbientLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.util.fastForEach

@Composable
internal fun LazyList(
    itemsCount: Int,
    modifier: Modifier = Modifier,
    state: LazyListState,
    contentPadding: PaddingValues,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    isVertical: Boolean,
    itemContentFactory: LazyItemScope.(Int) -> @Composable () -> Unit
) {
    val reverseDirection = AmbientLayoutDirection.current == LayoutDirection.Rtl && !isVertical

    val cachingItemContentFactory = remember { CachingItemContentFactory(itemContentFactory) }
    cachingItemContentFactory.itemContentFactory = itemContentFactory

    val startContentPadding = if (isVertical) contentPadding.top else contentPadding.start
    val endContentPadding = if (isVertical) contentPadding.bottom else contentPadding.end
    SubcomposeLayout(
        modifier
            .scrollable(
                orientation = if (isVertical) Orientation.Vertical else Orientation.Horizontal,
                // reverse scroll by default, to have "natural" gesture that goes reversed to layout
                reverseDirection = !reverseDirection,
                controller = state.scrollableController
            )
            .clipToBounds()
            .padding(contentPadding)
            .then(state.remeasurementModifier)
    ) { constraints ->
        constraints.assertNotNestingScrollableContainers(isVertical)

        // this will update the scope object if the constrains have been changed
        cachingItemContentFactory.updateItemScope(this, constraints)

        val startContentPaddingPx = startContentPadding.toIntPx()
        val endContentPaddingPx = endContentPadding.toIntPx()
        val mainAxisMaxSize = (if (isVertical) constraints.maxHeight else constraints.maxWidth)

        val itemProvider = LazyMeasuredItemProvider(
            constraints,
            isVertical,
            this,
            cachingItemContentFactory
        ) { placeables ->
            LazyMeasuredItem(
                placeables = placeables,
                isVertical = isVertical,
                horizontalAlignment = horizontalAlignment,
                verticalAlignment = verticalAlignment,
                layoutDirection = layoutDirection,
                startContentPadding = startContentPaddingPx,
                endContentPadding = endContentPaddingPx
            )
        }

        val measureResult = state.measure(
            itemsCount,
            itemProvider,
            mainAxisMaxSize,
            startContentPaddingPx,
            endContentPaddingPx,
            state.firstVisibleItemIndexNonObservable,
            state.firstVisibleItemScrollOffsetNonObservable,
            state.scrollToBeConsumed
        )

        state.applyMeasureResult(measureResult)

        val layoutWidth = constraints.constrainWidth(
            if (isVertical) measureResult.crossAxisSize else measureResult.mainAxisSize
        )
        val layoutHeight = constraints.constrainHeight(
            if (isVertical) measureResult.mainAxisSize else measureResult.crossAxisSize
        )
        layout(layoutWidth, layoutHeight) {
            var currentMainAxis = measureResult.itemsScrollOffset
            measureResult.items.fastForEach {
                it.place(this, layoutWidth, layoutHeight, currentMainAxis)
                currentMainAxis += it.mainAxisSize
            }
        }
    }
}
