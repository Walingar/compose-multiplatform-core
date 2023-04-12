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

package androidx.compose.foundation.copyPasteAndroidTests.lazy.list

import androidx.compose.foundation.assertThat
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.isEqualTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.ScrollWheel
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalTestApi::class)
class LazyArrangementsTest {

    private val ContainerTag = "ContainerTag"

    private var itemSize: Dp = Dp.Infinity
    private var smallerItemSize: Dp = Dp.Infinity
    private var containerSize: Dp = Dp.Infinity

    private val density = Density(1f)

    @BeforeTest
    fun before() {
        with(density) {
            itemSize = 50.toDp()
        }
        with(density) {
            smallerItemSize = 40.toDp()
        }
        containerSize = itemSize * 5
    }

    // cases when we have not enough items to fill min constraints:

    @Test
    fun column_defaultArrangementIsTop() = runSkikoComposeUiTest {
        setContent {
            LazyColumn(
                modifier = Modifier.requiredSize(containerSize)
            ) {
                items(2) {
                    Box(Modifier.requiredSize(itemSize).testTag(it.toString()))
                }
            }
        }

        assertArrangementForTwoItems(Arrangement.Top)
    }

    @Test
    fun column_centerArrangement() = runSkikoComposeUiTest {
        composeColumnWith(Arrangement.Center)
        assertArrangementForTwoItems(Arrangement.Center)
    }

    @Test
    fun column_bottomArrangement() = runSkikoComposeUiTest {
        composeColumnWith(Arrangement.Bottom)
        assertArrangementForTwoItems(Arrangement.Bottom)
    }

    @Test
    fun column_spacedArrangementNotFillingViewport() = runSkikoComposeUiTest {
        val arrangement = Arrangement.spacedBy(10.dp)
        composeColumnWith(arrangement)
        assertArrangementForTwoItems(arrangement)
    }

    @Test
    fun row_defaultArrangementIsStart() = runSkikoComposeUiTest {
        setContent {
            LazyRow(
                modifier = Modifier.requiredSize(containerSize)
            ) {
                items(2) {
                    Box(Modifier.requiredSize(itemSize).testTag(it.toString()))
                }
            }
        }

        assertArrangementForTwoItems(Arrangement.Start, LayoutDirection.Ltr)
    }

    @Test
    fun row_centerArrangement() = runSkikoComposeUiTest {
        composeRowWith(Arrangement.Center, LayoutDirection.Ltr)
        assertArrangementForTwoItems(Arrangement.Center, LayoutDirection.Ltr)
    }

    @Test
    fun row_endArrangement() = runSkikoComposeUiTest {
        composeRowWith(Arrangement.End, LayoutDirection.Ltr)
        assertArrangementForTwoItems(Arrangement.End, LayoutDirection.Ltr)
    }

    @Test
    fun row_spacedArrangementNotFillingViewport() = runSkikoComposeUiTest {
        val arrangement = Arrangement.spacedBy(10.dp)
        composeRowWith(arrangement, LayoutDirection.Ltr)
        assertArrangementForTwoItems(arrangement, LayoutDirection.Ltr)
    }

    @Test
    fun row_rtl_startArrangement() = runSkikoComposeUiTest {
        composeRowWith(Arrangement.Center, LayoutDirection.Rtl)
        assertArrangementForTwoItems(Arrangement.Center, LayoutDirection.Rtl)
    }

    @Test
    fun row_rtl_endArrangement() = runSkikoComposeUiTest {
        composeRowWith(Arrangement.End, LayoutDirection.Rtl)
        assertArrangementForTwoItems(Arrangement.End, LayoutDirection.Rtl)
    }

    @Test
    fun row_rtl_spacedArrangementNotFillingViewport() = runSkikoComposeUiTest {
        val arrangement = Arrangement.spacedBy(10.dp)
        composeRowWith(arrangement, LayoutDirection.Rtl)
        assertArrangementForTwoItems(arrangement, LayoutDirection.Rtl)
    }

    // wrap content and spacing

