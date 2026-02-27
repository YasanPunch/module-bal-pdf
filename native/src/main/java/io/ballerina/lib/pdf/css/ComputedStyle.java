/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.lib.pdf.css;

import io.ballerina.lib.pdf.util.CssValueParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolved CSS styles for a single element after cascade, specificity, and inheritance.
 * Provides typed accessors for commonly used properties.
 * 
 * This is a property bag with typed accessors for commonly used properties.
 * Also, every dimension accessor calls .toPoints() to convert CSS units
 * (px, pt, em, %, mm, etc.) to points (1/72 inch).
 */
public class ComputedStyle {

    private final Map<String, String> properties = new HashMap<>();

    // Properties that inherit from parent by default (CSS spec)
    private static final Set<String> INHERITED_PROPERTIES = Set.of(
            "color", "font-family", "font-size", "font-weight", "font-style",
            "text-align", "text-transform", "text-decoration", "line-height",
            "letter-spacing", "word-spacing", "white-space", "visibility",
            "list-style-type", "list-style-position", "border-collapse",
            "border-spacing"
    );

    /** Sets a CSS property value. */
    public void set(String property, String value) {
        properties.put(property, value);
    }

    /** Returns the value of a CSS property, or null if not set. */
    public String get(String property) {
        return properties.get(property);
    }

    /** Returns the value of a CSS property, or the default if not set. */
    public String get(String property, String defaultValue) {
        return properties.getOrDefault(property, defaultValue);
    }

    /** Returns all CSS properties as a map. */
    public Map<String, String> getAll() {
        return properties;
    }

    /**
     * Returns true if this property should be inherited from the parent.
     */
    public static boolean isInherited(String property) {
        return INHERITED_PROPERTIES.contains(property);
    }

    /** Returns the set of CSS properties that inherit by default. */
    public static Set<String> getInheritedProperties() {
        return INHERITED_PROPERTIES;
    }

    // --- Typed accessors ---

    /** Returns the display property value. */
    public String getDisplay() {
        return get("display", "inline");
    }

    /** Returns the font-family property value. */
    public String getFontFamily() {
        return get("font-family");
    }

    /** Returns the font size in points, resolving relative values. */
    public float getFontSize(float parentFontSize) {
        String val = get("font-size");
        if (val == null) {
            return parentFontSize;
        }
        return CssValueParser.parseFontSize(val, parentFontSize);
    }

    /** Returns whether the font weight is bold. */
    public boolean isBold() {
        return CssValueParser.isBold(get("font-weight"));
    }

    /** Returns whether the font style is italic. */
    public boolean isItalic() {
        return CssValueParser.isItalic(get("font-style"));
    }

    /** Returns the text-align property value. */
    public String getTextAlign() {
        return get("text-align", "left");
    }

    /** Returns the text-transform property value. */
    public String getTextTransform() {
        return get("text-transform", "none");
    }

    /** Returns the vertical-align property value. */
    public String getVerticalAlign() {
        return get("vertical-align", "baseline");
    }

    /** Returns the color property value. */
    public String getColor() {
        return get("color");
    }

    /** Returns the opacity value (0.0 to 1.0). */
    public float getOpacity() {
        String val = get("opacity");
        if (val == null) {
            return 1.0f;
        }
        try {
            return Math.max(0, Math.min(1, Float.parseFloat(val.trim())));
        } catch (NumberFormatException e) {
            return 1.0f;
        }
    }

    /**
     * Parsed box-shadow values.
     *
     * @param offsetX horizontal shadow offset in points
     * @param offsetY vertical shadow offset in points
     * @param blur    blur radius in points
     * @param spread  spread radius in points
     * @param color   shadow color value
     */
    public record BoxShadow(float offsetX, float offsetY, float blur, float spread, String color) { }

    // Pattern to extract length values from box-shadow
    private static final Pattern SHADOW_LENGTH = Pattern.compile("-?[\\d.]+(?:px|pt|em|rem|%|mm|cm|in)?");

    // Pattern to extract color from a shadow value (hex, rgb/rgba, hsl/hsla)
    private static final Pattern COLOR_IN_SHADOW = Pattern.compile(
            "#[0-9a-fA-F]{3,8}|rgba?\\([^)]+\\)|hsla?\\([^)]+\\)");

    /**
     * Parses the box-shadow property. Returns null if none/unset.
     * Supports a single shadow: offsetX offsetY [blur [spread]] [color].
     * For multiple shadows, use {@link #getBoxShadows(float, float)}.
     */
    public BoxShadow getBoxShadow(float containerWidth, float fontSize) {
        List<BoxShadow> shadows = getBoxShadows(containerWidth, fontSize);
        return shadows.isEmpty() ? null : shadows.get(0);
    }

