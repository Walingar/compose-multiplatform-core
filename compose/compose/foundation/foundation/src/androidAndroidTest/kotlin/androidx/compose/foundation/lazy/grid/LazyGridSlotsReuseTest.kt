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

package androidx.compose.foundation.lazy.grid

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.LazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFoundationApi::class)
@LargeTest
@RunWith(AndroidJUnit4::class)
class LazyGridSlotsReuseTest {

    @get:Rule
    val rule = createComposeRule()

    val itemsSizePx = 30f
    val itemsSizeDp = with(rule.density) { itemsSizePx.toDp() }

    @Test
    fun scroll1ItemScrolledOffItemIsKeptForReuse() {
        lateinit var state: LazyGridState
        rule.setContent {
            state = rememberLazyGridState()
            LazyVerticalGrid(
                GridCells.Fixed(1),
                Modifier.height(itemsSizeDp * 1.5f),
                state
            ) {
                items(100) {
                    Spacer(Modifier.height(itemsSizeDp).fillMaxWidth().testTag("$it"))
                }
            }
        }

        rule.onNodeWithTag("0")
            .assertIsDisplayed()

        rule.runOnIdle {
            runBlocking {
                state.scrollToItem(1)
            }
        }

        rule.onNodeWithTag("0")
            .assertExists()
            .assertIsNotDisplayed()
        rule.onNodeWithTag("1")
            .assertIsDisplayed()
    }

    @Test
    fun scroll2ItemsScrolledOffItemsAreKeptForReuse() {
        lateinit var state: LazyGridState
        rule.setContent {
            state = rememberLazyGridState()
            LazyVerticalGrid(
                GridCells.Fixed(1),
                Modifier.height(itemsSizeDp * 1.5f),
                state
            ) {
                items(100) {
                    Spacer(Modifier.height(itemsSizeDp).fillMaxWidth().testTag("$it"))
                }
            }
        }

        rule.onNodeWithTag("0")
            .assertIsDisplayed()
        rule.onNodeWithTag("1")
            .assertIsDisplayed()

        rule.runOnIdle {
            runBlocking {
                state.scrollToItem(2)
            }
        }

        rule.onNodeWithTag("0")
            .assertExists()
            .assertIsNotDisplayed()
        rule.onNodeWithTag("1")
            .assertExists()
            .assertIsNotDisplayed()
        rule.onNodeWithTag("2")
            .assertIsDisplayed()
    }

    @Test
    fun scroll3Items2OfScrolledOffItemsAreKeptForReuse() {
        lateinit var state: LazyGridState
        rule.setContent {
            state = rememberLazyGridState()
            LazyVerticalGrid(
                GridCells.Fixed(1),
                Modifier.height(itemsSizeDp * 1.5f),
                state
            ) {
                items(100) {
                    Spacer(Modifier.height(itemsSizeDp).fillMaxWidth().testTag("$it"))
                }
            }
        }

        rule.onNodeWithTag("0")
            .assertIsDisplayed()
        rule.onNodeWithTag("1")
            .assertIsDisplayed()

        rule.runOnIdle {
            runBlocking {
                // after this step 0 and 1 are in reusable buffer
                state.scrollToItem(2)

                // this step requires one item and will take the last item from the buffer - item
                // 1 plus will put 2 in the buffer. so expected buffer is items 2 and 0
                state.scrollToItem(3)
            }
        }

        // recycled
        rule.onNodeWithTag("1")
            .assertDoesNotExist()

        // in buffer
        rule.onNodeWithTag("0")
            .assertExists()
            .assertIsNotDisplayed()
        rule.onNodeWithTag("2")
            .assertExists()
            .assertIsNotDisplayed()

        // visible
        rule.onNodeWithTag("3")
            .assertIsDisplayed()
        rule.onNodeWithTag("4")
            .assertIsDisplayed()
    }

