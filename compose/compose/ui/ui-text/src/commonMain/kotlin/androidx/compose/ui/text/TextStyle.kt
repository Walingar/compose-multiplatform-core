/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.text

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.useOrElse
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.annotation.VisibleForTesting

/** The default font size if none is specified. */
private val DefaultFontSize = 14.sp
private val DefaultLetterSpacing = 0.sp
private val DefaultBackgroundColor = Color.Transparent
// TODO(nona): Introduce TextUnit.Original for representing "do not change the original result".
//  Need to distinguish from Inherit.
private val DefaultLineHeight = TextUnit.Inherit
private val DefaultColor = Color.Black

/**
 * Styling configuration for a `Text`.
 *
 * @sample androidx.compose.ui.text.samples.TextStyleSample
 *
 * @param color The text color.
 * @param fontSize The size of glyphs to use when painting the text. This
 * may be [TextUnit.Inherit] for inheriting from another [TextStyle].
 * @param fontWeight The typeface thickness to use when painting the text (e.g., bold).
 * @param fontStyle The typeface variant to use when drawing the letters (e.g., italic).
 * @param fontSynthesis Whether to synthesize font weight and/or style when the requested weight or
 *  style cannot be found in the provided custom font family.
 * @param fontFamily The font family to be used when rendering the text.
 * @param fontFeatureSettings The advanced typography settings provided by font. The format is the
 *  same as the CSS font-feature-settings attribute:
 *  https://www.w3.org/TR/css-fonts-3/#font-feature-settings-prop
 * @param letterSpacing The amount of space to add between each letter.
 * @param baselineShift The amount by which the text is shifted up from the current baseline.
 * @param textGeometricTransform The geometric transformation applied the text.
 * @param localeList The locale list used to select region-specific glyphs.
 * @param background The background color for the text.
 * @param textDecoration The decorations to paint on the text (e.g., an underline).
 * @param shadow The shadow effect applied on the text.
 * @param textAlign The alignment of the text within the lines of the paragraph.
 * @param textDirection The algorithm to be used to resolve the final text and paragraph
 * direction: Left To Right or Right To Left. If no value is provided the system will use the
 * [LayoutDirection] as the primary signal.
 * @param textIndent The indentation of the paragraph.
 * @param lineHeight Line height for the [Paragraph] in [TextUnit] unit, e.g. SP or EM.
 *
 * @see AnnotatedString
 * @see SpanStyle
 * @see ParagraphStyle
 */
@Immutable
data class TextStyle(
    val color: Color = Color.Unset,
    val fontSize: TextUnit = TextUnit.Inherit,
    val fontWeight: FontWeight? = null,
    val fontStyle: FontStyle? = null,
    val fontSynthesis: FontSynthesis? = null,
    val fontFamily: FontFamily? = null,
    val fontFeatureSettings: String? = null,
    val letterSpacing: TextUnit = TextUnit.Inherit,
    val baselineShift: BaselineShift? = null,
    val textGeometricTransform: TextGeometricTransform? = null,
    val localeList: LocaleList? = null,
    val background: Color = Color.Unset,
    val textDecoration: TextDecoration? = null,
    val shadow: Shadow? = null,
    val textAlign: TextAlign? = null,
    val textDirection: TextDirection? = null,
    val lineHeight: TextUnit = TextUnit.Inherit,
    val textIndent: TextIndent? = null
) {
    internal constructor(spanStyle: SpanStyle, paragraphStyle: ParagraphStyle) : this (
        color = spanStyle.color,
        fontSize = spanStyle.fontSize,
        fontWeight = spanStyle.fontWeight,
        fontStyle = spanStyle.fontStyle,
        fontSynthesis = spanStyle.fontSynthesis,
        fontFamily = spanStyle.fontFamily,
        fontFeatureSettings = spanStyle.fontFeatureSettings,
        letterSpacing = spanStyle.letterSpacing,
        baselineShift = spanStyle.baselineShift,
        textGeometricTransform = spanStyle.textGeometricTransform,
        localeList = spanStyle.localeList,
        background = spanStyle.background,
        textDecoration = spanStyle.textDecoration,
        shadow = spanStyle.shadow,
        textAlign = paragraphStyle.textAlign,
        textDirection = paragraphStyle.textDirection,
        lineHeight = paragraphStyle.lineHeight,
        textIndent = paragraphStyle.textIndent
    )

    init {
        if (lineHeight != TextUnit.Inherit) {
            // Since we are checking if it's negative, no need to convert Sp into Px at this point.
            check(lineHeight.value >= 0f) {
                "lineHeight can't be negative (${lineHeight.value})"
            }
        }
    }

    @Stable
    fun toSpanStyle(): SpanStyle = SpanStyle(
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        fontStyle = fontStyle,
        fontSynthesis = fontSynthesis,
        fontFamily = fontFamily,
        fontFeatureSettings = fontFeatureSettings,
        letterSpacing = letterSpacing,
        baselineShift = baselineShift,
        textGeometricTransform = textGeometricTransform,
        localeList = localeList,
        background = background,
        textDecoration = textDecoration,
        shadow = shadow
    )

    @Stable
    fun toParagraphStyle(): ParagraphStyle = ParagraphStyle(
        textAlign = textAlign,
        textDirection = textDirection,
        lineHeight = lineHeight,
        textIndent = textIndent
    )

    /**
     * Returns a new text style that is a combination of this style and the given [other] style.
     *
     * [other] text style's null or inherit properties are replaced with the non-null properties of
     * this text style. Another way to think of it is that the "missing" properties of the [other]
     * style are _filled_ by the properties of this style.
     *
     * If the given text style is null, returns this text style.
     */
    @Stable
    fun merge(other: TextStyle? = null): TextStyle {
        if (other == null || other == Default) return this
        return TextStyle(
            spanStyle = toSpanStyle().merge(other.toSpanStyle()),
            paragraphStyle = toParagraphStyle().merge(other.toParagraphStyle())
        )
    }

    /**
     * Returns a new text style that is a combination of this style and the given [other] style.
     *
     * @see merge
     */
    @Stable
    fun merge(other: SpanStyle): TextStyle {
        return TextStyle(
            spanStyle = toSpanStyle().merge(other),
            paragraphStyle = toParagraphStyle()
        )
    }

    /**
     * Returns a new text style that is a combination of this style and the given [other] style.
     *
     * @see merge
     */
    @Stable
    fun merge(other: ParagraphStyle): TextStyle {
        return TextStyle(
            spanStyle = toSpanStyle(),
            paragraphStyle = toParagraphStyle().merge(other)
        )
    }

    /**
     * Plus operator overload that applies a [merge].
     */
    @Stable
    operator fun plus(other: TextStyle): TextStyle = this.merge(other)

    /**
     * Plus operator overload that applies a [merge].
     */
    @Stable
    operator fun plus(other: ParagraphStyle): TextStyle = this.merge(other)

    /**
     * Plus operator overload that applies a [merge].
     */
    @Stable
    operator fun plus(other: SpanStyle): TextStyle = this.merge(other)

    companion object {
        /**
         * Constant for default text style.
         */
        @Stable
        val Default = TextStyle()
    }
}

