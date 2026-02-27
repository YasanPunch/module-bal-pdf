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

import io.ballerina.lib.pdf.ConversionOptions;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Loads and manages Liberation TTF fonts for PDFBox rendering.
 * Maps CSS font-family + weight + style to the correct PDType0Font.
 */
public class FontManager {

    private static final Logger LOG = LoggerFactory.getLogger(FontManager.class);

    private static final String[] FONT_FILES = {
            "fonts/LiberationSans-Regular.ttf",
            "fonts/LiberationSans-Bold.ttf",
            "fonts/LiberationSans-Italic.ttf",
            "fonts/LiberationSans-BoldItalic.ttf",
            "fonts/LiberationSerif-Regular.ttf",
            "fonts/LiberationSerif-Bold.ttf",
            "fonts/LiberationSerif-Italic.ttf",
            "fonts/LiberationSerif-BoldItalic.ttf",
    };

    private static final String SYMBOL_FONT_FILE = "fonts/NotoSansSymbols2-Regular.ttf";

    // PDFont metrics are expressed in 1/1000 of a point
    private static final float FONT_UNITS_PER_POINT = 1000f;
    private static final float FALLBACK_CHAR_WIDTH_RATIO = 0.5f;
    private static final float LEADING_FACTOR = 0.1f;
    private static final float DEFAULT_LINE_HEIGHT_RATIO = 1.2f;
    private static final float DEFAULT_ASCENT_RATIO = 0.8f;
    private static final float DEFAULT_DESCENT_RATIO = 0.2f;

    // Raw TTF bytes cached at class load to avoid repeated classpath I/O per conversion
    private static final Map<String, byte[]> FONT_BYTE_CACHE;

    static {
        Map<String, byte[]> cache = new HashMap<>();
        for (String fontFile : FONT_FILES) {
            cacheFont(cache, fontFile);
        }
        cacheFont(cache, SYMBOL_FONT_FILE);
        FONT_BYTE_CACHE = Map.copyOf(cache);
    }

    private static void cacheFont(Map<String, byte[]> cache, String fontFile) {
        try (InputStream is = FontManager.class.getClassLoader().getResourceAsStream(fontFile)) {
            if (is != null) {
                cache.put(fontFile, is.readAllBytes());
            } else {
                LOG.warn("Font not found on classpath during cache init: {}", fontFile);
            }
        } catch (IOException e) {
            LOG.warn("Failed to cache font bytes: {}", fontFile, e);
        }
    }

    // Key format: "family|bold|italic" e.g. "liberation sans|true|false"
    private final Map<String, PDFont> fontMap = new HashMap<>();
    private final Set<String> customFamilies = new HashSet<>();
    // Fallback fonts tried when primary font can't encode a character
    private final List<PDFont> fallbackFonts = new ArrayList<>();
    private PDFont defaultFont;

    /** Loads bundled and custom fonts into the PDF document. */
    public void loadFonts(PDDocument document) throws IOException {
        for (String fontFile : FONT_FILES) {
            byte[] cached = FONT_BYTE_CACHE.get(fontFile);
            if (cached == null) {
                LOG.warn("Font not found in cache: {}", fontFile);
                continue;
            }
            PDType0Font font = PDType0Font.load(document, new ByteArrayInputStream(cached), true);
            String key = buildKey(fontFile);
            fontMap.put(key, font);
        }
        // Default font is Liberation Sans Regular
        defaultFont = fontMap.get("liberation sans|false|false");
        if (defaultFont == null && !fontMap.isEmpty()) {
            defaultFont = fontMap.values().iterator().next();
        }
        if (defaultFont == null) {
            throw new IOException("No fonts could be loaded. At least one bundled font must be available.");
        }

        // Load symbol font for glyph fallback (Dingbats, Math, Arrows, etc.)
        byte[] symbolBytes = FONT_BYTE_CACHE.get(SYMBOL_FONT_FILE);
        if (symbolBytes != null) {
            PDType0Font symbolFont = PDType0Font.load(document, new ByteArrayInputStream(symbolBytes), true);
            fallbackFonts.add(symbolFont);
        }
    }

    /**
     * Loads custom TTF fonts supplied by the user.
     * Each entry specifies the font family, content bytes, and explicit bold/italic flags.
     */
    public void loadCustomFonts(PDDocument document,
                                List<ConversionOptions.FontEntry> fonts) throws IOException {
        for (ConversionOptions.FontEntry entry : fonts) {
            PDType0Font font = PDType0Font.load(
                    document, new ByteArrayInputStream(entry.content()), true);
            String family = entry.family().toLowerCase().trim();
            String key = family + "|" + entry.bold() + "|" + entry.italic();
            fontMap.put(key, font);
            customFamilies.add(family);
        }

        // Register custom font regular variants as glyph-level fallback candidates.
        // Inserted before existing fallback fonts (e.g., Noto Symbols) so user-provided
        // fonts are tried first for mixed-script content.
        int insertPos = 0;
        for (String family : customFamilies) {
            PDFont regular = fontMap.get(family + "|false|false");
            if (regular != null) {
                fallbackFonts.add(insertPos++, regular);
            }
        }
    }

    /**
     * Resolves a CSS font-family, weight, and style to a loaded PDFont.
     * Delegates to the array-based overload for a single-family input.
     */
    public PDFont getFont(String family, boolean bold, boolean italic) {
        return getFont(new String[]{family}, bold, italic);
    }