    @Test
    fun doMultipleScrollsOneByOne() {
        lateinit var state: LazyGridState
        rule.setContent {
            state = rememberLazyGridState()
            LazyVerticalGrid(
                GridCells.Fixed(1),
                Modifier.height(itemsSizeDp * 1.5f),
                state
            ) {
                items(100) {
                    Spacer(Modifier.height(itemsSizeDp).fillMaxWidth().testTag("$it"))
                }
            }
        }
        rule.runOnIdle {
            runBlocking {
                state.scrollToItem(1) // buffer is [0]
                state.scrollToItem(2) // 0 used, buffer is [1]
                state.scrollToItem(3) // 1 used, buffer is [2]
                state.scrollToItem(4) // 2 used, buffer is [3]
            }
        }

        // recycled
        rule.onNodeWithTag("0")
            .assertDoesNotExist()
        rule.onNodeWithTag("1")
            .assertDoesNotExist()
        rule.onNodeWithTag("2")
            .assertDoesNotExist()

        // in buffer
        rule.onNodeWithTag("3")
            .assertExists()
            .assertIsNotDisplayed()

        // visible
        rule.onNodeWithTag("4")
            .assertIsDisplayed()
        rule.onNodeWithTag("5")
            .assertIsDisplayed()
    }

    @Test
    fun scrollBackwardOnce() {
        lateinit var state: LazyGridState
        rule.setContent {
            state = rememberLazyGridState(10)
            LazyVerticalGrid(
                GridCells.Fixed(1),
                Modifier.height(itemsSizeDp * 1.5f),
                state
            ) {
                items(100) {
                    Spacer(Modifier.height(itemsSizeDp).fillMaxWidth().testTag("$it"))
                }
            }
        }
        rule.runOnIdle {
            runBlocking {
                state.scrollToItem(8) // buffer is [10, 11]
            }
        }

        // in buffer
        rule.onNodeWithTag("10")
            .assertExists()
            .assertIsNotDisplayed()
        rule.onNodeWithTag("11")
            .assertExists()
            .assertIsNotDisplayed()

        // visible
        rule.onNodeWithTag("8")
            .assertIsDisplayed()
        rule.onNodeWithTag("9")
            .assertIsDisplayed()
    }

    @Test
    fun scrollBackwardOneByOne() {
        lateinit var state: LazyGridState
        rule.setContent {
            state = rememberLazyGridState(10)
            LazyVerticalGrid(
                GridCells.Fixed(1),
                Modifier.height(itemsSizeDp * 1.5f),
                state
            ) {
                items(100) {
                    Spacer(Modifier.height(itemsSizeDp).fillMaxWidth().testTag("$it"))
                }
            }
        }
        rule.runOnIdle {
            runBlocking {
                state.scrollToItem(9) // buffer is [11]
                state.scrollToItem(7) // 11 reused, buffer is [9]
                state.scrollToItem(6) // 9 reused, buffer is [8]
            }
        }

        // in buffer
        rule.onNodeWithTag("8")
            .assertExists()
            .assertIsNotDisplayed()

        // visible
        rule.onNodeWithTag("6")
            .assertIsDisplayed()
        rule.onNodeWithTag("7")
            .assertIsDisplayed()
    }

