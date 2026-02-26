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

package io.ballerina.lib.pdf;

import java.util.Map;

/**
 * Configuration options for HTML-to-PDF conversion.
 */
public class ConversionOptions {

    /** CSS-spec default: medium = 16px = 12pt */
    public static final float DEFAULT_FALLBACK_FONT_SIZE = 12f;

    // A4 dimensions in points (210mm x 297mm)
    public static final float A4_WIDTH = 595.276f;
    public static final float A4_HEIGHT = 841.89f;

    // US Letter dimensions in points (8.5" x 11")
    public static final float LETTER_WIDTH = 612f;
    public static final float LETTER_HEIGHT = 792f;

    // US Legal dimensions in points (8.5" x 14")
    public static final float LEGAL_WIDTH = 612f;
    public static final float LEGAL_HEIGHT = 1008f;

    // Default margin: 0pt (no page margin; CSS controls spacing)
    public static final float DEFAULT_MARGIN = 0f;

    // BMap field keys — must match Ballerina record field names in types.bal
    public static final String KEY_FALLBACK_FONT_SIZE = "fallbackFontSize";
    public static final String KEY_PAGE_SIZE = "pageSize";
    public static final String KEY_ADDITIONAL_CSS = "additionalCss";
    public static final String KEY_MAX_PAGES = "maxPages";
    public static final String KEY_CUSTOM_FONTS = "customFonts";
    public static final String KEY_MARGINS = "margins";

    // PageMargins record field keys
    public static final String KEY_MARGIN_TOP = "top";
    public static final String KEY_MARGIN_RIGHT = "right";
    public static final String KEY_MARGIN_BOTTOM = "bottom";
    public static final String KEY_MARGIN_LEFT = "left";

    private final float fallbackFontSize;
    private final float pageWidth;
    private final float pageHeight;
    private final float marginTop;
    private final float marginRight;
    private final float marginBottom;
    private final float marginLeft;
    private final String additionalCss;
    private final Map<String, byte[]> customFonts;
    private final Integer maxPages;

    /** Full constructor with all options. */
    public ConversionOptions(float fallbackFontSize,
                            float pageWidth, float pageHeight,
                            float marginTop, float marginRight,
                            float marginBottom, float marginLeft,
                            String additionalCss,
                            Map<String, byte[]> customFonts, Integer maxPages) {
        if (fallbackFontSize <= 0) {
            throw new IllegalArgumentException("fallbackFontSize must be positive, got: " + fallbackFontSize);
        }
        if (pageWidth <= 0) {
            throw new IllegalArgumentException("pageWidth must be positive, got: " + pageWidth);
        }
        if (pageHeight <= 0) {
            throw new IllegalArgumentException("pageHeight must be positive, got: " + pageHeight);
        }
        if (marginTop < 0) {
            throw new IllegalArgumentException("marginTop must be non-negative, got: " + marginTop);
        }
        if (marginRight < 0) {
            throw new IllegalArgumentException("marginRight must be non-negative, got: " + marginRight);
        }
        if (marginBottom < 0) {
            throw new IllegalArgumentException("marginBottom must be non-negative, got: " + marginBottom);
        }
        if (marginLeft < 0) {
            throw new IllegalArgumentException("marginLeft must be non-negative, got: " + marginLeft);
        }
        this.fallbackFontSize = fallbackFontSize;
        this.pageWidth = pageWidth;
        this.pageHeight = pageHeight;
        this.marginTop = marginTop;
        this.marginRight = marginRight;
        this.marginBottom = marginBottom;
        this.marginLeft = marginLeft;
        this.additionalCss = additionalCss;
        this.customFonts = customFonts;
        this.maxPages = maxPages;
    }

    /** Resolves page dimensions from a page size name (A4, LETTER, LEGAL). */
    public static float[] pageDimensions(String pageSize) {
        return switch (pageSize.toUpperCase()) {
            case "LETTER" -> new float[]{LETTER_WIDTH, LETTER_HEIGHT};
            case "LEGAL" -> new float[]{LEGAL_WIDTH, LEGAL_HEIGHT};
            default -> new float[]{A4_WIDTH, A4_HEIGHT};
        };
    }

    public float getFallbackFontSize() { return fallbackFontSize; }
    public float getPageWidth() { return pageWidth; }
    public float getPageHeight() { return pageHeight; }
    public float getMarginTop() { return marginTop; }
    public float getMarginRight() { return marginRight; }
    public float getMarginBottom() { return marginBottom; }
    public float getMarginLeft() { return marginLeft; }
    public String getAdditionalCss() { return additionalCss; }
    public Map<String, byte[]> getCustomFonts() { return customFonts; }
    public Integer getMaxPages() { return maxPages; }

}
