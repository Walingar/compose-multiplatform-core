/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.material3

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.tokens.NavigationDrawerTokens
import androidx.compose.material3.tokens.PaletteTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Possible values of [DrawerState].
 */
@ExperimentalMaterial3Api
enum class DrawerValue {
    /**
     * The state of the drawer when it is closed.
     */
    Closed,

    /**
     * The state of the drawer when it is open.
     */
    Open
}

/**
 * State of the [ModalNavigationDrawer] and [DismissibleNavigationDrawer] composable.
 *
 * @param initialValue The initial value of the state.
 * @param confirmStateChange Optional callback invoked to confirm or veto a pending state change.
 */
@Suppress("NotCloseable")
@ExperimentalMaterial3Api
@Stable
class DrawerState(
    initialValue: DrawerValue,
    confirmStateChange: (DrawerValue) -> Boolean = { true }
) {

    internal val swipeableState = SwipeableState(
        initialValue = initialValue,
        animationSpec = AnimationSpec,
        confirmStateChange = confirmStateChange
    )

    /**
     * Whether the drawer is open.
     */
    val isOpen: Boolean
        get() = currentValue == DrawerValue.Open

    /**
     * Whether the drawer is closed.
     */
    val isClosed: Boolean
        get() = currentValue == DrawerValue.Closed

    /**
     * The current value of the state.
     *
     * If no swipe or animation is in progress, this corresponds to the start the drawer
     * currently in. If a swipe or an animation is in progress, this corresponds the state drawer
     * was in before the swipe or animation started.
     */
    val currentValue: DrawerValue
        get() {
            return swipeableState.currentValue
        }

    /**
     * Whether the state is currently animating.
     */
    val isAnimationRunning: Boolean
        get() {
            return swipeableState.isAnimationRunning
        }

    /**
     * Open the drawer with animation and suspend until it if fully opened or animation has been
     * cancelled. This method will throw [CancellationException] if the animation is
     * interrupted
     *
     * @return the reason the open animation ended
     */
    suspend fun open() = animateTo(DrawerValue.Open, AnimationSpec)

    /**
     * Close the drawer with animation and suspend until it if fully closed or animation has been
     * cancelled. This method will throw [CancellationException] if the animation is
     * interrupted
     *
     * @return the reason the close animation ended
     */
    suspend fun close() = animateTo(DrawerValue.Closed, AnimationSpec)

    /**
     * Set the state of the drawer with specific animation
     *
     * @param targetValue The new value to animate to.
     * @param anim The animation that will be used to animate to the new value.
     */
    @ExperimentalMaterial3Api
    suspend fun animateTo(targetValue: DrawerValue, anim: AnimationSpec<Float>) {
        swipeableState.animateTo(targetValue, anim)
    }

    /**
     * Set the state without any animation and suspend until it's set
     *
     * @param targetValue The new target value
     */
    @ExperimentalMaterial3Api
    suspend fun snapTo(targetValue: DrawerValue) {
        swipeableState.snapTo(targetValue)
    }

    /**
     * The target value of the drawer state.
     *
     * If a swipe is in progress, this is the value that the Drawer would animate to if the
     * swipe finishes. If an animation is running, this is the target value of that animation.
     * Finally, if no swipe or animation is in progress, this is the same as the [currentValue].
     */
    @Suppress("EXPERIMENTAL_ANNOTATION_ON_WRONG_TARGET")
    @ExperimentalMaterial3Api
    @get:ExperimentalMaterial3Api
    val targetValue: DrawerValue
        get() = swipeableState.targetValue

    /**
     * The current position (in pixels) of the drawer container.
     */
    @Suppress("EXPERIMENTAL_ANNOTATION_ON_WRONG_TARGET")
    @ExperimentalMaterial3Api
    @get:ExperimentalMaterial3Api
    val offset: State<Float>
        get() = swipeableState.offset

    companion object {
        /**
         * The default [Saver] implementation for [DrawerState].
         */
        fun Saver(confirmStateChange: (DrawerValue) -> Boolean) =
            Saver<DrawerState, DrawerValue>(
                save = { it.currentValue },
                restore = { DrawerState(it, confirmStateChange) }
            )
    }
}

/**
 * Create and [remember] a [DrawerState].
 *
 * @param initialValue The initial value of the state.
 * @param confirmStateChange Optional callback invoked to confirm or veto a pending state change.
 */