    @Test
    fun column_spacing_affects_wrap_content() = runSkikoComposeUiTest {
        setContent {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(itemSize),
                modifier = Modifier.testTag(ContainerTag)
            ) {
                items(2) {
                    Box(Modifier.requiredSize(itemSize))
                }
            }
        }

        onNodeWithTag(ContainerTag)
            .assertWidthIsEqualTo(itemSize)
            .assertHeightIsEqualTo(itemSize * 3)
    }

    @Test
    fun row_spacing_affects_wrap_content() = runSkikoComposeUiTest {
        setContent {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(itemSize),
                modifier = Modifier.testTag(ContainerTag)
            ) {
                items(2) {
                    Box(Modifier.requiredSize(itemSize))
                }
            }
        }

        onNodeWithTag(ContainerTag)
            .assertWidthIsEqualTo(itemSize * 3)
            .assertHeightIsEqualTo(itemSize)
    }

    // spacing added when we have enough items to fill the viewport

    @Test
    fun column_spacing_scrolledToTheTop() = runSkikoComposeUiTest {
        setContent {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(itemSize),
                modifier = Modifier.requiredSize(itemSize * 3.5f)
            ) {
                items(3) {
                    Box(Modifier.requiredSize(itemSize).testTag(it.toString()))
                }
            }
        }

        onNodeWithTag("0")
            .assertTopPositionInRootIsEqualTo(0.dp)

        onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(itemSize * 2)
    }

    @Test
    fun column_spacing_scrolledToTheBottom() = runSkikoComposeUiTest {
        setContent {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(itemSize),
                modifier = Modifier.requiredSize(itemSize * 3.5f).testTag(ContainerTag)
            ) {
                items(3) {
                    Box(Modifier.requiredSize(itemSize).testTag(it.toString()))
                }
            }
        }

        onNodeWithTag(ContainerTag).performMouseInput {
            scroll(itemSize.toPx()  * 2)
        }

        waitForIdle()
        onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(itemSize * 0.5f)

        onNodeWithTag("2")
            .assertTopPositionInRootIsEqualTo(itemSize * 2.5f)
    }

    @Test
    fun row_spacing_scrolledToTheStart() = runSkikoComposeUiTest {
        setContent {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(itemSize),
                modifier = Modifier.requiredSize(itemSize * 3.5f)
            ) {
                items(3) {
                    Box(Modifier.requiredSize(itemSize).testTag(it.toString()))
                }
            }
        }

        onNodeWithTag("0")
            .assertLeftPositionInRootIsEqualTo(0.dp)

        onNodeWithTag("1")
            .assertLeftPositionInRootIsEqualTo(itemSize * 2)
    }

    @Test
    fun row_spacing_scrolledToTheEnd() = runSkikoComposeUiTest {
        setContent {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(itemSize),
                modifier = Modifier.requiredSize(itemSize * 3.5f).testTag(ContainerTag)
            ) {
                items(3) {
                    Box(Modifier.requiredSize(itemSize).testTag(it.toString()))
                }
            }
        }

        onNodeWithTag(ContainerTag).performMouseInput {
            scroll(itemSize.toPx()  * 2, ScrollWheel.Horizontal)
        }

        waitForIdle()
        onNodeWithTag("1")
            .assertLeftPositionInRootIsEqualTo(itemSize * 0.5f)

        onNodeWithTag("2")
            .assertLeftPositionInRootIsEqualTo(itemSize * 2.5f)
    }

    @Test
    fun column_scrollingByExactlyTheItemSizePlusSpacer_switchesTheFirstVisibleItem() = runSkikoComposeUiTest {
        val itemSizePx = 30
        val spacingSizePx = 4
        val itemSize = with(density) { itemSizePx.toDp() }
        val spacingSize = with(density) { spacingSizePx.toDp() }
        lateinit var state: LazyListState
        var scope: CoroutineScope? = null
        setContent {
            scope = rememberCoroutineScope()
            LazyColumn(
                Modifier.size(itemSize * 3),
                state = rememberLazyListState().also { state = it },
                verticalArrangement = Arrangement.spacedBy(spacingSize)
            ) {
                items(5) {
                    Spacer(
                        Modifier.size(itemSize).testTag("$it")
                    )
                }
            }
        }

        runOnIdle {
            scope!!.launch {
                state.scrollBy((itemSizePx + spacingSizePx).toFloat())
            }
        }

        onNodeWithTag("0")
            .assertIsNotDisplayed()

        runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(1)
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
        }
    }

    @Test
    fun column_scrollingByExactlyTheItemSizePlusHalfTheSpacer_staysOnTheSameItem() = runSkikoComposeUiTest {
        val itemSizePx = 30
        val spacingSizePx = 4
        val itemSize = with(density) { itemSizePx.toDp() }
        val spacingSize = with(density) { spacingSizePx.toDp() }
        lateinit var state: LazyListState
        lateinit var scope: CoroutineScope
        setContent {
            scope = rememberCoroutineScope()
            LazyColumn(
                Modifier.size(itemSize * 3),
                state = rememberLazyListState().also { state = it },
                verticalArrangement = Arrangement.spacedBy(spacingSize)
            ) {
                items(5) {
                    Spacer(
                        Modifier.size(itemSize).testTag("$it")
                    )
                }
            }
        }

        runOnIdle {
            scope.launch {
                state.scrollBy((itemSizePx + spacingSizePx / 2).toFloat())
            }
        }

        onNodeWithTag("0")
            .assertIsNotDisplayed()

        runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
            assertThat(state.firstVisibleItemScrollOffset)
                .isEqualTo(itemSizePx + spacingSizePx / 2)
        }
    }

    @Test
    fun row_scrollingByExactlyTheItemSizePlusSpacer_switchesTheFirstVisibleItem() = runSkikoComposeUiTest {
        val itemSizePx = 30
        val spacingSizePx = 4
        val itemSize = with(density) { itemSizePx.toDp() }
        val spacingSize = with(density) { spacingSizePx.toDp() }
        lateinit var state: LazyListState
        lateinit var scope: CoroutineScope
        setContent {
            scope = rememberCoroutineScope()
            LazyRow(
                Modifier.size(itemSize * 3),
                state = rememberLazyListState().also { state = it },
                horizontalArrangement = Arrangement.spacedBy(spacingSize)
            ) {
                items(5) {
                    Spacer(
                        Modifier.size(itemSize).testTag("$it")
                    )
                }
            }
        }

        runOnIdle {
            scope.launch {
                state.scrollBy((itemSizePx + spacingSizePx).toFloat())
            }
        }

        onNodeWithTag("0")
            .assertIsNotDisplayed()

        runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(1)
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
        }
    }

    @Test
    fun row_scrollingByExactlyTheItemSizePlusHalfTheSpacer_staysOnTheSameItem() = runSkikoComposeUiTest {
        val itemSizePx = 30
        val spacingSizePx = 4
        val itemSize = with(density) { itemSizePx.toDp() }
        val spacingSize = with(density) { spacingSizePx.toDp() }
        lateinit var state: LazyListState
        lateinit var scope: CoroutineScope
        setContent {
            scope = rememberCoroutineScope()
            LazyRow(
                Modifier.size(itemSize * 3),
                state = rememberLazyListState().also { state = it },
                horizontalArrangement = Arrangement.spacedBy(spacingSize)
            ) {
                items(5) {
                    Spacer(
                        Modifier.size(itemSize).testTag("$it")
                    )
                }
            }
        }

        runOnIdle {
            scope.launch {
                state.scrollBy((itemSizePx + spacingSizePx / 2).toFloat())
            }
        }

        onNodeWithTag("0")
            .assertIsNotDisplayed()

        runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
            assertThat(state.firstVisibleItemScrollOffset)
                .isEqualTo(itemSizePx + spacingSizePx / 2)
        }
    }

    @Test
    fun column_defaultArrangementIsBottomWithReverseLayout() = runSkikoComposeUiTest {
        setContent {
            LazyColumn(
                reverseLayout = true,
                modifier = Modifier.requiredSize(containerSize)
            ) {
                items(2) {
                    Item(it)
                }
            }
        }

        assertArrangementForTwoItems(Arrangement.Bottom, reverseLayout = true)
    }

    @Test
    fun row_defaultArrangementIsEndWithReverseLayout() = runSkikoComposeUiTest {
        setContent {
            LazyRow(
                reverseLayout = true,
                modifier = Modifier.requiredSize(containerSize)
            ) {
                items(2) {
                    Item(it)
                }
            }
        }

        assertArrangementForTwoItems(
            Arrangement.End, LayoutDirection.Ltr, reverseLayout = true
        )
    }

    @Test
    fun column_whenArrangementChanges() = runSkikoComposeUiTest {
        var arrangement by mutableStateOf(Arrangement.Top)
        setContent {
            LazyColumn(
                modifier = Modifier.requiredSize(containerSize),
                verticalArrangement = arrangement
            ) {
                items(2) {
                    Item(it)
                }
            }
        }

        assertArrangementForTwoItems(Arrangement.Top)

        runOnIdle {
            arrangement = Arrangement.Bottom
        }

        assertArrangementForTwoItems(Arrangement.Bottom)
    }

    @Test
    fun row_whenArrangementChanges() = runSkikoComposeUiTest {
        var arrangement by mutableStateOf(Arrangement.Start)
        setContent {
            LazyRow(
                modifier = Modifier.requiredSize(containerSize),
                horizontalArrangement = arrangement
            ) {
                items(2) {
                    Item(it)
                }
            }
        }

        assertArrangementForTwoItems(Arrangement.Start, LayoutDirection.Ltr)

        runOnIdle {
            arrangement = Arrangement.End
        }

        assertArrangementForTwoItems(Arrangement.End, LayoutDirection.Ltr)
    }

    @Test
    fun column_negativeSpacing_itemsVisible() = runSkikoComposeUiTest {
        val state = LazyListState()
        val halfItemSize = itemSize / 2
        var scope: CoroutineScope? = null
        setContent {
            scope = rememberCoroutineScope()
            LazyColumn(
                modifier = Modifier.requiredSize(itemSize),
                verticalArrangement = Arrangement.spacedBy(-halfItemSize),
                state = state
            ) {
                items(100) { index ->
                    Box(Modifier.size(itemSize).testTag(index.toString()))
                }
            }
        }

        onNodeWithTag("0")
            .assertTopPositionInRootIsEqualTo(0.dp)
        onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(halfItemSize)

        runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
        }

        scope!!.launch {
            state.scrollBy(with(density) { halfItemSize.toPx() })
        }

        runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(1)
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
        }

        onNodeWithTag("0")
            .assertTopPositionInRootIsEqualTo(-halfItemSize)
        onNodeWithTag("1")
            .assertTopPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun row_negativeSpacing_itemsVisible() = runSkikoComposeUiTest {
        val state = LazyListState()
        val halfItemSize = itemSize / 2
        var scope: CoroutineScope? = null
        setContent {
            scope = rememberCoroutineScope()
            LazyRow(
                modifier = Modifier.requiredSize(itemSize),
                horizontalArrangement = Arrangement.spacedBy(-halfItemSize),
                state = state
            ) {
                items(100) { index ->
                    Box(Modifier.size(itemSize).testTag(index.toString()))
                }
            }
        }

        onNodeWithTag("0")
            .assertLeftPositionInRootIsEqualTo(0.dp)
        onNodeWithTag("1")
            .assertLeftPositionInRootIsEqualTo(halfItemSize)

        runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
        }

        scope!!.launch {
            state.scrollBy(with(density) { halfItemSize.toPx() })
        }

        runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(1)
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
        }

        onNodeWithTag("0")
            .assertLeftPositionInRootIsEqualTo(-halfItemSize)
        onNodeWithTag("1")
            .assertLeftPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun column_negativeSpacingLargerThanItem_itemsVisible() = runSkikoComposeUiTest {
        val state = LazyListState(firstVisibleItemIndex = 2)
        val largerThanItemSize = itemSize * 1.5f
        setContent {
            LazyColumn(
                modifier = Modifier.requiredSize(itemSize),
                verticalArrangement = Arrangement.spacedBy(-largerThanItemSize),
                state = state
            ) {
                items(4) { index ->
                    Box(Modifier.size(itemSize).testTag(index.toString()))
                }
            }
        }

        repeat(4) {
            onNodeWithTag("$it")
                .assertTopPositionInRootIsEqualTo(0.dp)
        }

        runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
        }
    }

    @Test
    fun row_negativeSpacingLargerThanItem_itemsVisible() = runSkikoComposeUiTest {
        val state = LazyListState(firstVisibleItemIndex = 2)
        val largerThanItemSize = itemSize * 1.5f
        setContent {
            LazyRow(
                modifier = Modifier.requiredSize(itemSize),
                horizontalArrangement = Arrangement.spacedBy(-largerThanItemSize),
                state = state
            ) {
                items(4) { index ->
                    Box(Modifier.size(itemSize).testTag(index.toString()))
                }
            }
        }

        repeat(4) {
            onNodeWithTag("$it")
                .assertLeftPositionInRootIsEqualTo(0.dp)
        }

        runOnIdle {
            assertThat(state.firstVisibleItemIndex).isEqualTo(0)
            assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
        }
    }

    private fun SkikoComposeUiTest.composeColumnWith(arrangement: Arrangement.Vertical) {
        setContent {
            LazyColumn(
                verticalArrangement = arrangement,
                modifier = Modifier.requiredSize(containerSize)
            ) {
                items(2) {
                    Item(it)
                }
            }
        }
    }

    private fun SkikoComposeUiTest.composeRowWith(arrangement: Arrangement.Horizontal, layoutDirection: LayoutDirection) {
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                LazyRow(
                    horizontalArrangement = arrangement,
                    modifier = Modifier.requiredSize(containerSize)
                ) {
                    items(2) {
                        Item(it)
                    }
                }
            }
        }
    }

    @Composable
    fun Item(index: Int) {
        require(index < 2)
        val size = if (index == 0) itemSize else smallerItemSize
        Box(Modifier.requiredSize(size).testTag(index.toString()))
    }

    private fun SkikoComposeUiTest.assertArrangementForTwoItems(
        arrangement: Arrangement.Vertical,
        reverseLayout: Boolean = false
    ) {
        with(density) {
            val sizes = IntArray(2) {
                val index = if (reverseLayout) if (it == 0) 1 else 0 else it
                if (index == 0) itemSize.roundToPx() else smallerItemSize.roundToPx()
            }
            val outPositions = IntArray(2) { 0 }
            with(arrangement) { arrange(containerSize.roundToPx(), sizes, outPositions) }

            outPositions.forEachIndexed { index, position ->
                val realIndex = if (reverseLayout) if (index == 0) 1 else 0 else index
                onNodeWithTag("$realIndex")
                    .assertTopPositionInRootIsEqualTo(position.toDp())
            }
        }
    }

    private fun SkikoComposeUiTest.assertArrangementForTwoItems(
        arrangement: Arrangement.Horizontal,
        layoutDirection: LayoutDirection,
        reverseLayout: Boolean = false
    ) {
        with(density) {
            val sizes = IntArray(2) {
                val index = if (reverseLayout) if (it == 0) 1 else 0 else it
                if (index == 0) itemSize.roundToPx() else smallerItemSize.roundToPx()
            }
            val outPositions = IntArray(2) { 0 }
            with(arrangement) {
                arrange(containerSize.roundToPx(), sizes, layoutDirection, outPositions)
            }

            outPositions.forEachIndexed { index, position ->
                val realIndex = if (reverseLayout) if (index == 0) 1 else 0 else index
                val expectedPosition = position.toDp()
                onNodeWithTag("$realIndex")
                    .assertLeftPositionInRootIsEqualTo(expectedPosition)
            }
        }
    }
}
