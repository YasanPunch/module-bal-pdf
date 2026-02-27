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

import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.TypeTags;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * Entry point for Ballerina Java interop.
 * <p>
 * Static methods in this class are called directly from Ballerina via {@code @java:Method} bindings.
 */
public final class Native {

    private Native() {
    }

    /**
     * Converts HTML to PDF bytes.
     * <p>
     * Called from Ballerina: {@code parseHtml(string html, *ConversionOptions options)}
     *
     * @param html    HTML content as a Ballerina string
     * @param options ConversionOptions record as a BMap
     * @return byte[] (as BArray) on success, BError on failure
     */
    @SuppressWarnings("unchecked")
    public static Object parseHtml(BString html, BMap<BString, Object> options) {
        try {
            // Read options from BMap
            float fontSize = getFloat(options, ConversionOptions.KEY_FALLBACK_FONT_SIZE,
                    ConversionOptions.DEFAULT_FALLBACK_FONT_SIZE);
            String additionalCss = getString(options, ConversionOptions.KEY_ADDITIONAL_CSS, null);

            // maxPages: absent = no limit
            Integer maxPages = null;
            Object maxPagesObj = options.get(StringUtils.fromString(ConversionOptions.KEY_MAX_PAGES));
            if (maxPagesObj != null
                    && TypeUtils.getType(maxPagesObj).getTag() == TypeTags.INT_TAG) {
                maxPages = ((Long) maxPagesObj).intValue();
            }

            // Read custom fonts from Font[] array
            List<ConversionOptions.FontEntry> customFonts = null;
            Object customFontsObj = options.get(StringUtils.fromString(ConversionOptions.KEY_CUSTOM_FONTS));
            if (customFontsObj != null
                    && TypeUtils.getType(customFontsObj).getTag() == TypeTags.ARRAY_TAG) {
                BArray fontsArray = (BArray) customFontsObj;
                customFonts = new ArrayList<>();
                for (int i = 0; i < fontsArray.size(); i++) {
                    @SuppressWarnings("unchecked")
                    BMap<BString, Object> fontRecord = (BMap<BString, Object>) fontsArray.get(i);
                    String family = getString(fontRecord, ConversionOptions.KEY_FONT_FAMILY, "");
                    byte[] content = ((BArray) fontRecord.get(
                            StringUtils.fromString(ConversionOptions.KEY_FONT_CONTENT))).getBytes();
                    boolean bold = getBool(fontRecord, ConversionOptions.KEY_FONT_BOLD);
                    boolean italic = getBool(fontRecord, ConversionOptions.KEY_FONT_ITALIC);
                    customFonts.add(new ConversionOptions.FontEntry(family, content, bold, italic));
                }
            }

            // Read margins from nested PageMargins record
            float marginTop = ConversionOptions.DEFAULT_MARGIN;
            float marginRight = ConversionOptions.DEFAULT_MARGIN;
            float marginBottom = ConversionOptions.DEFAULT_MARGIN;
            float marginLeft = ConversionOptions.DEFAULT_MARGIN;

            Object marginsObj = options.get(StringUtils.fromString(ConversionOptions.KEY_MARGINS));
            if (marginsObj != null
                    && TypeUtils.getType(marginsObj).getTag() == TypeTags.RECORD_TYPE_TAG) {
                BMap<BString, Object> margins = (BMap<BString, Object>) marginsObj;
                marginTop = getFloat(margins, ConversionOptions.KEY_MARGIN_TOP,
                        ConversionOptions.DEFAULT_MARGIN);
                marginRight = getFloat(margins, ConversionOptions.KEY_MARGIN_RIGHT,
                        ConversionOptions.DEFAULT_MARGIN);
                marginBottom = getFloat(margins, ConversionOptions.KEY_MARGIN_BOTTOM,
                        ConversionOptions.DEFAULT_MARGIN);
                marginLeft = getFloat(margins, ConversionOptions.KEY_MARGIN_LEFT,
                        ConversionOptions.DEFAULT_MARGIN);
            }

            // Resolve page dimensions: string (preset) or record (custom {width, height})
            float pageWidth;
            float pageHeight;
            Object pageSizeObj = options.get(StringUtils.fromString(ConversionOptions.KEY_PAGE_SIZE));
            if (pageSizeObj != null
                    && TypeUtils.getType(pageSizeObj).getTag() == TypeTags.RECORD_TYPE_TAG) {
                @SuppressWarnings("unchecked")
                BMap<BString, Object> customSize = (BMap<BString, Object>) pageSizeObj;
                pageWidth = getFloat(customSize, "width", ConversionOptions.A4_WIDTH);
                pageHeight = getFloat(customSize, "height", ConversionOptions.A4_HEIGHT);
            } else {
                String pageSizeName = (pageSizeObj != null)
                        ? ((BString) pageSizeObj).getValue() : "A4";
                float[] dims = ConversionOptions.pageDimensions(pageSizeName);
                pageWidth = dims[0];
                pageHeight = dims[1];
            }

            // Build ConversionOptions
            ConversionOptions opts = new ConversionOptions(
                    fontSize, pageWidth, pageHeight,
                    marginTop, marginRight, marginBottom, marginLeft,
                    additionalCss, customFonts, maxPages);

            // Phase 1: Parse HTML
            HtmlPreprocessor preprocessor = new HtmlPreprocessor();
            Document doc;
            try {
                doc = preprocessor.preprocess(html.getValue());
            } catch (Exception e) {
                return DiagnosticLog.htmlParseError(
                        "HTML parsing failed: " + e.getMessage(), e);
            }

            // Phase 2: Convert to PDF
            HtmlToPdfConverter converter = new HtmlToPdfConverter();
            byte[] pdf = converter.convert(doc, opts);

            return ValueCreator.createArrayValue(pdf);
        } catch (Exception e) {
            return DiagnosticLog.renderError(
                    "PDF rendering failed: " + e.getMessage(), e);
        }
    }