@Composable
@ExperimentalMaterial3Api
fun rememberDrawerState(
    initialValue: DrawerValue,
    confirmStateChange: (DrawerValue) -> Boolean = { true }
): DrawerState {
    return rememberSaveable(saver = DrawerState.Saver(confirmStateChange)) {
        DrawerState(initialValue, confirmStateChange)
    }
}

/**
 * ![Navigation drawer image](https://developer.android.com/images/reference/androidx/compose/material3/navigation-drawer.png)
 *
 * Material Design navigation drawer.
 *
 * Modal navigation drawers block interaction with the rest of an app’s content with a scrim.
 * They are elevated above most of the app’s UI and don’t affect the screen’s layout grid.
 *
 * @sample androidx.compose.material3.samples.ModalNavigationDrawerSample
 *
 * @param drawerContent composable that represents content inside the drawer
 * @param modifier optional modifier for the drawer
 * @param drawerState state of the drawer
 * @param gesturesEnabled whether or not drawer can be interacted by gestures
 * @param drawerShape shape of the drawer container
 * @param drawerTonalElevation Affects the alpha of the color overlay applied on the container color
 * of the drawer container.
 * @param drawerContainerColor container color to be used for the drawer container
 * @param drawerContentColor color of the content to use inside the drawer container. Defaults to
 * either the matching content color for [drawerContainerColor], or, if it is not a color from
 * the theme, this will keep the same value set above this Surface.
 * @param scrimColor color of the scrim that obscures content when the drawer is open
 * @param content content of the rest of the UI
 */
@Composable
@ExperimentalMaterial3Api
fun ModalNavigationDrawer(
    drawerContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
    gesturesEnabled: Boolean = true,
    drawerShape: Shape = RoundedCornerShape(0.dp, 16.dp, 16.dp, 0.dp),
    drawerTonalElevation: Dp = DrawerDefaults.ModalDrawerElevation,
    drawerContainerColor: Color = NavigationDrawerTokens.ContainerColor.toColor(),
    drawerContentColor: Color = contentColorFor(drawerContainerColor),
    scrimColor: Color = DrawerDefaults.scrimColor,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val minValue = -with(LocalDensity.current) { NavigationDrawerTokens.ContainerWidth.toPx() }
    val maxValue = 0f

    val anchors = mapOf(minValue to DrawerValue.Closed, maxValue to DrawerValue.Open)
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Box(
        modifier.fillMaxSize().swipeable(
            state = drawerState.swipeableState,
            anchors = anchors,
            thresholds = { _, _ -> FractionalThreshold(0.5f) },
            orientation = Orientation.Horizontal,
            enabled = gesturesEnabled,
            reverseDirection = isRtl,
            velocityThreshold = DrawerVelocityThreshold,
            resistance = null
        )
    ) {
        Box {
            content()
        }
        Scrim(
            open = drawerState.isOpen,
            onClose = {
                if (
                    gesturesEnabled &&
                    drawerState.swipeableState.confirmStateChange(DrawerValue.Closed)
                ) {
                    scope.launch { drawerState.close() }
                }
            },
            fraction = {
                calculateFraction(minValue, maxValue, drawerState.offset.value)
            },
            color = scrimColor
        )
        val navigationMenu = getString(Strings.NavigationMenu)
        Surface(
            modifier = Modifier
                .sizeIn(maxWidth = NavigationDrawerTokens.ContainerWidth)
                .offset { IntOffset(drawerState.offset.value.roundToInt(), 0) }
                .semantics {
                    paneTitle = navigationMenu
                    if (drawerState.isOpen) {
                        dismiss {
                            if (
                                drawerState.swipeableState
                                    .confirmStateChange(DrawerValue.Closed)
                            ) {
                                scope.launch { drawerState.close() }
                            }; true
                        }
                    }
                },
            shape = drawerShape,
            color = drawerContainerColor,
            contentColor = drawerContentColor,
            tonalElevation = drawerTonalElevation
        ) {
            Column(Modifier.fillMaxSize(), content = drawerContent)
        }
    }
}