    /**
     * Parses the box-shadow property into a list of shadows.
     * Supports comma-separated values like "0 0 0 2px #fff, 0 0 0 4px #d69e2e".
     * Returns an empty list if none/unset.
     */
    public List<BoxShadow> getBoxShadows(float containerWidth, float fontSize) {
        String val = get("box-shadow");
        if (val == null || val.equals("none")) {
            return List.of();
        }
        val = val.trim();

        // Split on commas that are not inside parentheses (e.g. rgba(...))
        List<String> parts = splitOnTopLevelCommas(val);
        List<BoxShadow> result = new ArrayList<>();

        for (String part : parts) {
            BoxShadow shadow = parseSingleShadow(part.trim(), containerWidth, fontSize);
            if (shadow != null) {
                result.add(shadow);
            }
        }
        return result;
    }

    /**
     * Splits a string on commas that are not nested inside parentheses.
     */
    private static List<String> splitOnTopLevelCommas(String value) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (c == ',' && depth == 0) {
                parts.add(value.substring(start, i));
                start = i + 1;
            }
        }
        parts.add(value.substring(start));
        return parts;
    }

    private BoxShadow parseSingleShadow(String val, float containerWidth, float fontSize) {
        if (val.isEmpty()) {
            return null;
        }

        // Extract color first — hex colors like #d69e2e contain digits that the
        // length regex would otherwise match as spurious length values
        Matcher cm = COLOR_IN_SHADOW.matcher(val);
        String shadowColor = null;
        String lengthPart = val;
        if (cm.find()) {
            shadowColor = cm.group();
            lengthPart = val.substring(0, cm.start()) + val.substring(cm.end());
        }

        // Extract lengths from the color-free string
        Matcher m = SHADOW_LENGTH.matcher(lengthPart);
        List<String> lengths = new ArrayList<>();
        while (m.find()) {
            lengths.add(m.group());
        }

        if (lengths.size() < 2) {
            return null;
        }

        float ox = CssValueParser.toPoints(lengths.get(0), containerWidth, fontSize);
        float oy = CssValueParser.toPoints(lengths.get(1), containerWidth, fontSize);
        float blur = lengths.size() > 2 ? CssValueParser.toPoints(lengths.get(2), containerWidth, fontSize) : 0;
        float spread = lengths.size() > 3 ? CssValueParser.toPoints(lengths.get(3), containerWidth, fontSize) : 0;
        if (shadowColor == null) {
            shadowColor = "rgba(0,0,0,1)";
        }

        return new BoxShadow(ox, oy, blur, spread, shadowColor);
    }

    /**
     * Returns the computed line-height in points.
     * Supports: "normal" (returns -1), unitless multiplier (1.8),
     * px, em, pt, %, and "inherit"/"initial" (returns -1).
     * A return value of -1 means the caller should use font metrics.
     */
    public float getLineHeight(float fontSize) {
        String val = get("line-height");
        if (val == null || "normal".equals(val) || "inherit".equals(val) || "initial".equals(val)) {
            return -1;
        }
        val = val.trim();
        if (val.endsWith("%")) {
            try {
                return Float.parseFloat(val.replace("%", "")) / 100f * fontSize;
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        if (val.endsWith("px") || val.endsWith("pt") || val.endsWith("em")) {
            return CssValueParser.toPoints(val, 0, fontSize);
        }
        // Unitless number: "1.8" → 1.8 * fontSize
        try {
            return Float.parseFloat(val) * fontSize;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** Returns the letter-spacing in points. */
    public float getLetterSpacing(float fontSize) {
        String val = get("letter-spacing");
        if (val == null || val.equals("normal")) {
            return 0;
        }
        return CssValueParser.toPoints(val, 0, fontSize);
    }

    /** Returns the word-spacing in points. */
    public float getWordSpacing(float fontSize) {
        String val = get("word-spacing");
        if (val == null || val.equals("normal")) {
            return 0;
        }
        return CssValueParser.toPoints(val, 0, fontSize);
    }

    /** Returns the background-color property value. */
    public String getBackgroundColor() {
        return get("background-color");
    }

    /** Returns the background-image property value. */
    public String getBackgroundImage() {
        return get("background-image");
    }

    /** Returns the width in points, or -1 for auto. */
    public float getWidth(float containerWidth, float fontSize) {
        String val = get("width");
        if (val == null || val.equals("auto")) {
            return -1;
        }
        return CssValueParser.toPoints(val, containerWidth, fontSize);
    }

    /** Returns the height in points, or -1 for auto. */
    public float getHeight(float containerHeight, float fontSize) {
        String val = get("height");
        if (val == null || val.equals("auto")) {
            return -1;
        }
        return CssValueParser.toPoints(val, containerHeight, fontSize);
    }

    /** Returns the max-width in points, or MAX_VALUE for none. */
    public float getMaxWidth(float containerWidth, float fontSize) {
        String val = get("max-width");
        if (val == null || val.equals("none")) {
            return Float.MAX_VALUE;
        }
        return CssValueParser.toPoints(val, containerWidth, fontSize);
    }

    /** Returns the min-width in points. */
    public float getMinWidth(float containerWidth, float fontSize) {
        String val = get("min-width");
        if (val == null || val.equals("auto")) {
            return 0;
        }
        return CssValueParser.toPoints(val, containerWidth, fontSize);
    }

    /** Returns the max-height in points, or MAX_VALUE for none. */
    public float getMaxHeight(float containerHeight, float fontSize) {
        String val = get("max-height");
        if (val == null || val.equals("none")) {
            return Float.MAX_VALUE;
        }
        return CssValueParser.toPoints(val, containerHeight, fontSize);
    }

    /** Returns the min-height in points. */
    public float getMinHeight(float containerHeight, float fontSize) {
        String val = get("min-height");
        if (val == null || val.equals("auto")) {
            return 0;
        }
        return CssValueParser.toPoints(val, containerHeight, fontSize);
    }

    /** Returns the top margin in points. */
    public float getMarginTop(float containerWidth, float fontSize) {
        return CssValueParser.toPoints(get("margin-top"), containerWidth, fontSize);
    }

    /** Returns the right margin in points. */
    public float getMarginRight(float containerWidth, float fontSize) {
        return CssValueParser.toPoints(get("margin-right"), containerWidth, fontSize);
    }

    /** Returns the bottom margin in points. */
    public float getMarginBottom(float containerWidth, float fontSize) {
        return CssValueParser.toPoints(get("margin-bottom"), containerWidth, fontSize);
    }

    /** Returns the left margin in points. */
    public float getMarginLeft(float containerWidth, float fontSize) {
        return CssValueParser.toPoints(get("margin-left"), containerWidth, fontSize);
    }

    /** Returns whether the left margin is auto. */
    public boolean isMarginLeftAuto() {
        String val = get("margin-left");
        return val != null && val.trim().equalsIgnoreCase("auto");
    }

    /** Returns whether the right margin is auto. */
    public boolean isMarginRightAuto() {
        String val = get("margin-right");
        return val != null && val.trim().equalsIgnoreCase("auto");
    }

    /** Returns the top padding in points. */
    public float getPaddingTop(float containerWidth, float fontSize) {
        return CssValueParser.toPoints(get("padding-top"), containerWidth, fontSize);
    }

    /** Returns the right padding in points. */
    public float getPaddingRight(float containerWidth, float fontSize) {
        return CssValueParser.toPoints(get("padding-right"), containerWidth, fontSize);
    }

    /** Returns the bottom padding in points. */
    public float getPaddingBottom(float containerWidth, float fontSize) {
        return CssValueParser.toPoints(get("padding-bottom"), containerWidth, fontSize);
    }

    /** Returns the left padding in points. */
    public float getPaddingLeft(float containerWidth, float fontSize) {
        return CssValueParser.toPoints(get("padding-left"), containerWidth, fontSize);
    }

    /** Returns the top border width in points. */
    public float getBorderTopWidth(float containerWidth, float fontSize) {
        String style = getBorderTopStyle();
        if ("none".equals(style) || "hidden".equals(style)) {
            return 0;
        }
        return parseBorderWidth(get("border-top-width"), containerWidth, fontSize);
    }

    /** Returns the right border width in points. */
    public float getBorderRightWidth(float containerWidth, float fontSize) {
        String style = getBorderRightStyle();
        if ("none".equals(style) || "hidden".equals(style)) {
            return 0;
        }
        return parseBorderWidth(get("border-right-width"), containerWidth, fontSize);
    }

    /** Returns the bottom border width in points. */
    public float getBorderBottomWidth(float containerWidth, float fontSize) {
        String style = getBorderBottomStyle();
        if ("none".equals(style) || "hidden".equals(style)) {
            return 0;
        }
        return parseBorderWidth(get("border-bottom-width"), containerWidth, fontSize);
    }

    /** Returns the left border width in points. */
    public float getBorderLeftWidth(float containerWidth, float fontSize) {
        String style = getBorderLeftStyle();
        if ("none".equals(style) || "hidden".equals(style)) {
            return 0;
        }
        return parseBorderWidth(get("border-left-width"), containerWidth, fontSize);
    }

    /** Returns the border-top-color value. */
    public String getBorderTopColor() {
        return get("border-top-color");
    }

    /** Returns the border-right-color value. */
    public String getBorderRightColor() {
        return get("border-right-color");
    }

    /** Returns the border-bottom-color value. */
    public String getBorderBottomColor() {
        return get("border-bottom-color");
    }

    /** Returns the border-left-color value. */
    public String getBorderLeftColor() {
        return get("border-left-color");
    }

    /** Returns the border-top-style value. */
    public String getBorderTopStyle() {
        return get("border-top-style", "none");
    }

    /** Returns the border-right-style value. */
    public String getBorderRightStyle() {
        return get("border-right-style", "none");
    }

    /** Returns the border-bottom-style value. */
    public String getBorderBottomStyle() {
        return get("border-bottom-style", "none");
    }

    /** Returns the border-left-style value. */
    public String getBorderLeftStyle() {
        return get("border-left-style", "none");
    }

    /** Returns the border-collapse value. */
    public String getBorderCollapse() {
        return get("border-collapse", "separate");
    }

    /** Returns the border-spacing in points. */
    public float getBorderSpacing(float containerWidth, float fontSize) {
        String val = get("border-spacing");
        if (val == null) {
            return 0;
        }
        return CssValueParser.toPoints(val, containerWidth, fontSize);
    }

    /** Returns the border-top-left-radius in points. */
    public float getBorderTopLeftRadius(float containerWidth, float fontSize) {
        String val = get("border-top-left-radius");
        if (val == null || val.equals("0")) {
            return 0;
        }
        return CssValueParser.toPoints(val, containerWidth, fontSize);
    }

    /** Returns the border-top-right-radius in points. */
    public float getBorderTopRightRadius(float containerWidth, float fontSize) {
        String val = get("border-top-right-radius");
        if (val == null || val.equals("0")) {
            return 0;
        }
        return CssValueParser.toPoints(val, containerWidth, fontSize);
    }

    /** Returns the border-bottom-right-radius in points. */
    public float getBorderBottomRightRadius(float containerWidth, float fontSize) {
        String val = get("border-bottom-right-radius");
        if (val == null || val.equals("0")) {
            return 0;
        }
        return CssValueParser.toPoints(val, containerWidth, fontSize);
    }

    /** Returns the border-bottom-left-radius in points. */
    public float getBorderBottomLeftRadius(float containerWidth, float fontSize) {
        String val = get("border-bottom-left-radius");
        if (val == null || val.equals("0")) {
            return 0;
        }
        return CssValueParser.toPoints(val, containerWidth, fontSize);
    }

    /** Returns the position property value. */
    public String getPosition() {
        return get("position", "static");
    }

    /** Returns the float property value. */
    public String getFloat() {
        return get("float", "none");
    }

    /** Returns the clear property value. */
    public String getClear() {
        return get("clear", "none");
    }

    /** Returns the top offset in points, or NaN for auto. */
    public float getTop(float containerSize, float fontSize) {
        String val = get("top");
        if (val == null || val.equals("auto")) {
            return Float.NaN;
        }
        return CssValueParser.toPoints(val, containerSize, fontSize);
    }

    /** Returns the left offset in points, or NaN for auto. */
    public float getLeft(float containerSize, float fontSize) {
        String val = get("left");
        if (val == null || val.equals("auto")) {
            return Float.NaN;
        }
        return CssValueParser.toPoints(val, containerSize, fontSize);
    }

    /** Returns the right offset in points, or NaN for auto. */
    public float getRight(float containerSize, float fontSize) {
        String val = get("right");
        if (val == null || val.equals("auto")) {
            return Float.NaN;
        }
        return CssValueParser.toPoints(val, containerSize, fontSize);
    }

    /** Returns the bottom offset in points, or NaN for auto. */
    public float getBottom(float containerSize, float fontSize) {
        String val = get("bottom");
        if (val == null || val.equals("auto")) {
            return Float.NaN;
        }
        return CssValueParser.toPoints(val, containerSize, fontSize);
    }

    private float parseBorderWidth(String val, float containerWidth, float fontSize) {
        if (val == null || val.equals("none") || val.equals("0")) {
            return 0;
        }
        if (val.equals("thin")) {
            return 0.75f;
        }
        if (val.equals("medium")) {
            return 1.5f;
        }
        if (val.equals("thick")) {
            return 2.25f;
        }
        return CssValueParser.toPoints(val, containerWidth, fontSize);
    }
}
