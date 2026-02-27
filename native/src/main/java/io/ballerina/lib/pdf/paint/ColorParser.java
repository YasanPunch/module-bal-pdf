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

package io.ballerina.lib.pdf.paint;

import java.awt.Color;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses CSS color values: #hex, rgb(), named colors → java.awt.Color.
 */
public class ColorParser {

    private static final Pattern HEX3 = Pattern.compile("#([0-9a-fA-F])([0-9a-fA-F])([0-9a-fA-F])");
    private static final Pattern HEX6 = Pattern.compile("#([0-9a-fA-F]{2})([0-9a-fA-F]{2})([0-9a-fA-F]{2})");
    private static final Pattern RGB = Pattern.compile("rgb\\s*\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\)");
    private static final Pattern RGBA = Pattern.compile(
            "rgba\\s*\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*([\\d.]+)\\s*\\)");

    // All 148 CSS named colors (147 keywords + transparent) per CSS Color Module Level 4.
    private static final Map<String, Color> NAMED_COLORS = Map.ofEntries(
            Map.entry("aliceblue", new Color(240, 248, 255)),
            Map.entry("antiquewhite", new Color(250, 235, 215)),
            Map.entry("aqua", new Color(0, 255, 255)),
            Map.entry("aquamarine", new Color(127, 255, 212)),
            Map.entry("azure", new Color(240, 255, 255)),
            Map.entry("beige", new Color(245, 245, 220)),
            Map.entry("bisque", new Color(255, 228, 196)),
            Map.entry("black", new Color(0, 0, 0)),
            Map.entry("blanchedalmond", new Color(255, 235, 205)),
            Map.entry("blue", new Color(0, 0, 255)),
            Map.entry("blueviolet", new Color(138, 43, 226)),
            Map.entry("brown", new Color(165, 42, 42)),
            Map.entry("burlywood", new Color(222, 184, 135)),
            Map.entry("cadetblue", new Color(95, 158, 160)),
            Map.entry("chartreuse", new Color(127, 255, 0)),
            Map.entry("chocolate", new Color(210, 105, 30)),
            Map.entry("coral", new Color(255, 127, 80)),
            Map.entry("cornflowerblue", new Color(100, 149, 237)),
            Map.entry("cornsilk", new Color(255, 248, 220)),
            Map.entry("crimson", new Color(220, 20, 60)),
            Map.entry("cyan", new Color(0, 255, 255)),
            Map.entry("darkblue", new Color(0, 0, 139)),
            Map.entry("darkcyan", new Color(0, 139, 139)),
            Map.entry("darkgoldenrod", new Color(184, 134, 11)),
            Map.entry("darkgray", new Color(169, 169, 169)),
            Map.entry("darkgreen", new Color(0, 100, 0)),
            Map.entry("darkgrey", new Color(169, 169, 169)),
            Map.entry("darkkhaki", new Color(189, 183, 107)),
            Map.entry("darkmagenta", new Color(139, 0, 139)),
            Map.entry("darkolivegreen", new Color(85, 107, 47)),
            Map.entry("darkorange", new Color(255, 140, 0)),
            Map.entry("darkorchid", new Color(153, 50, 204)),
            Map.entry("darkred", new Color(139, 0, 0)),
            Map.entry("darksalmon", new Color(233, 150, 122)),
            Map.entry("darkseagreen", new Color(143, 188, 143)),
            Map.entry("darkslateblue", new Color(72, 61, 139)),
            Map.entry("darkslategray", new Color(47, 79, 79)),
            Map.entry("darkslategrey", new Color(47, 79, 79)),
            Map.entry("darkturquoise", new Color(0, 206, 209)),
            Map.entry("darkviolet", new Color(148, 0, 211)),
            Map.entry("deeppink", new Color(255, 20, 147)),
            Map.entry("deepskyblue", new Color(0, 191, 255)),
            Map.entry("dimgray", new Color(105, 105, 105)),
            Map.entry("dimgrey", new Color(105, 105, 105)),
            Map.entry("dodgerblue", new Color(30, 144, 255)),
            Map.entry("firebrick", new Color(178, 34, 34)),
            Map.entry("floralwhite", new Color(255, 250, 240)),
            Map.entry("forestgreen", new Color(34, 139, 34)),
            Map.entry("fuchsia", new Color(255, 0, 255)),
            Map.entry("gainsboro", new Color(220, 220, 220)),
            Map.entry("ghostwhite", new Color(248, 248, 255)),
            Map.entry("gold", new Color(255, 215, 0)),
            Map.entry("goldenrod", new Color(218, 165, 32)),
            Map.entry("gray", new Color(128, 128, 128)),
            Map.entry("green", new Color(0, 128, 0)),
            Map.entry("greenyellow", new Color(173, 255, 47)),
            Map.entry("grey", new Color(128, 128, 128)),
            Map.entry("honeydew", new Color(240, 255, 240)),
            Map.entry("hotpink", new Color(255, 105, 180)),
            Map.entry("indianred", new Color(205, 92, 92)),
            Map.entry("indigo", new Color(75, 0, 130)),
            Map.entry("ivory", new Color(255, 255, 240)),
            Map.entry("khaki", new Color(240, 230, 140)),
            Map.entry("lavender", new Color(230, 230, 250)),
            Map.entry("lavenderblush", new Color(255, 240, 245)),
            Map.entry("lawngreen", new Color(124, 252, 0)),
            Map.entry("lemonchiffon", new Color(255, 250, 205)),
            Map.entry("lightblue", new Color(173, 216, 230)),
            Map.entry("lightcoral", new Color(240, 128, 128)),
            Map.entry("lightcyan", new Color(224, 255, 255)),
            Map.entry("lightgoldenrodyellow", new Color(250, 250, 210)),
            Map.entry("lightgray", new Color(211, 211, 211)),
            Map.entry("lightgreen", new Color(144, 238, 144)),
            Map.entry("lightgrey", new Color(211, 211, 211)),
            Map.entry("lightpink", new Color(255, 182, 193)),
            Map.entry("lightsalmon", new Color(255, 160, 122)),
            Map.entry("lightseagreen", new Color(32, 178, 170)),
            Map.entry("lightskyblue", new Color(135, 206, 250)),
            Map.entry("lightslategray", new Color(119, 136, 153)),
            Map.entry("lightslategrey", new Color(119, 136, 153)),
            Map.entry("lightsteelblue", new Color(176, 196, 222)),
            Map.entry("lightyellow", new Color(255, 255, 224)),
            Map.entry("lime", new Color(0, 255, 0)),
            Map.entry("limegreen", new Color(50, 205, 50)),
            Map.entry("linen", new Color(250, 240, 230)),
            Map.entry("magenta", new Color(255, 0, 255)),
            Map.entry("maroon", new Color(128, 0, 0)),
            Map.entry("mediumaquamarine", new Color(102, 205, 170)),
            Map.entry("mediumblue", new Color(0, 0, 205)),
            Map.entry("mediumorchid", new Color(186, 85, 211)),
            Map.entry("mediumpurple", new Color(147, 112, 219)),
            Map.entry("mediumseagreen", new Color(60, 179, 113)),
            Map.entry("mediumslateblue", new Color(123, 104, 238)),
            Map.entry("mediumspringgreen", new Color(0, 250, 154)),
            Map.entry("mediumturquoise", new Color(72, 209, 204)),
            Map.entry("mediumvioletred", new Color(199, 21, 133)),
            Map.entry("midnightblue", new Color(25, 25, 112)),
            Map.entry("mintcream", new Color(245, 255, 250)),
            Map.entry("mistyrose", new Color(255, 228, 225)),
            Map.entry("moccasin", new Color(255, 228, 181)),
            Map.entry("navajowhite", new Color(255, 222, 173)),
            Map.entry("navy", new Color(0, 0, 128)),
            Map.entry("oldlace", new Color(253, 245, 230)),
            Map.entry("olive", new Color(128, 128, 0)),
            Map.entry("olivedrab", new Color(107, 142, 35)),
            Map.entry("orange", new Color(255, 165, 0)),
            Map.entry("orangered", new Color(255, 69, 0)),
            Map.entry("orchid", new Color(218, 112, 214)),
            Map.entry("palegoldenrod", new Color(238, 232, 170)),
            Map.entry("palegreen", new Color(152, 251, 152)),
            Map.entry("paleturquoise", new Color(175, 238, 238)),
            Map.entry("palevioletred", new Color(219, 112, 147)),
            Map.entry("papayawhip", new Color(255, 239, 213)),
            Map.entry("peachpuff", new Color(255, 218, 185)),
            Map.entry("peru", new Color(205, 133, 63)),
            Map.entry("pink", new Color(255, 192, 203)),
            Map.entry("plum", new Color(221, 160, 221)),
            Map.entry("powderblue", new Color(176, 224, 230)),
            Map.entry("purple", new Color(128, 0, 128)),
            Map.entry("rebeccapurple", new Color(102, 51, 153)),
            Map.entry("red", new Color(255, 0, 0)),
            Map.entry("rosybrown", new Color(188, 143, 143)),
            Map.entry("royalblue", new Color(65, 105, 225)),
            Map.entry("saddlebrown", new Color(139, 69, 19)),
            Map.entry("salmon", new Color(250, 128, 114)),
            Map.entry("sandybrown", new Color(244, 164, 96)),
            Map.entry("seagreen", new Color(46, 139, 87)),
            Map.entry("seashell", new Color(255, 245, 238)),
            Map.entry("sienna", new Color(160, 82, 45)),
            Map.entry("silver", new Color(192, 192, 192)),
            Map.entry("skyblue", new Color(135, 206, 235)),
            Map.entry("slateblue", new Color(106, 90, 205)),
            Map.entry("slategray", new Color(112, 128, 144)),
            Map.entry("slategrey", new Color(112, 128, 144)),
            Map.entry("snow", new Color(255, 250, 250)),
            Map.entry("springgreen", new Color(0, 255, 127)),
            Map.entry("steelblue", new Color(70, 130, 180)),
            Map.entry("tan", new Color(210, 180, 140)),
            Map.entry("teal", new Color(0, 128, 128)),
            Map.entry("thistle", new Color(216, 191, 216)),
            Map.entry("tomato", new Color(255, 99, 71)),
            Map.entry("transparent", new Color(0, 0, 0, 0)),
            Map.entry("turquoise", new Color(64, 224, 208)),
            Map.entry("violet", new Color(238, 130, 238)),
            Map.entry("wheat", new Color(245, 222, 179)),
            Map.entry("white", new Color(255, 255, 255)),
            Map.entry("whitesmoke", new Color(245, 245, 245)),
            Map.entry("yellow", new Color(255, 255, 0)),
            Map.entry("yellowgreen", new Color(154, 205, 50))
    );