@Composable
@ExperimentalMaterial3Api
@Deprecated(
    "NavigationDrawer has been renamed to ModalNavigationDrawer to better specify " +
        "its modal nature", replaceWith = ReplaceWith(
        "ModalNavigationDrawer(drawerContent,\n" +
            "        modifier,\n" +
            "        drawerState,\n" +
            "        gesturesEnabled,\n" +
            "        drawerShape,\n" +
            "        drawerTonalElevation,\n" +
            "        drawerContainerColor,\n" +
            "        drawerContentColor,\n" +
            "        scrimColor,\n" +
            "        content)"
    )
)
fun NavigationDrawer(
    drawerContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
    gesturesEnabled: Boolean = true,
    drawerShape: Shape = RoundedCornerShape(0.dp, 16.dp, 16.dp, 0.dp),
    drawerTonalElevation: Dp = DrawerDefaults.ModalDrawerElevation,
    drawerContainerColor: Color = NavigationDrawerTokens.ContainerColor.toColor(),
    drawerContentColor: Color = contentColorFor(drawerContainerColor),
    scrimColor: Color = DrawerDefaults.scrimColor,
    content: @Composable () -> Unit
) {
    ModalNavigationDrawer(
        drawerContent,
        modifier,
        drawerState,
        gesturesEnabled,
        drawerShape,
        drawerTonalElevation,
        drawerContainerColor,
        drawerContentColor,
        scrimColor,
        content
    )
}

// TODO(b/218286829): Add spec image
/**
 * Material Design navigation drawer.
 *
 * Standard navigation drawers provide access to drawer destinations and app content on desktop and
 * tablet screens. They’re often next to app content and affect the screen’s layout grid.
 *
 * Dismissible standard drawers can be used for layouts that prioritize content (such as a
 * photo gallery) or for apps where users are unlikely to switch destinations often. They should
 * use a visible navigation menu icon to open and close the drawer.
 *
 * @sample androidx.compose.material3.samples.DismissibleNavigationDrawerSample
 *
 * @param drawerContent composable that represents content inside the drawer
 * @param modifier optional modifier for the drawer
 * @param drawerState state of the drawer
 * @param gesturesEnabled whether or not drawer can be interacted by gestures
 * @param drawerShape shape of the drawer container
 * @param drawerTonalElevation Affects the alpha of the color overlay applied on the container color
 * of the drawer container.
 * @param drawerContainerColor container color to be used for the drawer container
 * @param drawerContentColor color of the content to use inside the drawer container. Defaults to
 * either the matching content color for [drawerContainerColor], or, if it is not a color from
 * the theme, this will keep the same value set above this Surface.
 * @param content content of the rest of the UI
 */
@Composable
@ExperimentalMaterial3Api
fun DismissibleNavigationDrawer(
    drawerContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
    gesturesEnabled: Boolean = true,
    drawerShape: Shape = RectangleShape,
    drawerTonalElevation: Dp = DrawerDefaults.DismissibleDrawerElevation,
    drawerContainerColor: Color = MaterialTheme.colorScheme.surface,
    drawerContentColor: Color = contentColorFor(drawerContainerColor),
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val drawerWidth = NavigationDrawerTokens.ContainerWidth
    val drawerWidthPx = with(LocalDensity.current) { drawerWidth.toPx() }
    val minValue = -drawerWidthPx
    val maxValue = 0f

    val anchors = mapOf(minValue to DrawerValue.Closed, maxValue to DrawerValue.Open)
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Box(
        modifier.swipeable(
            state = drawerState.swipeableState,
            anchors = anchors,
            thresholds = { _, _ -> FractionalThreshold(0.5f) },
            orientation = Orientation.Horizontal,
            enabled = gesturesEnabled,
            reverseDirection = isRtl,
            velocityThreshold = DrawerVelocityThreshold,
            resistance = null
        )
    ) {
        Layout(content = {
            val navigationMenu = getString(Strings.NavigationMenu)
            Surface(
                modifier = Modifier
                    .sizeIn(maxWidth = drawerWidth)
                    .semantics {
                        paneTitle = navigationMenu
                        if (drawerState.isOpen) {
                            dismiss {
                                if (
                                    drawerState.swipeableState
                                        .confirmStateChange(DrawerValue.Closed)
                                ) {
                                    scope.launch { drawerState.close() }
                                }; true
                            }
                        }
                    },
                shape = drawerShape,
                color = drawerContainerColor,
                contentColor = drawerContentColor,
                tonalElevation = drawerTonalElevation
            ) {
                Column(Modifier.fillMaxSize(), content = drawerContent)
            }
            Box {
                content()
            }
        }) { measurables, constraints ->
            val sheetPlaceable = measurables[0].measure(constraints)
            val contentPlaceable = measurables[1].measure(constraints)
            layout(contentPlaceable.width, contentPlaceable.height) {
                contentPlaceable.placeRelative(
                    sheetPlaceable.width + drawerState.offset.value.roundToInt(),
                    0
                )
                sheetPlaceable.placeRelative(drawerState.offset.value.roundToInt(), 0)
            }
        }
    }
}