    @Test
    fun scrollingBackReusesTheSameSlot() {
        lateinit var state: LazyGridState
        var counter0 = 0
        var counter1 = 10
        var rememberedValue0 = -1
        var rememberedValue1 = -1
        rule.setContent {
            state = rememberLazyGridState()
            LazyVerticalGrid(
                GridCells.Fixed(1),
                Modifier.height(itemsSizeDp * 1.5f),
                state
            ) {
                items(100) {
                    if (it == 0) {
                        rememberedValue0 = remember { counter0++ }
                    }
                    if (it == 1) {
                        rememberedValue1 = remember { counter1++ }
                    }
                    Spacer(Modifier.height(itemsSizeDp).fillMaxWidth().testTag("$it"))
                }
            }
        }
        rule.runOnIdle {
            runBlocking {
                state.scrollToItem(2) // buffer is [0, 1]
                state.scrollToItem(0) // scrolled back, 0 and 1 are reused back. buffer: [2, 3]
            }
        }

        rule.runOnIdle {
            Truth.assertWithMessage("Item 0 restored remembered value is $rememberedValue0")
                .that(rememberedValue0).isEqualTo(0)
            Truth.assertWithMessage("Item 1 restored remembered value is $rememberedValue1")
                .that(rememberedValue1).isEqualTo(10)
        }

        rule.onNodeWithTag("0")
            .assertIsDisplayed()
        rule.onNodeWithTag("1")
            .assertIsDisplayed()

        rule.onNodeWithTag("2")
            .assertExists()
            .assertIsNotDisplayed()
        rule.onNodeWithTag("3")
            .assertExists()
            .assertIsNotDisplayed()
    }

    @Test
    fun differentContentTypes() {
        lateinit var state: LazyGridState
        rule.setContent {
            state = rememberLazyGridState()
            LazyVerticalGrid(
                GridCells.Fixed(1),
                Modifier.height(itemsSizeDp * 6.5f),
                state
            ) {
                items(
                    100,
                    contentType = { if (it <= 2) 0 else 1 }
                ) {
                    Spacer(Modifier.height(itemsSizeDp).fillMaxWidth().testTag("$it"))
                }
            }
        }

        rule.onNodeWithTag("0")
            .assertIsDisplayed()
        rule.onNodeWithTag("1")
            .assertIsDisplayed()

        rule.runOnIdle {
            runBlocking {
                state.scrollToItem(6)
            }
        }

        rule.onNodeWithTag("6")
            .assertIsDisplayed()

        // two items of type 0 are left for reuse
        rule.onNodeWithTag("0")
            .assertExists()
            .assertIsNotDisplayed()
        rule.onNodeWithTag("1")
            .assertExists()
            .assertIsNotDisplayed()
        rule.onNodeWithTag("2")
            .assertDoesNotExist()

        // and two items of type 1
        rule.onNodeWithTag("3")
            .assertExists()
            .assertIsNotDisplayed()
        rule.onNodeWithTag("4")
            .assertExists()
            .assertIsNotDisplayed()
        rule.onNodeWithTag("5")
            .assertDoesNotExist()
    }

    @Test
    fun differentTypesFromDifferentItemCalls() {
        lateinit var state: LazyGridState
        rule.setContent {
            state = rememberLazyGridState()
            LazyVerticalGrid(
                GridCells.Fixed(1),
                Modifier.height(itemsSizeDp * 2.5f),
                state
            ) {
                val content = @Composable { tag: String ->
                    Spacer(Modifier.height(itemsSizeDp).width(10.dp).testTag(tag))
                }
                item(contentType = "not-to-reuse-0") {
                    content("0")
                }
                item(contentType = "reuse") {
                    content("1")
                }
                items(
                    List(100) { it + 2 },
                    contentType = { if (it == 10) "reuse" else "not-to-reuse-$it" }) {
                    content("$it")
                }
            }
        }

        rule.runOnIdle {
            runBlocking {
                state.scrollToItem(2)
                // now items 0 and 1 are put into reusables
            }
        }

        rule.onNodeWithTag("0")
            .assertExists()
            .assertIsNotDisplayed()
        rule.onNodeWithTag("1")
            .assertExists()
            .assertIsNotDisplayed()

        rule.runOnIdle {
            runBlocking {
                state.scrollToItem(9)
                // item 10 should reuse slot 1
            }
        }

        rule.onNodeWithTag("0")
            .assertExists()
            .assertIsNotDisplayed()
        rule.onNodeWithTag("1")
            .assertDoesNotExist()
        rule.onNodeWithTag("9")
            .assertIsDisplayed()
        rule.onNodeWithTag("10")
            .assertIsDisplayed()
        rule.onNodeWithTag("11")
            .assertIsDisplayed()
    }
}