/**
 * Interpolate between two text styles.
 *
 * This will not work well if the styles don't set the same fields.
 *
 * The [fraction] argument represents position on the timeline, with 0.0 meaning
 * that the interpolation has not started, returning [start] (or something
 * equivalent to [start]), 1.0 meaning that the interpolation has finished,
 * returning [stop] (or something equivalent to [stop]), and values in between
 * meaning that the interpolation is at the relevant point on the timeline
 * between [start] and [stop]. The interpolation can be extrapolated beyond 0.0 and
 * 1.0, so negative values and values greater than 1.0 are valid.
 */
fun lerp(start: TextStyle, stop: TextStyle, fraction: Float): TextStyle {
    return TextStyle(
        spanStyle = lerp(start.toSpanStyle(), stop.toSpanStyle(), fraction),
        paragraphStyle = lerp(start.toParagraphStyle(), stop.toParagraphStyle(), fraction)
    )
}

/**
 * Fills missing values in TextStyle with default values and resolve [TextDirection].
 *
 * This function will fill all null or [TextUnit.Inherit] field with actual values.
 * @param style a text style to be resolved
 * @param direction a layout direction to be used for resolving text layout direction algorithm
 * @return resolved text style.
 */
fun resolveDefaults(style: TextStyle, direction: LayoutDirection) = TextStyle(
    color = style.color.useOrElse { DefaultColor },
    fontSize = if (style.fontSize == TextUnit.Inherit) DefaultFontSize else style.fontSize,
    fontWeight = style.fontWeight ?: FontWeight.Normal,
    fontStyle = style.fontStyle ?: FontStyle.Normal,
    fontSynthesis = style.fontSynthesis ?: FontSynthesis.All,
    fontFamily = style.fontFamily ?: FontFamily.Default,
    fontFeatureSettings = style.fontFeatureSettings ?: "",
    letterSpacing = if (style.letterSpacing.isInherit) {
        DefaultLetterSpacing
    } else {
        style.letterSpacing
    },
    baselineShift = style.baselineShift ?: BaselineShift.None,
    textGeometricTransform = style.textGeometricTransform ?: TextGeometricTransform.None,
    localeList = style.localeList ?: LocaleList.current,
    background = style.background.useOrElse { DefaultBackgroundColor },
    textDecoration = style.textDecoration ?: TextDecoration.None,
    shadow = style.shadow ?: Shadow.None,
    textAlign = style.textAlign ?: TextAlign.Start,
    textDirection = resolveTextDirection(direction, style.textDirection),
    lineHeight = if (style.lineHeight.isInherit) DefaultLineHeight else style.lineHeight,
    textIndent = style.textIndent ?: TextIndent.None
)

/**
 * If [textDirection] is null returns a [TextDirection] based on
 * [layoutDirection].
 */
@VisibleForTesting
internal fun resolveTextDirection(
    layoutDirection: LayoutDirection,
    textDirection: TextDirection?
): TextDirection {
    return when (textDirection) {
        TextDirection.Content -> when (layoutDirection) {
            LayoutDirection.Ltr -> TextDirection.ContentOrLtr
            LayoutDirection.Rtl -> TextDirection.ContentOrRtl
        }
        null -> when (layoutDirection) {
            LayoutDirection.Ltr -> TextDirection.Ltr
            LayoutDirection.Rtl -> TextDirection.Rtl
        }
        else -> textDirection
    }
}