    // --- PDF reading methods ---

    /**
     * Converts each page of a PDF (as byte[]) to Base64-encoded PNG images.
     */
    public static Object toImages(BArray pdf) {
        try (PDDocument doc = PdfReader.loadFromBytes(pdf.getBytes())) {
            return ValueCreator.createArrayValue(PdfReader.toImages(doc));
        } catch (Exception e) {
            return DiagnosticLog.readError("PDF image conversion failed: " + e.getMessage(), e);
        }
    }

    /**
     * Converts each page of a PDF file to Base64-encoded PNG images.
     */
    public static Object fileToImages(BString filePath) {
        try (PDDocument doc = PdfReader.loadFromFile(filePath.getValue())) {
            return ValueCreator.createArrayValue(PdfReader.toImages(doc));
        } catch (Exception e) {
            return DiagnosticLog.readError("PDF image conversion failed: " + e.getMessage(), e);
        }
    }

    /**
     * Converts each page of a PDF at the given URL to Base64-encoded PNG images.
     */
    public static Object urlToImages(BString url) {
        try (PDDocument doc = PdfReader.loadFromUrl(url.getValue())) {
            return ValueCreator.createArrayValue(PdfReader.toImages(doc));
        } catch (Exception e) {
            return DiagnosticLog.readError("PDF image conversion failed: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts text content from each page of a PDF (as byte[]).
     */
    public static Object extractText(BArray pdf) {
        try (PDDocument doc = PdfReader.loadFromBytes(pdf.getBytes())) {
            return ValueCreator.createArrayValue(PdfReader.extractText(doc));
        } catch (Exception e) {
            return DiagnosticLog.readError("PDF text extraction failed: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts text content from each page of a PDF file.
     */
    public static Object fileExtractText(BString filePath) {
        try (PDDocument doc = PdfReader.loadFromFile(filePath.getValue())) {
            return ValueCreator.createArrayValue(PdfReader.extractText(doc));
        } catch (Exception e) {
            return DiagnosticLog.readError("PDF text extraction failed: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts text content from each page of a PDF at the given URL.
     */
    public static Object urlExtractText(BString url) {
        try (PDDocument doc = PdfReader.loadFromUrl(url.getValue())) {
            return ValueCreator.createArrayValue(PdfReader.extractText(doc));
        } catch (Exception e) {
            return DiagnosticLog.readError("PDF text extraction failed: " + e.getMessage(), e);
        }
    }

    // --- BMap helper methods ---

    private static float getFloat(BMap<BString, Object> map, String key, float defaultValue) {
        Object value = map.get(StringUtils.fromString(key));
        if (value == null) {
            return defaultValue;
        }
        int tag = TypeUtils.getType(value).getTag();
        if (tag == TypeTags.INT_TAG) {
            return ((Long) value).floatValue();
        }
        if (tag == TypeTags.FLOAT_TAG) {
            return ((Double) value).floatValue();
        } //no need for default, no type check

        return defaultValue;
    }

    private static String getString(BMap<BString, Object> map, String key, String defaultValue) {
        Object value = map.get(StringUtils.fromString(key));
        if (value != null && TypeUtils.getType(value).getTag() == TypeTags.STRING_TAG) {
            return ((BString) value).getValue();
        }
        return defaultValue;
    }

    private static boolean getBool(BMap<BString, Object> map, String key) {
        Object value = map.get(StringUtils.fromString(key));
        if (value != null && TypeUtils.getType(value).getTag() == TypeTags.BOOLEAN_TAG) {
            return (Boolean) value;
        }
        return false;
    }
}