// TODO(b/218286829): Add spec image
/**
 * Material Design navigation permanent drawer.
 *
 * Standard navigation drawers provide access to drawer destinations and app content on desktop
 * and tablet screens. They’re often next to app content and affect the screen’s layout grid.
 *
 * The permanent navigation drawer is always visible and usually used for frequently switching
 * destinations. On mobile screens, use [ModalNavigationDrawer] instead.
 *
 * @sample androidx.compose.material3.samples.PermanentNavigationDrawerSample
 *
 * @param drawerContent composable that represents content inside the drawer
 * @param modifier optional modifier for the drawer
 * @param drawerShape shape of the drawer container
 * @param drawerTonalElevation Affects the alpha of the color overlay applied on the container color
 * of the drawer container.
 * @param drawerContainerColor container color to be used for the drawer container
 * @param drawerContentColor color of the content to use inside the drawer container. Defaults to
 * either the matching content color for [drawerContainerColor], or, if it is not a color from
 * the theme, this will keep the same value set above this Surface.
 * @param content content of the rest of the UI
 */
@ExperimentalMaterial3Api
@Composable
fun PermanentNavigationDrawer(
    drawerContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    drawerShape: Shape = RectangleShape,
    drawerTonalElevation: Dp = DrawerDefaults.PermanentDrawerElevation,
    drawerContainerColor: Color = MaterialTheme.colorScheme.surface,
    drawerContentColor: Color = contentColorFor(drawerContainerColor),
    content: @Composable () -> Unit
) {
    val drawerWidth = NavigationDrawerTokens.ContainerWidth
    Row(modifier.fillMaxSize()) {
        val navigationMenu = getString(Strings.NavigationMenu)
        Surface(
            modifier = Modifier
                .sizeIn(maxWidth = drawerWidth)
                .semantics {
                    paneTitle = navigationMenu
                },
            shape = drawerShape,
            color = drawerContainerColor,
            contentColor = drawerContentColor,
            tonalElevation = drawerTonalElevation
        ) {
            Column(Modifier.fillMaxSize(), content = drawerContent)
        }
        Box {
            content()
        }
    }
}

/**
 * Object to hold default values for [ModalNavigationDrawer]
 */
@ExperimentalMaterial3Api
object DrawerDefaults {

    /**
     * Default Elevation for drawer container in the [ModalNavigationDrawer] as specified in
     * material specs
     */
    val ModalDrawerElevation = NavigationDrawerTokens.ModalContainerElevation

    /**
     * Default Elevation for drawer container in the [PermanentNavigationDrawer] as specified in
     * material specs
     */
    val PermanentDrawerElevation = NavigationDrawerTokens.StandardContainerElevation

    /**
     * Default Elevation for drawer container in the [DismissibleNavigationDrawer] as specified in
     * material specs
     */
    val DismissibleDrawerElevation = NavigationDrawerTokens.StandardContainerElevation

    val scrimColor: Color
        @Composable
        get() = PaletteTokens.NeutralVariant0.copy(alpha = NavigationDrawerTokens.ScrimOpacity)
}

private fun calculateFraction(a: Float, b: Float, pos: Float) =
    ((pos - a) / (b - a)).coerceIn(0f, 1f)

@Composable
private fun Scrim(
    open: Boolean,
    onClose: () -> Unit,
    fraction: () -> Float,
    color: Color
) {
    val closeDrawer = getString(Strings.CloseDrawer)
    val dismissDrawer = if (open) {
        Modifier
            .pointerInput(onClose) { detectTapGestures { onClose() } }
            .semantics(mergeDescendants = true) {
                contentDescription = closeDrawer
                onClick { onClose(); true }
            }
    } else {
        Modifier
    }

    Canvas(
        Modifier
            .fillMaxSize()
            .then(dismissDrawer)
    ) {
        drawRect(color, alpha = fraction())
    }
}

private val DrawerVelocityThreshold = 400.dp

// TODO: b/177571613 this should be a proper decay settling
// this is taken from the DrawerLayout's DragViewHelper as a min duration.
private val AnimationSpec = TweenSpec<Float>(durationMillis = 256)