    /**
     * Resolves a CSS font-family fallback chain to a loaded PDFont.
     * Walks the chain in order, returning the first family that maps to a loaded font.
     * Falls back to the default font only after exhausting all families.
     */
    public PDFont getFont(String[] families, boolean bold, boolean italic) {
        for (String family : families) {
            String resolved = tryResolveFamily(family);
            if (resolved == null) {
                continue;
            }
            PDFont font = lookupFont(resolved, bold, italic);
            if (font != null) {
                return font;
            }
        }
        return defaultFont;
    }

    /**
     * Attempts to resolve a single CSS family name to a loaded font family key.
     * Returns null if the family is not recognized — the caller should try the next
     * family in the chain rather than falling back to a default.
     */
    private String tryResolveFamily(String family) {
        if (family == null) {
            return null;
        }
        String lower = family.toLowerCase().trim();
        lower = lower.replace("'", "").replace("\"", "");

        // Custom fonts take priority
        if (customFamilies.contains(lower)) {
            return lower;
        }

        // Map known families — sans-serif check must precede serif check
        // because "sans-serif".contains("serif") is true
        if (lower.contains("liberation sans") || lower.contains("arial")
                || lower.contains("helvetica") || lower.equals("sans-serif")) {
            return "liberation sans";
        }
        if (lower.contains("liberation serif") || lower.contains("times")
                || lower.equals("serif")) {
            return "liberation serif";
        }
        if (lower.equals("monospace") || lower.equals("courier")) {
            return "liberation sans"; // no monospace font bundled; best available fallback
        }

        // Unrecognized family — return null so the chain walker tries the next entry
        return null;
    }

    /**
     * Looks up a font by resolved family key with bold/italic fallback cascade.
     * Returns null if no variant of the family is loaded.
     */
    private PDFont lookupFont(String resolvedFamily, boolean bold, boolean italic) {
        String key = resolvedFamily + "|" + bold + "|" + italic;
        PDFont font = fontMap.get(key);
        if (font != null) {
            return font;
        }

        // Try without italic
        if (italic) {
            font = fontMap.get(resolvedFamily + "|" + bold + "|false");
            if (font != null) {
                return font;
            }
        }
        // Try without bold
        if (bold) {
            font = fontMap.get(resolvedFamily + "|false|" + italic);
            if (font != null) {
                return font;
            }
        }
        // Try regular variant of the family
        font = fontMap.get(resolvedFamily + "|false|false");
        return font;
    }

    public PDFont getDefaultFont() {
        return defaultFont;
    }

    /**
     * Measures the width of a string in points at the given font size.
     */
    public float measureText(String text, PDFont font, float fontSize) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        try {
            return font.getStringWidth(text) / FONT_UNITS_PER_POINT * fontSize;
        } catch (Exception e) {
            return text.length() * fontSize * FALLBACK_CHAR_WIDTH_RATIO;
        }
    }

    /**
     * Returns the line height (ascent + descent + leading) for a font at a given size.
     */
    public float getLineHeight(PDFont font, float fontSize) {
        var desc = font.getFontDescriptor();
        if (desc != null) {
            float ascent = desc.getAscent() / FONT_UNITS_PER_POINT * fontSize;
            float descent = Math.abs(desc.getDescent() / FONT_UNITS_PER_POINT * fontSize);
            return ascent + descent + fontSize * LEADING_FACTOR;
        }
        return fontSize * DEFAULT_LINE_HEIGHT_RATIO;
    }

    /**
     * Returns the ascent of a font at a given size.
     * Ascent is the distance from the baseline to the top of the font.
     * Baseline is the line used to align the text.
     */
    public float getAscent(PDFont font, float fontSize) {
        var desc = font.getFontDescriptor();
        if (desc != null) {
            return desc.getAscent() / FONT_UNITS_PER_POINT * fontSize;
        }
        return fontSize * DEFAULT_ASCENT_RATIO;
    }

    /**
     * Returns the descent of a font at a given size.
     * Descent is the distance from the baseline to the bottom of the font.
     */
    public float getDescent(PDFont font, float fontSize) {
        var desc = font.getFontDescriptor();
        if (desc != null) {
            return Math.abs(desc.getDescent() / FONT_UNITS_PER_POINT * fontSize);
        }
        return fontSize * DEFAULT_DESCENT_RATIO;
    }

    /**
     * Finds a fallback font that can encode the given character.
     * Returns null if no loaded font supports the character.
     */
    public PDFont findFallbackFont(char c) {
        for (PDFont font : fallbackFonts) {
            if (canEncode(font, c)) {
                return font;
            }
        }
        return null;
    }

    /**
     * Tests whether a font can encode a character without throwing.
     */
    public static boolean canEncode(PDFont font, char c) {
        try {
            font.encode(String.valueOf(c));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String buildKey(String fontFile) {
        String name = fontFile.toLowerCase();
        String family;
        if (name.contains("liberationsans")) {
            family = "liberation sans";
        } else if (name.contains("liberationserif")) {
            family = "liberation serif";
        } else {
            family = "unknown";
        }
        boolean bold = name.contains("bold");
        boolean italic = name.contains("italic");
        return family + "|" + bold + "|" + italic;
    }

}