    /**
     * Parses a CSS color string. Returns null if unparseable.
     */
    public static Color parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        value = value.trim().toLowerCase();

        // Named color
        Color named = NAMED_COLORS.get(value);
        if (named != null) {
            return named;
        }

        // #RRGGBB
        Matcher m6 = HEX6.matcher(value);
        if (m6.matches()) {
            return new Color(
                    Integer.parseInt(m6.group(1), 16),
                    Integer.parseInt(m6.group(2), 16),
                    Integer.parseInt(m6.group(3), 16)
            );
        }

        // #RGB
        Matcher m3 = HEX3.matcher(value);
        if (m3.matches()) {
            return new Color(
                    Integer.parseInt(m3.group(1) + m3.group(1), 16),
                    Integer.parseInt(m3.group(2) + m3.group(2), 16),
                    Integer.parseInt(m3.group(3) + m3.group(3), 16)
            );
        }

        // rgb(r, g, b)
        Matcher mRgb = RGB.matcher(value);
        if (mRgb.matches()) {
            return new Color(
                    clamp(Integer.parseInt(mRgb.group(1))),
                    clamp(Integer.parseInt(mRgb.group(2))),
                    clamp(Integer.parseInt(mRgb.group(3)))
            );
        }

        // rgba(r, g, b, a)
        Matcher mRgba = RGBA.matcher(value);
        if (mRgba.matches()) {
            float alpha = Float.parseFloat(mRgba.group(4));
            return new Color(
                    clamp(Integer.parseInt(mRgba.group(1))),
                    clamp(Integer.parseInt(mRgba.group(2))),
                    clamp(Integer.parseInt(mRgba.group(3))),
                    clamp((int) (alpha * 255))
            );
        }

        return null;
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
