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

/**
 * A replaced element box (img with base64 data URL).
 * Stores the data URL source and intrinsic dimensions.
 */
public class ReplacedBox extends Box {

    private String src; // data:image/png;base64,...
    private float intrinsicWidth;
    private float intrinsicHeight;

    /** Creates a replaced box with the given style. */
    public ReplacedBox(ComputedStyle style) {
        super(style);
    }

    /** Returns the image data URL source. */
    public String getSrc() {
        return src;
    }

    /** Sets the image data URL source. */
    public void setSrc(String src) {
        this.src = src;
    }

    /** Returns the intrinsic image width in points. */
    public float getIntrinsicWidth() {
        return intrinsicWidth;
    }

    /** Sets the intrinsic image width in points. */
    public void setIntrinsicWidth(float w) {
        this.intrinsicWidth = w;
    }

    /** Returns the intrinsic image height in points. */
    public float getIntrinsicHeight() {
        return intrinsicHeight;
    }

    /** Sets the intrinsic image height in points. */
    public void setIntrinsicHeight(float h) {
        this.intrinsicHeight = h;
    }

    @Override
    public String getBoxType() {
        return "replaced";
    }
}
