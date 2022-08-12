/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.foundation.lazy.staggeredgrid

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.checkScrollableContainerConstraints
import androidx.compose.foundation.fastFold
import androidx.compose.foundation.fastMaxOfOrNull
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasureScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.util.fastForEach
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

@Composable
@ExperimentalFoundationApi
internal fun rememberStaggeredGridMeasurePolicy(
    state: LazyStaggeredGridState,
    itemProvider: LazyLayoutItemProvider,
    contentPadding: PaddingValues,
    reverseLayout: Boolean,
    orientation: Orientation,
    verticalArrangement: Arrangement.Vertical,
    horizontalArrangement: Arrangement.Horizontal,
    slotSizesSums: Density.(Constraints) -> IntArray,
    overscrollEffect: OverscrollEffect
): LazyLayoutMeasureScope.(Constraints) -> LazyStaggeredGridMeasureResult = remember(
    state,
    itemProvider,
    contentPadding,
    reverseLayout,
    orientation,
    verticalArrangement,
    horizontalArrangement,
    slotSizesSums,
    overscrollEffect,
) {
    { constraints ->
        checkScrollableContainerConstraints(
            constraints,
            orientation
        )
        val isVertical = orientation == Orientation.Vertical

        val resolvedSlotSums = slotSizesSums(this, constraints)
        val itemCount = itemProvider.itemCount

        val mainAxisAvailableSize =
            if (isVertical) constraints.maxHeight else constraints.maxWidth

        val measuredItemProvider = LazyStaggeredGridMeasureProvider(
            isVertical,
            itemProvider,
            this,
            resolvedSlotSums
        ) { index, key, placeables ->
            LazyStaggeredGridMeasuredItem(
                index,
                key,
                placeables,
                isVertical
            )
        }

        val beforeContentPadding = 0
        val afterContentPadding = 0

        var initialItemIndices: IntArray
        var initialItemOffsets: IntArray

        Snapshot.withoutReadObservation {
            initialItemIndices =
                if (state.firstVisibleItems.size == resolvedSlotSums.size) {
                    state.firstVisibleItems
                } else {
                    IntArray(resolvedSlotSums.size) { -1 }
                }
            initialItemOffsets =
                if (state.firstVisibleItemScrollOffsets.size == resolvedSlotSums.size) {
                    state.firstVisibleItemScrollOffsets
                } else {
                    IntArray(resolvedSlotSums.size) { 0 }
                }
        }

        val spans = state.spans
        val firstItemIndices = initialItemIndices.copyOf()
        val firstItemOffsets = initialItemOffsets.copyOf()

        // Measure items

        if (itemCount <= 0 || resolvedSlotSums.isEmpty()) {
            LazyStaggeredGridMeasureResult(
                firstVisibleItemIndices = IntArray(0),
                firstVisibleItemScrollOffsets = IntArray(0),
                consumedScroll = 0f,
                measureResult = layout(constraints.minWidth, constraints.minHeight) {},
                canScrollForward = false,
                canScrollBackward = false,
                visibleItemsInfo = emptyArray()
            )
        } else {
            // todo(b/182882362): content padding

            // represents the real amount of scroll we applied as a result of this measure pass.
            var scrollDelta = state.scrollToBeConsumed.roundToInt()

            // applying the whole requested scroll offset. we will figure out if we can't consume
            // all of it later
            firstItemOffsets.offsetBy(-scrollDelta)

            // if the current scroll offset is less than minimally possible
            if (firstItemIndices[0] == 0 && firstItemOffsets[0] < 0) {
                scrollDelta += firstItemOffsets[0]
                firstItemOffsets.fill(0)
            }

            // this will contain all the MeasuredItems representing the visible items
            val measuredItems = Array(resolvedSlotSums.size) {
                mutableListOf<LazyStaggeredGridMeasuredItem>()
            }

            // include the start padding so we compose items in the padding area. before starting
            // scrolling forward we would remove it back
            firstItemOffsets.offsetBy(-beforeContentPadding)

            // define min and max offsets (min offset currently includes beforeContentPadding)
            val minOffset = -beforeContentPadding
            val maxOffset = mainAxisAvailableSize

            fun hasSpaceOnTop(): Boolean {
                for (lane in firstItemIndices.indices) {
                    val itemIndex = firstItemIndices[lane]
                    val itemOffset = firstItemOffsets[lane]

                    if (itemOffset <= 0 && itemIndex > 0) {
                        return true
                    }
                }

                return false
            }

            // we had scrolled backward or we compose items in the start padding area, which means
            // items before current firstItemScrollOffset should be visible. compose them and update
            // firstItemScrollOffset
            while (hasSpaceOnTop()) {
                val laneIndex = firstItemOffsets.indexOfMinValue()
                val previousItemIndex = spans.findPreviousItemIndex(
                    item = firstItemIndices[laneIndex],
                    lane = laneIndex
                )

                if (previousItemIndex < 0) {
                    // If we scrolled past the first item in the lane, we have a point of reference
                    // to re-align items.
                    // Case 1: Item offsets for first item are not aligned
                    val misalignedOffsets = firstItemOffsets.any {
                        it != firstItemOffsets[laneIndex]
                    }
                    // Case 2: Other lanes have more items than the current one
                    val moreItemsInOtherLanes = firstItemIndices.indices.any { lane ->
                        spans.findPreviousItemIndex(firstItemIndices[lane], lane) != -1
                    }
                    // Case 3: the first item is in the wrong lane (it should always be in
                    // the first one)
                    val firstItemInWrongLane = spans.getSpan(0) != 0
                    // If items are not aligned, reset all measurement data we gathered before and
                    // proceed with initial measure
                    if (misalignedOffsets || moreItemsInOtherLanes || firstItemInWrongLane) {
                        initialItemIndices = IntArray(firstItemIndices.size) { -1 }
                        initialItemOffsets = IntArray(firstItemOffsets.size) {
                            initialItemOffsets[laneIndex]
                        }
                        firstItemIndices.fill(-1)
                        firstItemOffsets.fill(initialItemOffsets[laneIndex] - scrollDelta)
                        measuredItems.forEach { it.clear() }
                    }
                    break
                }

                if (spans.getSpan(previousItemIndex) == SpanLookup.SpanUnset) {
                    spans.setSpan(previousItemIndex, laneIndex)
                }

                val measuredItem = measuredItemProvider.getAndMeasure(
                    previousItemIndex,
                    laneIndex
                )
                measuredItems[laneIndex].add(0, measuredItem)

                firstItemIndices[laneIndex] = previousItemIndex
                firstItemOffsets[laneIndex] += measuredItem.sizeWithSpacings
            }

            // if we were scrolled backward, but there were not enough items before. this means
            // not the whole scroll was consumed
            if (firstItemOffsets[0] < minOffset) {
                scrollDelta += firstItemOffsets[0]
                firstItemOffsets.offsetBy(minOffset - firstItemOffsets[0])
            }

            val currentItemIndices = initialItemIndices.copyOf()
            val currentItemOffsets = IntArray(initialItemOffsets.size) {
                -(initialItemOffsets[it] - scrollDelta)
            }

            // neutralize previously added start padding as we stopped filling the before content padding
            firstItemOffsets.offsetBy(beforeContentPadding)

            val maxMainAxis = (maxOffset + afterContentPadding).coerceAtLeast(0)

            // compose first visible items we received from state
            currentItemIndices.forEachIndexed { laneIndex, itemIndex ->
                if (itemIndex == -1) return@forEachIndexed

                val measuredItem = measuredItemProvider.getAndMeasure(itemIndex, laneIndex)
                currentItemOffsets[laneIndex] += measuredItem.sizeWithSpacings

                if (
                    currentItemOffsets[laneIndex] <= minOffset &&
                        measuredItem.index != itemCount - 1
                ) {
                    // this item is offscreen and will not be placed. advance item index
                    firstItemIndices[laneIndex] = -1
                    firstItemOffsets[laneIndex] -= measuredItem.sizeWithSpacings
                } else {
                    measuredItems[laneIndex].add(measuredItem)
                }
            }

            // then composing visible items forward until we fill the whole viewport.
            // we want to have at least one item in visibleItems even if in fact all the items are
            // offscreen, this can happen if the content padding is larger than the available size.
            while (
                currentItemOffsets.any { it <= maxMainAxis } ||
                    measuredItems.all { it.isEmpty() }
            ) {
                val laneIndex = currentItemOffsets.indexOfMinValue()
                val nextItemIndex = currentItemIndices.max() + 1

                if (nextItemIndex == itemCount) {
                    break
                }

                if (firstItemIndices[laneIndex] == -1) {
                    firstItemIndices[laneIndex] = nextItemIndex
                }
                spans.setSpan(nextItemIndex, laneIndex)

                val measuredItem = measuredItemProvider.getAndMeasure(nextItemIndex, laneIndex)
                currentItemOffsets[laneIndex] += measuredItem.sizeWithSpacings

                if (
                    currentItemOffsets[laneIndex] <= minOffset &&
                        measuredItem.index != itemCount - 1
                ) {
                    // this item is offscreen and will not be placed. advance item index
                    firstItemIndices[laneIndex] = -1
                    firstItemOffsets[laneIndex] -= measuredItem.sizeWithSpacings
                } else {
                    measuredItems[laneIndex].add(measuredItem)
                }

                currentItemIndices[laneIndex] = nextItemIndex
            }

            // we didn't fill the whole viewport with items starting from firstVisibleItemIndex.
            // lets try to scroll back if we have enough items before firstVisibleItemIndex.
            if (currentItemOffsets.all { it < maxOffset }) {
                val maxOffsetLane = currentItemOffsets.indexOfMaxValue()
                val toScrollBack = maxOffset - currentItemOffsets[maxOffsetLane]
                firstItemOffsets.offsetBy(-toScrollBack)
                currentItemOffsets.offsetBy(toScrollBack)
                while (
                    firstItemOffsets.any { it < beforeContentPadding } &&
                        firstItemIndices.all { it != 0 }
                ) {
                    val laneIndex = firstItemOffsets.indexOfMinValue()
                    val currentIndex =
                        if (firstItemIndices[laneIndex] == -1) {
                            itemCount
                        } else {
                            firstItemIndices[laneIndex]
                        }

                    val previousIndex =
                        spans.findPreviousItemIndex(currentIndex, laneIndex)

                    if (previousIndex < 0) {
                        break
                    }

                    val measuredItem = measuredItemProvider.getAndMeasure(
                        previousIndex,
                        laneIndex
                    )
                    measuredItems[laneIndex].add(0, measuredItem)
                    firstItemOffsets[laneIndex] += measuredItem.sizeWithSpacings
                    firstItemIndices[laneIndex] = previousIndex
                }
                scrollDelta += toScrollBack

                val minOffsetLane = firstItemOffsets.indexOfMinValue()
                if (firstItemOffsets[minOffsetLane] < 0) {
                    val offsetValue = firstItemOffsets[minOffsetLane]
                    scrollDelta += offsetValue
                    currentItemOffsets.offsetBy(offsetValue)
                    firstItemOffsets.offsetBy(-offsetValue)
                }
            }

            // report the amount of pixels we consumed. scrollDelta can be smaller than
            // scrollToBeConsumed if there were not enough items to fill the offered space or it
            // can be larger if items were resized, or if, for example, we were previously
            // displaying the item 15, but now we have only 10 items in total in the data set.
            val consumedScroll = if (
                state.scrollToBeConsumed.roundToInt().sign == scrollDelta.sign &&
                    abs(state.scrollToBeConsumed.roundToInt()) >= abs(scrollDelta)
            ) {
                scrollDelta.toFloat()
            } else {
                state.scrollToBeConsumed
            }

            // todo(b/182882362):
            // even if we compose items to fill before content padding we should ignore items fully
            // located there for the state's scroll position calculation (first item + first offset)

            // end measure

            val layoutWidth = if (isVertical) {
                constraints.maxWidth
            } else {
                constraints.constrainWidth(currentItemOffsets.max())
            }
            val layoutHeight = if (isVertical) {
                constraints.constrainHeight(currentItemOffsets.max())
            } else {
                constraints.maxHeight
            }

            // Placement

            val itemScrollOffsets = firstItemOffsets.map { -it }
            val positionedItems = Array(measuredItems.size) {
                mutableListOf<LazyStaggeredGridPositionedItem>()
            }

            var currentCrossAxis = 0
            measuredItems.forEachIndexed { i, laneItems ->
                var currentMainAxis = itemScrollOffsets[i]

                // todo(b/182882362): arrangement/spacing support

                laneItems.fastForEach { item ->
                    positionedItems[i] += item.position(
                        currentMainAxis,
                        currentCrossAxis,
                    )
                    currentMainAxis += item.sizeWithSpacings
                }
                if (laneItems.isNotEmpty()) {
                    currentCrossAxis += laneItems[0].crossAxisSize
                }
            }

            // End placement

            // todo: reverse layout support
            // only scroll backward if the first item is not on screen or fully visible
            val canScrollBackward = !(firstItemIndices[0] == 0 && firstItemOffsets[0] <= 0)
            // only scroll forward if the last item is not on screen or fully visible
            val canScrollForward = currentItemIndices.indexOf(itemCount - 1).let { laneIndex ->
                if (laneIndex == -1) {
                    true
                } else {
                    (currentItemOffsets[laneIndex] -
                        measuredItems[laneIndex].last().sizeWithSpacings) < mainAxisAvailableSize
                }
            }

            @Suppress("UNCHECKED_CAST")
            LazyStaggeredGridMeasureResult(
                firstVisibleItemIndices = firstItemIndices,
                firstVisibleItemScrollOffsets = firstItemOffsets,
                consumedScroll = consumedScroll,
                measureResult = layout(layoutWidth, layoutHeight) {
                    positionedItems.forEach {
                        it.fastForEach { item ->
                            item.place(this)
                        }
                    }
                },
                canScrollForward = canScrollForward,
                canScrollBackward = canScrollBackward,
                visibleItemsInfo = positionedItems as Array<List<LazyStaggeredGridItemInfo>>
            ).also {
                state.applyMeasureResult(it)
                refreshOverscrollInfo(overscrollEffect, it)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun refreshOverscrollInfo(
    overscrollEffect: OverscrollEffect,
    result: LazyStaggeredGridMeasureResult
) {
    overscrollEffect.isEnabled = result.canScrollForward || result.canScrollBackward
}

private fun IntArray.offsetBy(delta: Int) {
    for (i in indices) {
        this[i] = this[i] + delta
    }
}

private fun IntArray.indexOfMinValue(): Int {
    var result = -1
    var min = Int.MAX_VALUE
    for (i in indices) {
        if (min > this[i]) {
            min = this[i]
            result = i
        }
    }

    return result
}

private fun IntArray.indexOfMaxValue(): Int {
    var result = -1
    var max = Int.MIN_VALUE
    for (i in indices) {
        if (max < this[i]) {
            max = this[i]
            result = i
        }
    }

    return result
}

private fun SpanLookup.findPreviousItemIndex(item: Int, lane: Int): Int {
    for (i in (item - 1) downTo 0) {
        val span = getSpan(i)
        if (span == lane || span == SpanLookup.SpanUnset) {
            return i
        }
    }
    return -1
}

@OptIn(ExperimentalFoundationApi::class)
private class LazyStaggeredGridMeasureProvider(
    private val isVertical: Boolean,
    private val itemProvider: LazyLayoutItemProvider,
    private val measureScope: LazyLayoutMeasureScope,
    private val resolvedSlotSums: IntArray,
    private val measuredItemFactory: MeasuredItemFactory
) {
    fun childConstraints(slot: Int): Constraints {
        val previousSum = if (slot == 0) 0 else resolvedSlotSums[slot - 1]
        val crossAxisSize = resolvedSlotSums[slot] - previousSum
        return if (isVertical) {
            Constraints.fixedWidth(crossAxisSize)
        } else {
            Constraints.fixedHeight(crossAxisSize)
        }
    }

    fun getAndMeasure(index: Int, slot: Int): LazyStaggeredGridMeasuredItem {
        val key = itemProvider.getKey(index)
        val placeables = measureScope.measure(index, childConstraints(slot))
        return measuredItemFactory.createItem(index, key, placeables)
    }
}

private class LazyStaggeredGridMeasuredItem(
    val index: Int,
    val key: Any,
    val placeables: List<Placeable>,
    val isVertical: Boolean
) {
    val sizeWithSpacings: Int = placeables.fastFold(0) { size, placeable ->
        size + if (isVertical) placeable.height else placeable.width
    }

    val crossAxisSize: Int = placeables.fastMaxOfOrNull {
        if (isVertical) it.width else it.height
    }!!

    fun position(
        mainAxis: Int,
        crossAxis: Int,
    ): LazyStaggeredGridPositionedItem =
        LazyStaggeredGridPositionedItem(
            offset = if (isVertical) {
                IntOffset(crossAxis, mainAxis)
            } else {
                IntOffset(mainAxis, crossAxis)
            },
            index = index,
            key = key,
            size = IntSize(sizeWithSpacings, crossAxisSize),
            placeables = placeables
        )
}

private class LazyStaggeredGridPositionedItem(
    override val offset: IntOffset,
    override val index: Int,
    override val key: Any,
    override val size: IntSize,
    private val placeables: List<Placeable>
) : LazyStaggeredGridItemInfo {
    fun place(scope: Placeable.PlacementScope) = with(scope) {
        placeables.fastForEach { placeable ->
            placeable.placeWithLayer(offset)
        }
    }
}

// This interface allows to avoid autoboxing on index param
private fun interface MeasuredItemFactory {
    fun createItem(
        index: Int,
        key: Any,
        placeables: List<Placeable>
    ): LazyStaggeredGridMeasuredItem
}