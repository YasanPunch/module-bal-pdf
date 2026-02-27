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

package io.ballerina.lib.pdf.box;

import io.ballerina.lib.pdf.css.ComputedStyle;
import org.apache.pdfbox.pdmodel.font.PDFont;

/**
 * A text fragment with associated font metrics.
 * Leaf node in the box tree — has no children.
 */
public class TextRun extends Box {

    private String text;
    private PDFont font;
    private float fontSize;
    private float textWidth;
    private boolean isSuperscript;
    private boolean isSubscript;

    /** Creates a text run with the given style and text content. */
    public TextRun(ComputedStyle style, String text) {
        super(style);
        this.text = text;
    }

    /** Returns the text content. */
    public String getText() {
        return text;
    }

    /** Sets the text content. */
    public void setText(String text) {
        this.text = text;
    }

    /** Returns the resolved PDF font. */
    public PDFont getFont() {
        return font;
    }

    /** Sets the resolved PDF font. */
    public void setFont(PDFont font) {
        this.font = font;
    }

    /** Returns the font size in points. */
    public float getFontSize() {
        return fontSize;
    }

    /** Sets the font size in points. */
    public void setFontSize(float fontSize) {
        this.fontSize = fontSize;
    }

    /** Returns the measured text width in points. */
    public float getTextWidth() {
        return textWidth;
    }

    /** Sets the measured text width in points. */
    public void setTextWidth(float textWidth) {
        this.textWidth = textWidth;
    }

    /** Returns whether this is superscript text. */
    public boolean isSuperscript() {
        return isSuperscript;
    }

    /** Sets whether this is superscript text. */
    public void setSuperscript(boolean superscript) {
        isSuperscript = superscript;
    }

    /** Returns whether this is subscript text. */
    public boolean isSubscript() {
        return isSubscript;
    }

    /** Sets whether this is subscript text. */
    public void setSubscript(boolean subscript) {
        isSubscript = subscript;
    }

    @Override
    public String getBoxType() {
        return "text";
    }
}
