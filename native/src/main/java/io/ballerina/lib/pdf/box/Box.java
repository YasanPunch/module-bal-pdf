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

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for all layout boxes.
 * Each box has position, size, margin/padding/border, a computed style, and children.
 */
public abstract class Box {

    // Position relative to parent's content area (in points)
    protected float x;
    protected float y;

    // Content dimensions (in points)
    protected float width;
    protected float height;

    // Margin, Padding, Border are the CSS box model properties.
    protected float marginTop, marginRight, marginBottom, marginLeft;
    protected float paddingTop, paddingRight, paddingBottom, paddingLeft;
    protected float borderTop, borderRight, borderBottom, borderLeft;

    /* Style and structure properties.*/
    protected ComputedStyle style;
    // children list is set during box tree construction and never changes. 
    protected List<Box> children = new ArrayList<>();
    // if the layout engine reorders or wraps children, it stores the result in this list.
    private List<Box> layoutChildren;  // positioned children from layout engine (null = use children).
    
    protected Box parent;
    private String href;  // link target from <a> tags, propagated to leaf boxes
    private String id;    // element id for internal anchor links (destinations)
    /* */

    /** Creates a box with the given computed style. */
    public Box(ComputedStyle style) {
        this.style = style;
    }

    /** Adds a child box and sets its parent reference. */
    public void addChild(Box child) {
        children.add(child);
        child.parent = this;
    }

    // --- Total dimensions including margin/padding/border ---

    /** Total outer width (margin + border + padding + content). */
    public float getOuterWidth() {
        return marginLeft + borderLeft + paddingLeft + width + paddingRight + borderRight + marginRight;
    }

    /** Total outer height (margin + border + padding + content). */
    public float getOuterHeight() {
        return marginTop + borderTop + paddingTop + height + paddingBottom + borderBottom + marginBottom;
    }

    /** Width including border + padding + content (no margin). */
    public float getBorderBoxWidth() {
        return borderLeft + paddingLeft + width + paddingRight + borderRight;
    }

    /** Height including border + padding + content (no margin). */
    public float getBorderBoxHeight() {
        return borderTop + paddingTop + height + paddingBottom + borderBottom;
    }

    /** X position of the content area's left edge (absolute). */
    public float getContentX() {
        return getAbsoluteX() + borderLeft + paddingLeft;
    }

    /** Y position of the content area's top edge (absolute). */
    public float getContentY() {
        return getAbsoluteY() + borderTop + paddingTop;
    }

    /** Absolute X position (traversing parent chain). */
    public float getAbsoluteX() {
        float ax = x + marginLeft;
        if (parent != null) {
            // parent's content area starts after its own border + padding.
            ax += parent.getContentX();
        }
        return ax;
    }

    /** Absolute Y position (traversing parent chain). */
    public float getAbsoluteY() {
        float ay = y + marginTop;
        if (parent != null) {
            ay += parent.getContentY();
        }
        return ay;
    }

    // --- Getters/Setters ---

    /** Returns the x position relative to parent. */
    public float getX() {
        return x;
    }

    /** Sets the x position relative to parent. */
    public void setX(float x) {
        this.x = x;
    }

    /** Returns the y position relative to parent. */
    public float getY() {
        return y;
    }

    /** Sets the y position relative to parent. */
    public void setY(float y) {
        this.y = y;
    }

    /** Returns the content width in points. */
    public float getWidth() {
        return width;
    }

    /** Sets the content width in points. */
    public void setWidth(float width) {
        this.width = width;
    }

    /** Returns the content height in points. */
    public float getHeight() {
        return height;
    }

    /** Sets the content height in points. */
    public void setHeight(float height) {
        this.height = height;
    }

    /** Returns the top margin. */
    public float getMarginTop() {
        return marginTop;
    }

    /** Sets the top margin. */
    public void setMarginTop(float v) {
        this.marginTop = v;
    }

    /** Returns the right margin. */
    public float getMarginRight() {
        return marginRight;
    }

    /** Sets the right margin. */
    public void setMarginRight(float v) {
        this.marginRight = v;
    }

    /** Returns the bottom margin. */
    public float getMarginBottom() {
        return marginBottom;
    }

    /** Sets the bottom margin. */
    public void setMarginBottom(float v) {
        this.marginBottom = v;
    }

    /** Returns the left margin. */
    public float getMarginLeft() {
        return marginLeft;
    }

    /** Sets the left margin. */
    public void setMarginLeft(float v) {
        this.marginLeft = v;
    }

    /** Returns the top padding. */
    public float getPaddingTop() {
        return paddingTop;
    }

    /** Sets the top padding. */
    public void setPaddingTop(float v) {
        this.paddingTop = v;
    }

    /** Returns the right padding. */
    public float getPaddingRight() {
        return paddingRight;
    }

    /** Sets the right padding. */
    public void setPaddingRight(float v) {
        this.paddingRight = v;
    }

    /** Returns the bottom padding. */
    public float getPaddingBottom() {
        return paddingBottom;
    }

    /** Sets the bottom padding. */
    public void setPaddingBottom(float v) {
        this.paddingBottom = v;
    }

    /** Returns the left padding. */
    public float getPaddingLeft() {
        return paddingLeft;
    }

    /** Sets the left padding. */
    public void setPaddingLeft(float v) {
        this.paddingLeft = v;
    }

    /** Returns the top border width. */
    public float getBorderTopWidth() {
        return borderTop;
    }

    /** Sets the top border width. */
    public void setBorderTopWidth(float v) {
        this.borderTop = v;
    }

    /** Returns the right border width. */
    public float getBorderRightWidth() {
        return borderRight;
    }

    /** Sets the right border width. */
    public void setBorderRightWidth(float v) {
        this.borderRight = v;
    }

    /** Returns the bottom border width. */
    public float getBorderBottomWidth() {
        return borderBottom;
    }

    /** Sets the bottom border width. */
    public void setBorderBottomWidth(float v) {
        this.borderBottom = v;
    }

    /** Returns the left border width. */
    public float getBorderLeftWidth() {
        return borderLeft;
    }

    /** Sets the left border width. */
    public void setBorderLeftWidth(float v) {
        this.borderLeft = v;
    }

    /** Returns the computed style. */
    public ComputedStyle getStyle() {
        return style;
    }

    /** Returns the child boxes. */
    public List<Box> getChildren() {
        return children;
    }

    /** Returns the parent box. */
    public Box getParent() {
        return parent;
    }

    /** Returns the layout-ordered children, or null if not set. */
    public List<Box> getLayoutChildren() {
        return layoutChildren;
    }

    /** Sets the layout-ordered children. */
    public void setLayoutChildren(List<Box> lc) {
        this.layoutChildren = lc;
    }

    /** Returns layout children if set, otherwise structural children. */
    public List<Box> getEffectiveChildren() {
        return layoutChildren != null ? layoutChildren : children;
    }

    /** Clears the layout children. */
    public void clearLayoutChildren() {
        this.layoutChildren = null;
    }

    /** Returns the link target URL. */
    public String getHref() {
        return href;
    }

    /** Sets the link target URL. */
    public void setHref(String href) {
        this.href = href;
    }

    /** Returns the element id. */
    public String getId() {
        return id;
    }

    /** Sets the element id. */
    public void setId(String id) {
        this.id = id;
    }

    /** Returns the box type identifier. */
    public abstract String getBoxType();
}
